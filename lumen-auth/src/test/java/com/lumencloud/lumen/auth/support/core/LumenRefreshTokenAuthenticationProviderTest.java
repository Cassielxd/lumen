package com.lumencloud.lumen.auth.support.core;

import com.lumencloud.lumen.admin.api.dto.AuthSessionLogoutDTO;
import com.lumencloud.lumen.admin.api.entity.AuthSession;
import com.lumencloud.lumen.admin.api.feign.RemoteAuthSessionService;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.util.R;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LumenRefreshTokenAuthenticationProviderTest {

	private static final String REFRESH_TOKEN = "refresh-token";

	@Mock
	private OAuth2RefreshTokenAuthenticationProvider delegate;

	@Mock
	private RemoteAuthSessionService remoteAuthSessionService;

	@InjectMocks
	private LumenRefreshTokenAuthenticationProvider provider;

	@Test
	void authenticateDelegatesWhenSessionIsActive() {
		OAuth2RefreshTokenAuthenticationToken authentication = createAuthentication();
		AuthSession session = new AuthSession();
		session.setStatus(CommonConstants.STATUS_NORMAL);
		Authentication delegated = mock(Authentication.class);
		when(remoteAuthSessionService.getByRefreshToken(any())).thenReturn(R.ok(session));
		when(delegate.authenticate(authentication)).thenReturn(delegated);

		Authentication result = provider.authenticate(authentication);

		assertThat(result).isSameAs(delegated);
		ArgumentCaptor<AuthSessionLogoutDTO> requestCaptor = ArgumentCaptor.forClass(AuthSessionLogoutDTO.class);
		verify(remoteAuthSessionService).getByRefreshToken(requestCaptor.capture());
		assertThat(requestCaptor.getValue().getRefreshToken()).isEqualTo(REFRESH_TOKEN);
		verify(delegate).authenticate(authentication);
	}

	@Test
	void authenticateRejectsMissingSession() {
		OAuth2RefreshTokenAuthenticationToken authentication = createAuthentication();
		when(remoteAuthSessionService.getByRefreshToken(any())).thenReturn(R.ok(null));

		assertThatThrownBy(() -> provider.authenticate(authentication)).isInstanceOf(OAuth2AuthenticationException.class)
			.extracting(ex -> ((OAuth2AuthenticationException) ex).getError().getErrorCode())
			.isEqualTo(OAuth2ErrorCodes.INVALID_GRANT);
		verify(delegate, never()).authenticate(any());
	}

	@Test
	void authenticateRejectsLoggedOutSession() {
		OAuth2RefreshTokenAuthenticationToken authentication = createAuthentication();
		AuthSession session = new AuthSession();
		session.setStatus(CommonConstants.STATUS_NORMAL);
		session.setLogoutTime(LocalDateTime.now());
		when(remoteAuthSessionService.getByRefreshToken(any())).thenReturn(R.ok(session));

		assertThatThrownBy(() -> provider.authenticate(authentication)).isInstanceOf(OAuth2AuthenticationException.class)
			.extracting(ex -> ((OAuth2AuthenticationException) ex).getError().getErrorCode())
			.isEqualTo(OAuth2ErrorCodes.INVALID_GRANT);
		verify(delegate, never()).authenticate(any());
	}

	private OAuth2RefreshTokenAuthenticationToken createAuthentication() {
		RegisteredClient registeredClient = RegisteredClient.withId("client")
			.clientId("client")
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
			.build();
		OAuth2ClientAuthenticationToken clientPrincipal = new OAuth2ClientAuthenticationToken(registeredClient,
				ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "secret");
		return new OAuth2RefreshTokenAuthenticationToken(REFRESH_TOKEN, clientPrincipal, Set.of(), Map.of());
	}

}
