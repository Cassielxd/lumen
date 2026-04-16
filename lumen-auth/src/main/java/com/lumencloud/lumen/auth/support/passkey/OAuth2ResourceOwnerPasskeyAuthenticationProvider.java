package com.lumencloud.lumen.auth.support.passkey;

import com.lumencloud.lumen.admin.api.dto.PasskeyAccountLookupDTO;
import com.lumencloud.lumen.admin.api.dto.PasskeyCredentialCounterUpdateDTO;
import com.lumencloud.lumen.admin.api.feign.RemotePasskeyService;
import com.lumencloud.lumen.admin.api.vo.PasskeyAccountInfoVO;
import com.lumencloud.lumen.admin.api.vo.PasskeyCredentialVO;
import com.lumencloud.lumen.auth.support.base.OAuth2ResourceOwnerBaseAuthenticationProvider;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.util.RetOps;
import com.lumencloud.lumen.common.security.passkey.PasskeyChallengeContext;
import com.lumencloud.lumen.common.security.passkey.PasskeyChallengeService;
import com.lumencloud.lumen.common.security.passkey.PasskeyWebAuthnUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import java.util.Map;

/**
 * OAuth2 passkey authentication provider.
 */
public class OAuth2ResourceOwnerPasskeyAuthenticationProvider
		extends OAuth2ResourceOwnerBaseAuthenticationProvider<OAuth2ResourceOwnerPasskeyAuthenticationToken> {

	private static final String PARAM_CREDENTIAL_ID = "credentialId";

	private static final String PARAM_CLIENT_DATA_JSON = "clientDataJSON";

	private static final String PARAM_AUTHENTICATOR_DATA = "authenticatorData";

	private static final String PARAM_SIGNATURE = "signature";

	private final RemotePasskeyService remotePasskeyService;

	private final PasskeyChallengeService passkeyChallengeService;

	public OAuth2ResourceOwnerPasskeyAuthenticationProvider(AuthenticationManager authenticationManager,
			OAuth2AuthorizationService authorizationService,
			OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator, RemotePasskeyService remotePasskeyService,
			PasskeyChallengeService passkeyChallengeService) {
		super(authenticationManager, authorizationService, tokenGenerator);
		this.remotePasskeyService = remotePasskeyService;
		this.passkeyChallengeService = passkeyChallengeService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) {
		verifyPasskey((OAuth2ResourceOwnerPasskeyAuthenticationToken) authentication);
		return super.authenticate(authentication);
	}

	@Override
	public UsernamePasswordAuthenticationToken buildToken(Map<String, Object> reqParameters) {
		return new UsernamePasswordAuthenticationToken(reqParameters.get(SecurityConstants.USERNAME), "");
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return OAuth2ResourceOwnerPasskeyAuthenticationToken.class.isAssignableFrom(authentication);
	}

	@Override
	public void checkClient(RegisteredClient registeredClient) {
		if (!registeredClient.getAuthorizationGrantTypes().contains(new AuthorizationGrantType(SecurityConstants.PASSKEY))) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
		}
	}

	private void verifyPasskey(OAuth2ResourceOwnerPasskeyAuthenticationToken authenticationToken) {
		Map<String, Object> parameters = authenticationToken.getAdditionalParameters();
		OAuth2ClientAuthenticationToken clientPrincipal = (OAuth2ClientAuthenticationToken) authenticationToken
			.getClientPrincipal();
		String clientId = clientPrincipal.getRegisteredClient().getClientId();
		String username = (String) parameters.get(SecurityConstants.USERNAME);
		String credentialId = (String) parameters.get(PARAM_CREDENTIAL_ID);
		String clientDataJSON = (String) parameters.get(PARAM_CLIENT_DATA_JSON);
		String authenticatorData = (String) parameters.get(PARAM_AUTHENTICATOR_DATA);
		String signature = (String) parameters.get(PARAM_SIGNATURE);

		PasskeyAccountLookupDTO lookupDTO = new PasskeyAccountLookupDTO();
		lookupDTO.setClientId(clientId);
		lookupDTO.setUsername(username);
		PasskeyAccountInfoVO accountInfo = RetOps.of(remotePasskeyService.getAccount(lookupDTO))
			.getData()
			.orElseThrow(() -> invalidGrant("passkey account not found"));
		PasskeyCredentialVO credential = accountInfo.getCredentials()
			.stream()
			.filter(item -> credentialId.equals(item.getCredentialKey()))
			.filter(item -> CommonConstants.STATUS_NORMAL.equals(item.getStatus()))
			.findFirst()
			.orElseThrow(() -> invalidGrant("passkey credential not found"));

		String challenge = PasskeyWebAuthnUtils.extractChallenge(clientDataJSON);
		PasskeyChallengeContext challengeContext = passkeyChallengeService
			.consume(PasskeyChallengeContext.TYPE_ASSERTION, challenge)
			.orElseThrow(() -> invalidGrant("passkey challenge is invalid or expired"));
		if (!clientId.equals(challengeContext.getClientId()) || !accountInfo.getAccountId().equals(challengeContext.getAccountId())
				|| !username.equals(challengeContext.getUsername())) {
			throw invalidGrant("passkey challenge mismatch");
		}

		PasskeyWebAuthnUtils.AssertionResult assertionResult = PasskeyWebAuthnUtils.validateAssertion(credentialId,
				clientDataJSON, authenticatorData, signature, credential.getPayload(), challengeContext);
		if (assertionResult.signCount() != null && assertionResult.signCount() > 0) {
			PasskeyCredentialCounterUpdateDTO updateDTO = new PasskeyCredentialCounterUpdateDTO();
			updateDTO.setAccountId(accountInfo.getAccountId());
			updateDTO.setCredentialKey(credentialId);
			updateDTO.setSignCount(assertionResult.signCount());
			Boolean updated = RetOps.of(remotePasskeyService.updateSignCount(updateDTO)).getData().orElse(Boolean.FALSE);
			if (!Boolean.TRUE.equals(updated)) {
				throw invalidGrant("passkey credential update failed");
			}
		}
	}

	private OAuth2AuthenticationException invalidGrant(String message) {
		return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, message, null));
	}

}
