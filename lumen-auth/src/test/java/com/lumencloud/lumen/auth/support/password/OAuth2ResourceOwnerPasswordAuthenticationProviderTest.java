package com.lumencloud.lumen.auth.support.password;

import java.util.Map;
import java.util.Set;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.security.util.OAuth2ErrorCodesExpand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OAuth2ResourceOwnerPasswordAuthenticationProviderTest {

	private static GenericApplicationContext applicationContext;

	@BeforeAll
	static void setUpContext() {
		applicationContext = new GenericApplicationContext();
		applicationContext.registerBean("securityMessageSource", StaticMessageSource.class, StaticMessageSource::new);
		applicationContext.refresh();
		new SpringUtil().setApplicationContext(applicationContext);
	}

	@AfterAll
	static void tearDownContext() {
		if (applicationContext != null) {
			applicationContext.close();
		}
	}

	@Test
	void shouldTranslateBadCredentialsFromAuthenticationManager() {
		OAuth2ResourceOwnerPasswordAuthenticationProvider provider = new OAuth2ResourceOwnerPasswordAuthenticationProvider(
				authentication -> {
					throw new BadCredentialsException("bad");
				}, mockAuthorizationService(), mockTokenGenerator());

		assertThatThrownBy(() -> provider.authenticate(buildAuthenticationToken()))
			.isInstanceOf(OAuth2AuthenticationException.class)
			.satisfies(ex -> assertThat(((OAuth2AuthenticationException) ex).getError().getErrorCode())
				.isEqualTo(OAuth2ErrorCodesExpand.BAD_CREDENTIALS));
	}

	@Test
	void shouldReturnServerErrorWhenAuthenticationManagerThrowsUnexpectedException() {
		OAuth2ResourceOwnerPasswordAuthenticationProvider provider = new OAuth2ResourceOwnerPasswordAuthenticationProvider(
				authentication -> {
					throw new IllegalStateException("boom");
				}, mockAuthorizationService(), mockTokenGenerator());

		assertThatThrownBy(() -> provider.authenticate(buildAuthenticationToken()))
			.isInstanceOf(OAuth2AuthenticationException.class)
			.satisfies(ex -> {
				OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) ex;
				assertThat(oauth2Exception.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.SERVER_ERROR);
				assertThat(oauth2Exception.getError().getDescription()).isEqualTo("boom");
			});
	}

	private OAuth2ResourceOwnerPasswordAuthenticationToken buildAuthenticationToken() {
		RegisteredClient registeredClient = RegisteredClient.withId("registered-client-id")
			.clientId("test")
			.clientSecret("{noop}test")
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.authorizationGrantType(new AuthorizationGrantType(SecurityConstants.PASSWORD))
			.build();
		OAuth2ClientAuthenticationToken clientAuthenticationToken = new OAuth2ClientAuthenticationToken(
				registeredClient, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "test");
		return new OAuth2ResourceOwnerPasswordAuthenticationToken(new AuthorizationGrantType(SecurityConstants.PASSWORD),
				clientAuthenticationToken, Set.of(),
				Map.of(CommonConstants.USERNAME, "admin", CommonConstants.PASSWORD, "123456"));
	}

	private OAuth2AuthorizationService mockAuthorizationService() {
		return mock(OAuth2AuthorizationService.class);
	}

	@SuppressWarnings("unchecked")
	private OAuth2TokenGenerator<?> mockTokenGenerator() {
		return mock(OAuth2TokenGenerator.class);
	}

}
