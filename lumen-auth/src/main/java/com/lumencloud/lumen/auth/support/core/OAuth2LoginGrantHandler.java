package com.lumencloud.lumen.auth.support.core;

import org.springframework.core.Ordered;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Login grant extension point for the authorization server.
 */
public interface OAuth2LoginGrantHandler extends Ordered {

	AuthenticationConverter getAuthenticationConverter();

	AuthenticationProvider getAuthenticationProvider(AuthenticationManager authenticationManager,
			OAuth2AuthorizationService authorizationService,
			OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator);

	@Override
	default int getOrder() {
		return 0;
	}

}
