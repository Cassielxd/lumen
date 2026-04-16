package com.lumencloud.lumen.auth.endpoint;

import com.lumencloud.lumen.admin.api.feign.RemoteAuthSessionService;
import com.lumencloud.lumen.admin.api.feign.RemoteClientDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LumenTokenEndpointTest {

	@Test
	void requireAddsTokenDemoConfiguration() {
		LumenTokenEndpoint endpoint = new LumenTokenEndpoint(mock(OAuth2AuthorizationService.class),
				mock(RemoteClientDetailsService.class), mock(RemoteAuthSessionService.class), mock(CacheManager.class));

		ModelAndView modelAndView = endpoint.require(new ModelAndView(), "bad_credentials");

		assertThat(modelAndView.getViewName()).isEqualTo("ftl/login");
		assertThat(modelAndView.getModel()).containsEntry("error", "bad_credentials")
			.containsEntry("tokenDemoClientId", "test")
			.containsEntry("tokenDemoBasicAuth", "Basic dGVzdDp0ZXN0");
	}

}
