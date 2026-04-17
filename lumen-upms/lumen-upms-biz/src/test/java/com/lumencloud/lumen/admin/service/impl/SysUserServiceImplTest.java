package com.lumencloud.lumen.admin.service.impl;

import com.lumencloud.lumen.admin.api.dto.UserDTO;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.entity.SysUser;
import com.lumencloud.lumen.admin.mapper.SysUserMapper;
import com.lumencloud.lumen.admin.mapper.SysUserPostMapper;
import com.lumencloud.lumen.admin.mapper.SysUserRoleMapper;
import com.lumencloud.lumen.admin.service.AuthAccountService;
import com.lumencloud.lumen.admin.service.SysDeptService;
import com.lumencloud.lumen.admin.service.SysMenuService;
import com.lumencloud.lumen.admin.service.SysPostService;
import com.lumencloud.lumen.admin.service.SysRoleService;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.security.service.LumenUser;
import com.lumencloud.lumen.common.security.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserServiceImplTest {

	private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

	@Test
	void changePasswordShouldVerifyAgainstAccountCredentialFirst() throws Exception {
		SysUserMapper userMapper = mock(SysUserMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		SysUserServiceImpl service = createService(userMapper, authAccountService);

		SysUser sysUser = new SysUser();
		sysUser.setUserId(1L);
		sysUser.setUsername("admin");
		sysUser.setPassword(ENCODER.encode("legacy-password"));
		sysUser.setUpdateBy("admin");
		when(userMapper.selectById(1L)).thenReturn(sysUser);

		AuthAccountCredential credential = new AuthAccountCredential();
		credential.setSecretValue(ENCODER.encode("current-password"));
		when(authAccountService.getCredential(200L, "PASSWORD")).thenReturn(Optional.of(credential));

		UserDTO request = new UserDTO();
		request.setUsername("admin");
		request.setPassword("current-password");
		request.setNewpassword1("new-password");

		try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
			securityUtils.when(SecurityUtils::getUser).thenReturn(createUser(1L, 200L));

			R response = service.changePassword(request);

			assertThat(response.getCode()).isEqualTo(0);
			verify(userMapper).updateById(any(SysUser.class));
			verify(authAccountService).updatePasswordCredential(eq(200L), any(String.class), eq("admin"));
		}
	}

	@Test
	void checkPasswordShouldUseAccountCredentialWhenAvailable() throws Exception {
		SysUserMapper userMapper = mock(SysUserMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		SysUserServiceImpl service = createService(userMapper, authAccountService);

		SysUser sysUser = new SysUser();
		sysUser.setUserId(1L);
		sysUser.setUsername("admin");
		sysUser.setPassword(ENCODER.encode("legacy-password"));
		when(userMapper.selectById(1L)).thenReturn(sysUser);

		AuthAccountCredential credential = new AuthAccountCredential();
		credential.setSecretValue(ENCODER.encode("current-password"));
		when(authAccountService.getCredential(200L, "PASSWORD")).thenReturn(Optional.of(credential));

		try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
			securityUtils.when(SecurityUtils::getUser).thenReturn(createUser(1L, 200L));

			R response = service.checkPassword("current-password");

			assertThat(response.getCode()).isEqualTo(0);
		}
	}

	@Test
	void updateUserShouldSyncPasswordOnlyForSpecifiedClients() throws Exception {
		SysUserMapper userMapper = mock(SysUserMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		SysUserServiceImpl service = createService(userMapper, authAccountService);

		SysUser latestUser = new SysUser();
		latestUser.setUserId(1L);
		latestUser.setUsername("admin");
		latestUser.setPhone("17034642999");
		latestUser.setPassword(ENCODER.encode("new-password"));
		latestUser.setLockFlag("0");
		latestUser.setUpdateBy("platform-admin");

		when(userMapper.selectById(1L)).thenReturn(latestUser);

		UserDTO request = new UserDTO();
		request.setUserId(1L);
		request.setUsername("admin");
		request.setPassword("new-password");
		request.setClientIds(List.of("app"));

		Boolean result = service.updateUser(request);

		assertThat(result).isTrue();
		verify(authAccountService).ensureUserAccounts(eq(latestUser), eq(List.of("app")));
		verify(authAccountService).syncPasswordCredentialForClients(eq(1L), eq(List.of("app")),
				eq(latestUser.getPassword()), eq("platform-admin"));
	}

	private SysUserServiceImpl createService(SysUserMapper userMapper, AuthAccountService authAccountService)
			throws Exception {
		CacheManager cacheManager = mock(CacheManager.class);
		SysUserServiceImpl service = new SysUserServiceImpl(mock(SysMenuService.class), mock(SysRoleService.class),
				mock(SysPostService.class), mock(SysDeptService.class), mock(SysUserRoleMapper.class),
				mock(SysUserPostMapper.class), cacheManager, authAccountService);
		setField(service, "baseMapper", userMapper);
		return service;
	}

	private LumenUser createUser(Long userId, Long accountId) {
		return new LumenUser(userId, 1L, accountId, "app", "admin", "ignored", "17034642999", true, true, true, true,
				List.of());
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Class<?> type = target.getClass();
		while (type != null) {
			try {
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				field.set(target, value);
				return;
			}
			catch (NoSuchFieldException ignored) {
				type = type.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

}
