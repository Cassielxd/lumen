package com.lumencloud.lumen.config;

import com.lumencloud.lumen.admin.api.dto.AuthSessionLogoutDTO;
import com.lumencloud.lumen.admin.api.dto.AuthSessionSaveDTO;
import com.lumencloud.lumen.admin.api.dto.UserDTO;
import com.lumencloud.lumen.admin.api.dto.UserInfo;
import com.lumencloud.lumen.admin.api.entity.AuthSession;
import com.lumencloud.lumen.admin.api.feign.RemoteAuthSessionService;
import com.lumencloud.lumen.admin.api.feign.RemoteUserService;
import com.lumencloud.lumen.admin.service.AuthSessionService;
import com.lumencloud.lumen.admin.service.SysUserService;
import com.lumencloud.lumen.common.feign.LumenFeignAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonolithRemoteServiceConfigurationTest {

	private final MonolithRemoteServiceConfiguration configuration = new MonolithRemoteServiceConfiguration();

	private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LumenFeignAutoConfiguration.class));

	@Test
	void shouldDelegateUserLookupToLocalService() {
		SysUserService sysUserService = mock(SysUserService.class);
		RemoteUserService remoteUserService = configuration.remoteUserService(sysUserService);
		UserDTO request = new UserDTO();
		request.setUsername("admin");

		UserInfo userInfo = new UserInfo();
		userInfo.setUsername("admin");
		when(sysUserService.getUserInfo(request)).thenReturn(com.lumencloud.lumen.common.core.util.R.ok(userInfo));

		UserInfo result = remoteUserService.info(request).getData();

		verify(sysUserService).getUserInfo(request);
		assertThat(result.getUsername()).isEqualTo("admin");
	}

	@Test
	void shouldDelegateSessionOperationsToLocalService() {
		AuthSessionService authSessionService = mock(AuthSessionService.class);
		RemoteAuthSessionService remoteAuthSessionService = configuration.remoteAuthSessionService(authSessionService);

		AuthSessionSaveDTO saveRequest = new AuthSessionSaveDTO();
		saveRequest.setClientId("test");
		AuthSession session = new AuthSession();
		session.setSid("sid-1");
		when(authSessionService.saveSession(saveRequest)).thenReturn(session);

		AuthSessionLogoutDTO tokenRequest = new AuthSessionLogoutDTO();
		tokenRequest.setAccessToken("access-token");
		when(authSessionService.logoutByAccessToken("access-token")).thenReturn(Boolean.TRUE);
		when(authSessionService.getByAccessToken("access-token")).thenReturn(session);

		AuthSessionLogoutDTO refreshRequest = new AuthSessionLogoutDTO();
		refreshRequest.setRefreshToken("refresh-token");
		when(authSessionService.getByRefreshToken("refresh-token")).thenReturn(session);

		assertThat(remoteAuthSessionService.saveSession(saveRequest).getData().getSid()).isEqualTo("sid-1");
		assertThat(remoteAuthSessionService.logout(tokenRequest).getData()).isTrue();
		assertThat(remoteAuthSessionService.getByAccessToken(tokenRequest).getData().getSid()).isEqualTo("sid-1");
		assertThat(remoteAuthSessionService.getByRefreshToken(refreshRequest).getData().getSid()).isEqualTo("sid-1");

		verify(authSessionService).saveSession(saveRequest);
		verify(authSessionService).logoutByAccessToken("access-token");
		verify(authSessionService).getByAccessToken("access-token");
		verify(authSessionService).getByRefreshToken("refresh-token");
	}

	@Test
	void shouldDisableFeignAutoConfigurationInMonolithMode() {
		applicationContextRunner.withPropertyValues("security.micro=false").run((context) -> {
			assertThat(context).doesNotHaveBean("lumenFeignRequestCloseInterceptor");
			assertThat(context).doesNotHaveBean("lumenFeignInnerRequestInterceptor");
		});
	}

	@Test
	void shouldEnableFeignAutoConfigurationInMicroMode() {
		applicationContextRunner.withPropertyValues("security.micro=true").run((context) -> {
			assertThat(context).hasBean("lumenFeignRequestCloseInterceptor");
			assertThat(context).hasBean("lumenFeignInnerRequestInterceptor");
		});
	}

}
