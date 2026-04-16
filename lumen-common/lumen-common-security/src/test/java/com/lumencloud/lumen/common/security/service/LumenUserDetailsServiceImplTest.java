package com.lumencloud.lumen.common.security.service;

import com.lumencloud.lumen.admin.api.dto.UserDTO;
import com.lumencloud.lumen.admin.api.dto.UserInfo;
import com.lumencloud.lumen.admin.api.feign.RemoteUserService;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.util.R;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LumenUserDetailsServiceImplTest {

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void shouldResolveClientIdFromRequestParameter() {
		RemoteUserService remoteUserService = mock(RemoteUserService.class);
		CacheManager cacheManager = new ConcurrentMapCacheManager(CacheConstants.USER_DETAILS);
		LumenUserDetailsServiceImpl service = new LumenUserDetailsServiceImpl(remoteUserService, cacheManager);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("client_id", "test");
		request.setParameter("grant_type", "password");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		UserInfo userInfo = new UserInfo();
		userInfo.setUserId(1L);
		userInfo.setAccountId(100006L);
		userInfo.setAccountClientId("test");
		userInfo.setUsername("admin");
		userInfo.setPassword("$2a$10$c/Ae0pRjJtMZg3BnvVpO.eIK6WYWVbKTzqgdy3afR7w.vd.xi3Mgy");
		userInfo.setPhone("17034642999");
		userInfo.setLockFlag(CommonConstants.STATUS_NORMAL);
		when(remoteUserService.info(any(UserDTO.class))).thenReturn(R.ok(userInfo));

		LumenUser lumenUser = (LumenUser) service.loadUserByUsername("admin");

		ArgumentCaptor<UserDTO> userCaptor = ArgumentCaptor.forClass(UserDTO.class);
		verify(remoteUserService).info(userCaptor.capture());
		assertThat(userCaptor.getValue().getClientId()).isEqualTo("test");
		assertThat(userCaptor.getValue().getGrantType()).isEqualTo("password");
		assertThat(lumenUser.getAccountClientId()).isEqualTo("test");
		assertThat(lumenUser.getUsername()).isEqualTo("admin");
	}

}
