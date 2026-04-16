package com.lumencloud.lumen.auth.support.core;

import com.lumencloud.lumen.admin.api.dto.AuthSessionLogoutDTO;
import com.lumencloud.lumen.admin.api.entity.AuthSession;
import com.lumencloud.lumen.admin.api.feign.RemoteAuthSessionService;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.util.RetOps;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;

/**
 * Refresh token provider with auth_session validation.
 */
@RequiredArgsConstructor
public class LumenRefreshTokenAuthenticationProvider implements AuthenticationProvider {

	private final OAuth2RefreshTokenAuthenticationProvider delegate;

	private final RemoteAuthSessionService remoteAuthSessionService;

	@Override
	public Authentication authenticate(Authentication authentication) {
		OAuth2RefreshTokenAuthenticationToken refreshTokenAuthentication = (OAuth2RefreshTokenAuthenticationToken) authentication;
		requireActiveSession(refreshTokenAuthentication.getRefreshToken());
		return delegate.authenticate(authentication);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return OAuth2RefreshTokenAuthenticationToken.class.isAssignableFrom(authentication);
	}

	private void requireActiveSession(String refreshToken) {
		AuthSessionLogoutDTO request = new AuthSessionLogoutDTO();
		request.setRefreshToken(refreshToken);
		AuthSession session = RetOps.of(remoteAuthSessionService.getByRefreshToken(request))
			.getData()
			.orElseThrow(this::invalidGrant);
		if (!CommonConstants.STATUS_NORMAL.equals(session.getStatus()) || session.getLogoutTime() != null) {
			throw invalidGrant();
		}
	}

	private OAuth2AuthenticationException invalidGrant() {
		return new OAuth2AuthenticationException(
				new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "session is invalid", null));
	}

}
