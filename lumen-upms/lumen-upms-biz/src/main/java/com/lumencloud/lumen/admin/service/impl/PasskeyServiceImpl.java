package com.lumencloud.lumen.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lumencloud.lumen.admin.api.dto.PasskeyCredentialCounterUpdateDTO;
import com.lumencloud.lumen.admin.api.dto.PasskeyCredentialPayload;
import com.lumencloud.lumen.admin.api.dto.PasskeyRegistrationFinishDTO;
import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.vo.PasskeyAccountInfoVO;
import com.lumencloud.lumen.admin.api.vo.PasskeyCredentialVO;
import com.lumencloud.lumen.admin.mapper.AuthAccountCredentialMapper;
import com.lumencloud.lumen.admin.mapper.AuthAccountMapper;
import com.lumencloud.lumen.admin.service.AuthAccountService;
import com.lumencloud.lumen.admin.service.PasskeyService;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.security.passkey.PasskeyChallengeContext;
import com.lumencloud.lumen.common.security.passkey.PasskeyChallengeService;
import com.lumencloud.lumen.common.security.passkey.PasskeyWebAuthnUtils;
import com.lumencloud.lumen.common.security.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Passkey credential management service.
 */
@Service
@RequiredArgsConstructor
public class PasskeyServiceImpl implements PasskeyService {

	private static final String CREDENTIAL_TYPE_PASSKEY = "PASSKEY";

	private static final String IDENTIFIER_USERNAME = "USERNAME";

	private static final String RP_NAME = "Lumen";

	private final AuthAccountMapper authAccountMapper;

	private final AuthAccountCredentialMapper authAccountCredentialMapper;

	private final AuthAccountService authAccountService;

	private final PasskeyChallengeService passkeyChallengeService;

