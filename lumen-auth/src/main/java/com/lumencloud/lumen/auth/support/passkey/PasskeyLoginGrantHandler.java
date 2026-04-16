package com.lumencloud.lumen.auth.support.passkey;

import com.lumencloud.lumen.admin.api.feign.RemotePasskeyService;
import com.lumencloud.lumen.auth.support.core.OAuth2LoginGrantHandler;
import com.lumencloud.lumen.common.security.passkey.PasskeyChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;

/**
 * Passkey login grant handler.
 */
@Component
@RequiredArgsConstructor
public class PasskeyLoginGrantHandler implements OAuth2LoginGrantHandler {

	private final RemotePasskeyService remotePasskeyService;

	private final PasskeyChallengeService passkeyChallengeService;

	@Override
	public AuthenticationConverter getAuthenticationConverter() {
		return new OAuth2ResourceOwnerPasskeyAuthenticationConverter();
	}

	@Override
	public AuthenticationProvider getAuthenticationProvider(AuthenticationManager authenticationManager,
			OAuth2AuthorizationService authorizationService,
			OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator) {
		return new OAuth2ResourceOwnerPasskeyAuthenticationProvider(authenticationManager, authorizationService,
				tokenGenerator, remotePasskeyService, passkeyChallengeService);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 15;
	}

}
