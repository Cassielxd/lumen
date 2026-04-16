package com.lumencloud.lumen.auth.support.password;

import com.lumencloud.lumen.auth.support.core.OAuth2LoginGrantHandler;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;

/**
 * Password grant handler.
 */
@Component
public class PasswordLoginGrantHandler implements OAuth2LoginGrantHandler {

	@Override
	public AuthenticationConverter getAuthenticationConverter() {
		return new OAuth2ResourceOwnerPasswordAuthenticationConverter();
	}

	@Override
	public AuthenticationProvider getAuthenticationProvider(AuthenticationManager authenticationManager,
			OAuth2AuthorizationService authorizationService,
			OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator) {
		return new OAuth2ResourceOwnerPasswordAuthenticationProvider(authenticationManager, authorizationService,
				tokenGenerator);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