	@Override
	public Map<String, Object> createCurrentRegistrationOptions(HttpServletRequest request) {
		AuthAccount account = requireCurrentAccount();
		String passkeyUserName = resolvePasskeyUserName(account);
		String passkeyDisplayName = resolvePasskeyDisplayName(account);
		PasskeyChallengeContext challengeContext = new PasskeyChallengeContext();
		challengeContext.setType(PasskeyChallengeContext.TYPE_REGISTRATION);
		challengeContext.setChallenge(PasskeyWebAuthnUtils.randomChallenge());
		challengeContext.setClientId(account.getClientId());
		challengeContext.setAccountId(account.getAccountId());
		challengeContext.setUserId(account.getUserId());
		challengeContext.setUsername(passkeyUserName);
		challengeContext.setDisplayName(passkeyDisplayName);
		challengeContext.setRpId(PasskeyWebAuthnUtils.resolveRpId(request));
		challengeContext.setOrigin(PasskeyWebAuthnUtils.resolveOrigin(request));
		passkeyChallengeService.save(challengeContext);

		List<Map<String, Object>> excludeCredentials = listPasskeyCredentials(account.getAccountId()).stream()
			.map(this::toInternalView)
			.map(credential -> PasskeyWebAuthnUtils.createDescriptor(credential.getCredentialKey(),
					credential.getPayload() == null ? List.of() : credential.getPayload().getTransports()))
			.toList();

		Map<String, Object> rp = new LinkedHashMap<>();
		rp.put("id", challengeContext.getRpId());
		rp.put("name", RP_NAME);

		Map<String, Object> user = new LinkedHashMap<>();
		user.put("id", PasskeyWebAuthnUtils.encodeUserHandle(account.getAccountId()));
		user.put("name", passkeyUserName);
		user.put("displayName", passkeyDisplayName);

		Map<String, Object> es256 = new LinkedHashMap<>();
		es256.put("type", "public-key");
		es256.put("alg", -7);

		Map<String, Object> rs256 = new LinkedHashMap<>();
		rs256.put("type", "public-key");
		rs256.put("alg", -257);

		Map<String, Object> authenticatorSelection = new LinkedHashMap<>();
		authenticatorSelection.put("residentKey", "preferred");
		authenticatorSelection.put("userVerification", "preferred");

		Map<String, Object> options = new LinkedHashMap<>();
		options.put("challenge", challengeContext.getChallenge());
		options.put("rp", rp);
		options.put("user", user);
		options.put("pubKeyCredParams", List.of(es256, rs256));
		options.put("timeout", PasskeyChallengeService.DEFAULT_TTL_SECONDS * 1000);
		options.put("attestation", "none");
		options.put("authenticatorSelection", authenticatorSelection);
		options.put("excludeCredentials", excludeCredentials);
		return options;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PasskeyCredentialVO registerCurrentPasskey(HttpServletRequest request, PasskeyRegistrationFinishDTO finishDTO) {
		AuthAccount account = requireCurrentAccount();
		String operator = resolveAccountUsername(account);
		String challenge = PasskeyWebAuthnUtils.extractChallenge(finishDTO.getClientDataJSON());
		PasskeyChallengeContext challengeContext = passkeyChallengeService
			.consume(PasskeyChallengeContext.TYPE_REGISTRATION, challenge)
			.orElseThrow(() -> new IllegalArgumentException("passkey challenge is invalid or expired"));
		if (!account.getAccountId().equals(challengeContext.getAccountId())
				|| !account.getClientId().equals(challengeContext.getClientId())) {
			throw new IllegalArgumentException("passkey challenge account mismatch");
		}
		if (!challengeContext.getOrigin().equals(PasskeyWebAuthnUtils.resolveOrigin(request))) {
			throw new IllegalArgumentException("passkey origin mismatch");
		}

		PasskeyWebAuthnUtils.RegistrationResult registration = PasskeyWebAuthnUtils
			.validateRegistration(finishDTO.getClientDataJSON(), finishDTO.getAttestationObject(), challengeContext);

		AuthAccountCredential credential = authAccountCredentialMapper.selectOne(Wrappers.<AuthAccountCredential>lambdaQuery()
			.eq(AuthAccountCredential::getAccountId, account.getAccountId())
			.eq(AuthAccountCredential::getCredentialType, CREDENTIAL_TYPE_PASSKEY)
			.eq(AuthAccountCredential::getCredentialKey, registration.credentialId()), false);
		if (credential == null) {
			credential = new AuthAccountCredential();
			credential.setAccountId(account.getAccountId());
			credential.setCredentialType(CREDENTIAL_TYPE_PASSKEY);
			credential.setCredentialKey(registration.credentialId());
			credential.setCreateBy(operator);
			credential.setCreateTime(LocalDateTime.now());
		}

		PasskeyCredentialPayload payload = new PasskeyCredentialPayload();
		payload.setPublicKeyCose(registration.publicKeyCose());
		payload.setSignCount(registration.signCount());
		payload.setAlgorithm(registration.algorithm());
		payload.setAaguid(registration.aaguid());
		payload.setTransports(finishDTO.getTransports());

		credential.setSecretValue(PasskeyWebAuthnUtils.writeJson(payload));
		credential.setStatus(CommonConstants.STATUS_NORMAL);
		credential.setVerifiedAt(registration.verifiedAt());
		credential.setUpdateBy(operator);
		credential.setUpdateTime(LocalDateTime.now());
		saveCredential(credential);
		return toPublicView(credential);
	}

	@Override
	public List<PasskeyCredentialVO> listCurrentPasskeys() {
		return listPasskeyCredentials(requireCurrentAccount().getAccountId()).stream().map(this::toPublicView).toList();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean removeCurrentPasskey(String credentialKey) {
		if (!StringUtils.hasText(credentialKey)) {
			return Boolean.FALSE;
		}
		AuthAccount account = requireCurrentAccount();
		authAccountCredentialMapper.delete(Wrappers.<AuthAccountCredential>lambdaQuery()
			.eq(AuthAccountCredential::getAccountId, account.getAccountId())
			.eq(AuthAccountCredential::getCredentialType, CREDENTIAL_TYPE_PASSKEY)
			.eq(AuthAccountCredential::getCredentialKey, credentialKey));
		return Boolean.TRUE;
	}

	@Override
	public PasskeyAccountInfoVO getAccount(String clientId, String username) {
		if (!StringUtils.hasText(clientId) || !StringUtils.hasText(username)) {
			return null;
		}
		AuthAccount account = authAccountService.resolveAccount(clientId, username, null).orElse(null);
		if (account == null || !CommonConstants.STATUS_NORMAL.equals(account.getStatus())) {
			return null;
		}
		PasskeyAccountInfoVO accountInfo = new PasskeyAccountInfoVO();
		accountInfo.setAccountId(account.getAccountId());
		accountInfo.setUserId(account.getUserId());
		accountInfo.setClientId(account.getClientId());
		accountInfo.setUsername(resolveAccountUsername(account));
		accountInfo.setCredentials(listPasskeyCredentials(account.getAccountId()).stream().map(this::toInternalView).toList());
		return accountInfo;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean updateSignCount(PasskeyCredentialCounterUpdateDTO request) {
		if (request == null || request.getAccountId() == null || !StringUtils.hasText(request.getCredentialKey())
				|| request.getSignCount() == null) {
			return Boolean.FALSE;
		}
		AuthAccountCredential credential = authAccountCredentialMapper.selectOne(Wrappers.<AuthAccountCredential>lambdaQuery()
			.eq(AuthAccountCredential::getAccountId, request.getAccountId())
			.eq(AuthAccountCredential::getCredentialType, CREDENTIAL_TYPE_PASSKEY)
			.eq(AuthAccountCredential::getCredentialKey, request.getCredentialKey()), false);
		if (credential == null) {
			return Boolean.FALSE;
		}
		PasskeyCredentialPayload payload = readPayload(credential);
		payload.setSignCount(request.getSignCount());
		credential.setSecretValue(PasskeyWebAuthnUtils.writeJson(payload));
		credential.setVerifiedAt(LocalDateTime.now());
		credential.setUpdateTime(LocalDateTime.now());
		credential.setUpdateBy(Optional.ofNullable(credential.getUpdateBy()).orElse("passkey"));
		authAccountCredentialMapper.updateById(credential);
		return Boolean.TRUE;
	}

	private AuthAccount requireCurrentAccount() {
		Long accountId = SecurityUtils.getUser().getAccountId();
		AuthAccount account = authAccountMapper.selectById(accountId);
		if (account == null) {
			throw new IllegalArgumentException("current auth account not found");
		}
		return account;
	}

	private List<AuthAccountCredential> listPasskeyCredentials(Long accountId) {
		return authAccountCredentialMapper.selectList(Wrappers.<AuthAccountCredential>lambdaQuery()
			.eq(AuthAccountCredential::getAccountId, accountId)
			.eq(AuthAccountCredential::getCredentialType, CREDENTIAL_TYPE_PASSKEY)
			.eq(AuthAccountCredential::getStatus, CommonConstants.STATUS_NORMAL)
			.orderByDesc(AuthAccountCredential::getVerifiedAt)
			.orderByDesc(AuthAccountCredential::getCreateTime));
	}

	private PasskeyCredentialPayload readPayload(AuthAccountCredential credential) {
		return StringUtils.hasText(credential.getSecretValue())
				? PasskeyWebAuthnUtils.readJson(credential.getSecretValue(), PasskeyCredentialPayload.class)
				: new PasskeyCredentialPayload();
	}

	private PasskeyCredentialVO toInternalView(AuthAccountCredential credential) {
		PasskeyCredentialVO view = new PasskeyCredentialVO();
		view.setCredentialKey(credential.getCredentialKey());
		view.setStatus(credential.getStatus());
		view.setVerifiedAt(credential.getVerifiedAt());
		view.setCreateTime(credential.getCreateTime());
		view.setPayload(readPayload(credential));
		return view;
	}

	private PasskeyCredentialVO toPublicView(AuthAccountCredential credential) {
		PasskeyCredentialVO view = toInternalView(credential);
		PasskeyCredentialPayload payload = view.getPayload();
		if (payload != null) {
			PasskeyCredentialPayload safePayload = new PasskeyCredentialPayload();
			safePayload.setAlgorithm(payload.getAlgorithm());
			safePayload.setAaguid(payload.getAaguid());
			safePayload.setTransports(payload.getTransports());
			view.setPayload(safePayload);
		}
		return view;
	}

	private void saveCredential(AuthAccountCredential credential) {
		if (credential.getCredentialId() == null) {
			authAccountCredentialMapper.insert(credential);
		}
		else {
			authAccountCredentialMapper.updateById(credential);
		}
	}

	private String resolvePasskeyUserName(AuthAccount account) {
		return account.getClientId() + ":" + resolveAccountUsername(account);
	}

	private String resolvePasskeyDisplayName(AuthAccount account) {
		return account.getClientId() + " / " + resolveAccountUsername(account);
	}

	private String resolveAccountUsername(AuthAccount account) {
		return authAccountService.getPrimaryIdentifierValue(account.getAccountId(), IDENTIFIER_USERNAME)
			.orElse("account-" + account.getAccountId());
	}

}
