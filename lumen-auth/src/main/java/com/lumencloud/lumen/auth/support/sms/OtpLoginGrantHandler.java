package com.lumencloud.lumen.auth.support.sms;

import org.springframework.core.Ordered;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;

import com.lumencloud.lumen.auth.support.core.OAuth2LoginGrantHandler;

/**
 * OTP grant handler. Keeps compatibility with the legacy mobile grant.
 */
@Component
public class OtpLoginGrantHandler implements OAuth2LoginGrantHandler {

	@Override
	public AuthenticationConverter getAuthenticationConverter() {
		return new OAuth2ResourceOwnerSmsAuthenticationConverter();
	}

	@Override
	public AuthenticationProvider getAuthenticationProvider(AuthenticationManager authenticationManager,
			OAuth2AuthorizationService authorizationService,
			OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator) {
		return new OAuth2ResourceOwnerSmsAuthenticationProvider(authenticationManager, authorizationService,
				tokenGenerator);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}

}
