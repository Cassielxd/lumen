package com.lumencloud.lumen.common.security.passkey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.lumencloud.lumen.admin.api.dto.PasskeyCredentialPayload;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal WebAuthn helpers for passkey registration and assertion verification.
 */
@UtilityClass
public class PasskeyWebAuthnUtils {

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final ObjectMapper CBOR_MAPPER = new ObjectMapper(new CBORFactory());

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private static final int FLAG_USER_PRESENT = 0x01;

	private static final int FLAG_USER_VERIFIED = 0x04;

	private static final int FLAG_ATTESTED_CREDENTIAL_DATA = 0x40;

	public String randomChallenge() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return encodeBase64Url(bytes);
	}

	public String encodeBase64Url(byte[] value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	public byte[] decodeBase64Url(String value) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException("passkey payload is empty");
		}
		return Base64.getUrlDecoder().decode(value);
	}

	public String encodeUserHandle(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId is required");
		}
		return encodeBase64Url(ByteBuffer.allocate(Long.BYTES).putLong(userId).array());
	}

	public String resolveOrigin(HttpServletRequest request) {
		String scheme = firstHeader(request, "X-Forwarded-Proto", request.getScheme());
		String host = firstHeader(request, "X-Forwarded-Host", request.getHeader("Host"));
		if (!StringUtils.hasText(host)) {
			host = request.getServerName();
			int port = request.getServerPort();
			if (port > 0 && !isDefaultPort(scheme, port)) {
				host = host + ":" + port;
			}
		}
		return scheme + "://" + host;
	}

	public String resolveRpId(HttpServletRequest request) {
		String host = firstHeader(request, "X-Forwarded-Host", request.getHeader("Host"));
		if (!StringUtils.hasText(host)) {
			host = request.getServerName();
		}
		int separator = host.indexOf(',');
		if (separator >= 0) {
			host = host.substring(0, separator).trim();
		}
		int colonIndex = host.indexOf(':');
		String rpId = colonIndex >= 0 ? host.substring(0, colonIndex) : host;
		if (isIpLiteralHost(rpId)) {
			throw new IllegalArgumentException(
					"passkey requires localhost or a real domain, do not use an IP address: " + rpId);
		}
		return rpId;
	}

	public String extractChallenge(String clientDataJson) {
		return parseClientData(clientDataJson).challenge();
	}

	public RegistrationResult validateRegistration(String clientDataJson, String attestationObject,
			PasskeyChallengeContext challengeContext) {
		ClientData clientData = parseClientData(clientDataJson);
		assertClientData(clientData, challengeContext, "webauthn.create");

		JsonNode attestationNode = readCborNode(decodeBase64Url(attestationObject));
		byte[] authData = binary(attestationNode, "authData");
		AuthData parsed = parseAuthData(authData);
		if ((parsed.flags() & FLAG_ATTESTED_CREDENTIAL_DATA) == 0) {
			throw new IllegalArgumentException("missing attested credential data");
		}
		assertRpIdHash(parsed.rpIdHash(), challengeContext.getRpId());
		if ((parsed.flags() & FLAG_USER_PRESENT) == 0) {
			throw new IllegalArgumentException("user presence flag is missing");
		}
		if (!StringUtils.hasText(parsed.credentialIdBase64Url())
				|| !StringUtils.hasText(parsed.publicKeyCoseBase64Url())) {
			throw new IllegalArgumentException("passkey credential payload is incomplete");
		}
		return new RegistrationResult(parsed.credentialIdBase64Url(), parsed.publicKeyCoseBase64Url(), parsed.signCount(),
				parsed.algorithm(), parsed.aaguid(), LocalDateTime.now());
	}

	public AssertionResult validateAssertion(String credentialId, String clientDataJson, String authenticatorData,
			String signature, PasskeyCredentialPayload payload, PasskeyChallengeContext challengeContext) {
		if (payload == null || !StringUtils.hasText(payload.getPublicKeyCose())) {
			throw new IllegalArgumentException("stored passkey credential is invalid");
		}
		ClientData clientData = parseClientData(clientDataJson);
		assertClientData(clientData, challengeContext, "webauthn.get");
		if (!StringUtils.hasText(credentialId)) {
			throw new IllegalArgumentException("credentialId is required");
		}

		byte[] authenticatorDataBytes = decodeBase64Url(authenticatorData);
		AuthData parsed = parseAuthData(authenticatorDataBytes);
		assertRpIdHash(parsed.rpIdHash(), challengeContext.getRpId());
		if ((parsed.flags() & FLAG_USER_PRESENT) == 0) {
			throw new IllegalArgumentException("user presence flag is missing");
		}

		Long storedSignCount = payload.getSignCount();
		if (storedSignCount != null && storedSignCount > 0 && parsed.signCount() > 0
				&& parsed.signCount() <= storedSignCount) {
			throw new IllegalArgumentException("passkey signature counter is invalid");
		}

		byte[] clientDataHash = sha256(decodeBase64Url(clientDataJson));
		byte[] signedBytes = new byte[authenticatorDataBytes.length + clientDataHash.length];
		System.arraycopy(authenticatorDataBytes, 0, signedBytes, 0, authenticatorDataBytes.length);
		System.arraycopy(clientDataHash, 0, signedBytes, authenticatorDataBytes.length, clientDataHash.length);

		PublicKey publicKey = readPublicKey(payload);
		String signatureAlgorithm = resolveSignatureAlgorithm(payload.getAlgorithm());
		if (!verifySignature(signatureAlgorithm, publicKey, signedBytes, decodeBase64Url(signature))) {
			throw new IllegalArgumentException("passkey signature verification failed");
		}
		return new AssertionResult(parsed.signCount(), (parsed.flags() & FLAG_USER_VERIFIED) != 0);
	}

	public List<String> parseTransports(String transports) {
		if (!StringUtils.hasText(transports)) {
			return List.of();
		}
		try {
			return JSON_MAPPER.readValue(transports, new TypeReference<List<String>>() {
			});
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("invalid passkey transports", ex);
		}
	}

	public String writeJson(Object value) {
		try {
			return JSON_MAPPER.writeValueAsString(value);
		}
		catch (IOException ex) {
			throw new IllegalStateException("failed to serialize passkey payload", ex);
		}
	}

	public <T> T readJson(String value, Class<T> targetType) {
		try {
			return JSON_MAPPER.readValue(value, targetType);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("invalid passkey json payload", ex);
		}
	}

	public Map<String, Object> createDescriptor(String credentialId, List<String> transports) {
		Map<String, Object> descriptor = new LinkedHashMap<>();
		descriptor.put("type", "public-key");
		descriptor.put("id", credentialId);
		if (transports != null && !transports.isEmpty()) {
			descriptor.put("transports", transports);
		}
		return descriptor;
	}

	private ClientData parseClientData(String clientDataJson) {
		try {
			byte[] clientDataBytes = decodeBase64Url(clientDataJson);
			JsonNode node = JSON_MAPPER.readTree(clientDataBytes);
			return new ClientData(node.path("type").asText(null), node.path("challenge").asText(null),
					node.path("origin").asText(null));
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("invalid clientDataJSON", ex);
		}
	}

	private void assertClientData(ClientData clientData, PasskeyChallengeContext challengeContext, String expectedType) {
		if (!expectedType.equals(clientData.type())) {
			throw new IllegalArgumentException("unexpected WebAuthn ceremony type");
		}
		if (!StringUtils.hasText(clientData.challenge()) || !clientData.challenge().equals(challengeContext.getChallenge())) {
			throw new IllegalArgumentException("passkey challenge does not match");
		}
		if (!StringUtils.hasText(clientData.origin()) || !clientData.origin().equals(challengeContext.getOrigin())) {
			throw new IllegalArgumentException("passkey origin does not match");
		}
	}

	private AuthData parseAuthData(byte[] authData) {
		if (authData == null || authData.length < 37) {
			throw new IllegalArgumentException("authenticator data is invalid");
		}
		int index = 0;
		byte[] rpIdHash = Arrays.copyOfRange(authData, index, index + 32);
		index += 32;
		int flags = authData[index++] & 0xFF;
		long signCount = readUnsignedInt(authData, index);
		index += 4;

		String credentialId = null;
		String publicKeyCose = null;
		Integer algorithm = null;
		String aaguid = null;

		if ((flags & FLAG_ATTESTED_CREDENTIAL_DATA) != 0) {
			byte[] aaguidBytes = Arrays.copyOfRange(authData, index, index + 16);
			aaguid = HexFormat.of().formatHex(aaguidBytes);
			index += 16;
			int credentialIdLength = ((authData[index] & 0xFF) << 8) | (authData[index + 1] & 0xFF);
			index += 2;
			byte[] credentialIdBytes = Arrays.copyOfRange(authData, index, index + credentialIdLength);
			credentialId = encodeBase64Url(credentialIdBytes);
			index += credentialIdLength;
			JsonNode publicKeyNode = readFirstCborNode(Arrays.copyOfRange(authData, index, authData.length));
			publicKeyCose = encodeBase64Url(writeCbor(publicKeyNode));
			algorithm = publicKeyNode.path("3").asInt();
		}

		return new AuthData(rpIdHash, flags, signCount, credentialId, publicKeyCose, algorithm, aaguid);
	}

	private JsonNode readCborNode(byte[] value) {
		try {
			return CBOR_MAPPER.readTree(value);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("invalid CBOR payload", ex);
		}
	}

	private JsonNode readFirstCborNode(byte[] value) {
		try (var parser = new CBORFactory().createParser(value)) {
			return CBOR_MAPPER.readTree(parser);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("invalid embedded COSE key", ex);
		}
	}

	private byte[] writeCbor(JsonNode node) {
		try {
			return CBOR_MAPPER.writeValueAsBytes(node);
		}
		catch (IOException ex) {
			throw new IllegalStateException("failed to serialize COSE key", ex);
		}
	}

	private byte[] binary(JsonNode node, String field) {
		try {
			return node.path(field).binaryValue();
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("invalid binary field: " + field, ex);
		}
	}

	private void assertRpIdHash(byte[] actualHash, String rpId) {
		byte[] expectedHash = sha256(rpId.getBytes(StandardCharsets.UTF_8));
		if (!Arrays.equals(expectedHash, actualHash)) {
			throw new IllegalArgumentException("rpId hash does not match");
		}
	}

	private PublicKey readPublicKey(PasskeyCredentialPayload payload) {
		JsonNode cose = readCborNode(decodeBase64Url(payload.getPublicKeyCose()));
		int keyType = cose.path("1").asInt();
		int algorithm = payload.getAlgorithm() == null ? cose.path("3").asInt() : payload.getAlgorithm();
		try {
			if (keyType == 2 && algorithm == -7) {
				AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
				parameters.init(new ECGenParameterSpec("secp256r1"));
				ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
				BigInteger x = new BigInteger(1, cose.path("-2").binaryValue());
				BigInteger y = new BigInteger(1, cose.path("-3").binaryValue());
				return KeyFactory.getInstance("EC")
					.generatePublic(new ECPublicKeySpec(new java.security.spec.ECPoint(x, y), ecParameters));
			}
			if (keyType == 3 && algorithm == -257) {
				BigInteger modulus = new BigInteger(1, cose.path("-1").binaryValue());
				BigInteger exponent = new BigInteger(1, cose.path("-2").binaryValue());
				return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
			}
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("failed to decode WebAuthn public key", ex);
		}
		throw new IllegalArgumentException("unsupported WebAuthn public key algorithm");
	}

	private String resolveSignatureAlgorithm(Integer algorithm) {
		if (algorithm == null || algorithm == -7) {
			return "SHA256withECDSA";
		}
		if (algorithm == -257) {
			return "SHA256withRSA";
		}
		throw new IllegalArgumentException("unsupported WebAuthn signature algorithm");
	}

	private boolean verifySignature(String signatureAlgorithm, PublicKey publicKey, byte[] data, byte[] signature) {
		try {
			Signature verifier = Signature.getInstance(signatureAlgorithm);
			verifier.initVerify(publicKey);
			verifier.update(data);
			return verifier.verify(signature);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("failed to verify WebAuthn signature", ex);
		}
	}

	private byte[] sha256(byte[] value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value);
		}
		catch (Exception ex) {
			throw new IllegalStateException("SHA-256 unavailable", ex);
		}
	}

	private long readUnsignedInt(byte[] value, int offset) {
		return ((long) (value[offset] & 0xFF) << 24) | ((long) (value[offset + 1] & 0xFF) << 16)
				| ((long) (value[offset + 2] & 0xFF) << 8) | (value[offset + 3] & 0xFFL);
	}

	private boolean isDefaultPort(String scheme, int port) {
		return ("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443);
	}

	private String firstHeader(HttpServletRequest request, String name, String fallback) {
		String value = request.getHeader(name);
		return StringUtils.hasText(value) ? value.split(",")[0].trim() : fallback;
	}

	private boolean isIpLiteralHost(String host) {
		if (!StringUtils.hasText(host)) {
			return false;
		}
		String normalizedHost = host.trim();
		if ("::1".equals(normalizedHost) || "[::1]".equals(normalizedHost)) {
			return true;
		}
		return normalizedHost.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
	}

	public record RegistrationResult(String credentialId, String publicKeyCose, Long signCount, Integer algorithm,
			String aaguid, LocalDateTime verifiedAt) {
	}

	public record AssertionResult(Long signCount, boolean userVerified) {
	}

	private record ClientData(String type, String challenge, String origin) {
	}

	private record AuthData(byte[] rpIdHash, int flags, Long signCount, String credentialIdBase64Url,
			String publicKeyCoseBase64Url, Integer algorithm, String aaguid) {
	}

}
