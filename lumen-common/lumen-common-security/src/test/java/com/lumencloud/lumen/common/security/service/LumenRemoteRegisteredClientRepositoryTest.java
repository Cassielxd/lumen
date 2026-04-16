package com.lumencloud.lumen.common.security.service;

import com.lumencloud.lumen.admin.api.entity.SysOauthClientDetails;
import com.lumencloud.lumen.admin.api.feign.RemoteClientDetailsService;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.util.R;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LumenRemoteRegisteredClientRepositoryTest {

	@Test
	void shouldReadLatestClientDefinitionInMonolithMode() {
		RemoteClientDetailsService remoteClientDetailsService = mock(RemoteClientDetailsService.class);
		when(remoteClientDetailsService.getClientDetailsById("app"))
			.thenReturn(R.ok(client("app", "password")))
			.thenReturn(R.ok(client("app", "otp")));

		LumenRemoteRegisteredClientRepository repository = new LumenRemoteRegisteredClientRepository(
				remoteClientDetailsService, new ConcurrentMapCacheManager(CacheConstants.CLIENT_DETAILS_KEY));
		repository.setMicro(false);

		RegisteredClient first = repository.findByClientId("app");
		RegisteredClient second = repository.findByClientId("app");

		assertEquals(Set.of("password"), grantTypes(first));
		assertEquals(Set.of("otp"), grantTypes(second));
		verify(remoteClientDetailsService, times(2)).getClientDetailsById("app");
	}

	@Test
	void shouldCacheClientDefinitionInMicroMode() {
		RemoteClientDetailsService remoteClientDetailsService = mock(RemoteClientDetailsService.class);
		when(remoteClientDetailsService.getClientDetailsById("app"))
			.thenReturn(R.ok(client("app", "password")))
			.thenReturn(R.ok(client("app", "otp")));

		LumenRemoteRegisteredClientRepository repository = new LumenRemoteRegisteredClientRepository(
				remoteClientDetailsService, new ConcurrentMapCacheManager(CacheConstants.CLIENT_DETAILS_KEY));
		repository.setMicro(true);

		RegisteredClient first = repository.findByClientId("app");
		RegisteredClient second = repository.findByClientId("app");

		assertEquals(Set.of("password"), grantTypes(first));
		assertEquals(Set.of("password"), grantTypes(second));
		assertNotEquals(Set.of("otp"), grantTypes(second));
		verify(remoteClientDetailsService, times(1)).getClientDetailsById("app");
	}

	private static SysOauthClientDetails client(String clientId, String... grantTypes) {
		SysOauthClientDetails client = new SysOauthClientDetails();
		client.setClientId(clientId);
		client.setClientSecret(clientId);
		client.setScope("server");
		client.setAuthorizedGrantTypes(grantTypes);
		return client;
	}

	private static Set<String> grantTypes(RegisteredClient client) {
		return client.getAuthorizationGrantTypes()
			.stream()
			.map(AuthorizationGrantType::getValue)
			.collect(Collectors.toSet());
	}

}
