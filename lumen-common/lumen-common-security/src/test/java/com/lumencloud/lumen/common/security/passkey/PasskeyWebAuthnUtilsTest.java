package com.lumencloud.lumen.common.security.passkey;

import com.lumencloud.lumen.admin.api.dto.PasskeyCredentialPayload;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class PasskeyWebAuthnUtilsTest {

	@Test
	void randomChallengeUsesThirtyTwoBytes() {
		String challenge = PasskeyWebAuthnUtils.randomChallenge();

		assertThat(challenge).isNotBlank();
		assertThat(PasskeyWebAuthnUtils.decodeBase64Url(challenge)).hasSize(32);
	}

	@Test
	void encodeUserHandleSerializesLongValue() {
		String userHandle = PasskeyWebAuthnUtils.encodeUserHandle(42L);

		assertThat(ByteBuffer.wrap(PasskeyWebAuthnUtils.decodeBase64Url(userHandle)).getLong()).isEqualTo(42L);
	}

	@Test
	void resolveOriginPrefersForwardedHeaders() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("internal.local");
		request.setServerPort(8080);
		request.addHeader("X-Forwarded-Proto", "https,http");
		request.addHeader("X-Forwarded-Host", "login.example.com:8443, proxy.local");

		assertThat(PasskeyWebAuthnUtils.resolveOrigin(request)).isEqualTo("https://login.example.com:8443");
	}

	@Test
	void resolveRpIdStripsPortAndProxyChain() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Forwarded-Host", "login.example.com:8443, proxy.local");

		assertThat(PasskeyWebAuthnUtils.resolveRpId(request)).isEqualTo("login.example.com");
	}

	@Test
	void resolveRpIdRejectsIpLiteralHost() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Host", "127.0.0.1:9999");

		assertThatThrownBy(() -> PasskeyWebAuthnUtils.resolveRpId(request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("do not use an IP address");
	}

	@Test
	void descriptorIncludesTransportsOnlyWhenPresent() {
		Map<String, Object> withTransports = PasskeyWebAuthnUtils.createDescriptor("cred-1", List.of("internal"));
		Map<String, Object> withoutTransports = PasskeyWebAuthnUtils.createDescriptor("cred-2", List.of());

		assertThat(withTransports).containsEntry("type", "public-key")
			.containsEntry("id", "cred-1")
			.containsEntry("transports", List.of("internal"));
		assertThat(withoutTransports).containsEntry("type", "public-key").containsEntry("id", "cred-2");
		assertThat(withoutTransports).doesNotContainKey("transports");
	}

	@Test
	void writeAndReadJsonRoundTripPayload() {
		PasskeyCredentialPayload payload = new PasskeyCredentialPayload();
		payload.setPublicKeyCose("public-key");
		payload.setSignCount(7L);
		payload.setAlgorithm(-7);
		payload.setTransports(List.of("internal", "hybrid"));

		String json = PasskeyWebAuthnUtils.writeJson(payload);
		PasskeyCredentialPayload parsed = PasskeyWebAuthnUtils.readJson(json, PasskeyCredentialPayload.class);

		assertThat(parsed.getPublicKeyCose()).isEqualTo("public-key");
		assertThat(parsed.getSignCount()).isEqualTo(7L);
		assertThat(parsed.getAlgorithm()).isEqualTo(-7);
		assertThat(parsed.getTransports()).containsExactly("internal", "hybrid");
	}
}
