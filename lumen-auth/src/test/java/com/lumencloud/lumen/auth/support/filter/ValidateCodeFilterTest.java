package com.lumencloud.lumen.auth.support.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidateCodeFilterTest {

	@Test
	void ignoresBrowserGetTokenRequest() throws ServletException, IOException {
		ValidateCodeFilter filter = new ValidateCodeFilter(properties(List.of("test")));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		filter.doFilter(request, response, filterChain);

		assertThat(filterChain.getRequest()).isSameAs(request);
	}

	@Test
	void ignoresConfiguredClientWhenClientIdIsProvidedAsRequestParameter() throws ServletException, IOException {
		ValidateCodeFilter filter = new ValidateCodeFilter(properties(List.of("test")));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
		request.addParameter("grant_type", "password");
		request.addParameter("client_id", "test");
		request.addParameter("username", "admin");
		request.addParameter("password", "123456");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		filter.doFilter(request, response, filterChain);

		assertThat(filterChain.getRequest()).isSameAs(request);
	}

	private static AuthSecurityConfigProperties properties(List<String> ignoreClients) {
		AuthSecurityConfigProperties properties = new AuthSecurityConfigProperties();
		properties.setIgnoreClients(ignoreClients);
		return properties;
	}

}
