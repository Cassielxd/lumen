package com.lumencloud.lumen.admin.service.impl;

import com.lumencloud.lumen.admin.api.dto.UserDTO;
import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.vo.AuthAccountIdentifierManageVO;
import com.lumencloud.lumen.admin.api.entity.SysUser;
import com.lumencloud.lumen.admin.mapper.SysUserMapper;
import com.lumencloud.lumen.admin.mapper.SysUserPostMapper;
import com.lumencloud.lumen.admin.mapper.SysUserRoleMapper;
import com.lumencloud.lumen.admin.service.AuthAccountService;
import com.lumencloud.lumen.admin.service.SysDeptService;
import com.lumencloud.lumen.admin.service.SysMenuService;
import com.lumencloud.lumen.admin.service.SysPostService;
import com.lumencloud.lumen.admin.service.SysRoleService;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.exception.CheckedException;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.security.service.LumenUser;
import com.lumencloud.lumen.common.security.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserServiceImplTest {

	private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

	@Test
	void changePasswordShouldVerifyAgainstAccountCredentialFirst() throws Exception {
		SysUserMapper userMapper = mock(SysUserMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		SysUserServiceImpl service = createService(userMapper, authAccountService, new ConcurrentMapCacheManager(CacheConstants.USER_DETAILS));

		SysUser sysUser = new SysUser();
		sysUser.setUserId(1L);
		sysUser.setUsername("admin");
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
			verify(userMapper, never()).updateById(any(SysUser.class));
			verify(authAccountService).updatePasswordCredential(eq(200L), any(String.class), eq("admin"));
		}
	}

	@Test
	void checkPasswordShouldUseAccountCredentialWhenAvailable() throws Exception {
		SysUserMapper userMapper = mock(SysUserMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		SysUserServiceImpl service = createService(userMapper, authAccountService, mock(CacheManager.class));

		SysUser sysUser = new SysUser();
		sysUser.setUserId(1L);
		sysUser.setUsername("admin");
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
	void changePasswordShouldFailWhenAccountContextIsMissing() throws Exception {
		SysUserMapper userMapper = mock(SysUserMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		SysUserServiceImpl service = createService(userMapper, authAccountService, mock(CacheManager.class));

		UserDTO request = new UserDTO();
		request.setUsername("admin");
		request.setPassword("current-password");
		request.setNewpassword1("new-password");

		try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
			securityUtils.when(SecurityUtils::getUser).thenReturn(createUser(1L, null));

			R response = service.changePassword(request);

			assertThat(response.getCode()).isNotEqualTo(0);
			assertThat(response.getMsg()).contains("重新登录");
			verify(authAccountService, never()).updatePasswordCredential(any(), any(String.class), any(String.class));
			verify(authAccountService, never()).syncPasswordCredential(any(), any(String.class), any(String.class));
		}
	}

	@Test
	void checkPasswordShouldFailWhenAccountContextIsMissing() throws Exception {
		SysUserMapper userMapper = mock(SysUserMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		SysUserServiceImpl service = createService(userMapper, authAccountService, mock(CacheManager.class));

		try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
			securityUtils.when(SecurityUtils::getUser).thenReturn(createUser(1L, null));

			R response = service.checkPassword("current-password");

			assertThat(response.getCode()).isNotEqualTo(0);
			assertThat(response.getMsg()).contains("重新登录");
			verify(authAccountService, never()).getCredential(any(), eq("PASSWORD"));
		}
	}

	@Test
	void updateUserShouldRejectPasswordMutationThroughProfileEndpoint() throws Exception {
		SysUserServiceImpl service = createService(mock(SysUserMapper.class), mock(AuthAccountService.class), mock(CacheManager.class));

		UserDTO request = new UserDTO();
		request.setUserId(1L);
		request.setUsername("admin");
		request.setPassword("new-password");
		request.setClientIds(List.of("app"));

		assertThatThrownBy(() -> service.updateUser(request)).isInstanceOf(CheckedException.class)
			.hasMessageContaining("修改密码");
	}

	@Test
	void removeUserByIdsShouldEvictIdentifierCacheVariantsBeforeDeleting() throws Exception {
		SysUserMapper userMapper = mock(SysUserMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		CacheManager cacheManager = new ConcurrentMapCacheManager(CacheConstants.USER_DETAILS);
		SysUserServiceImpl service = createService(userMapper, authAccountService, cacheManager);

		SysUser user = new SysUser();
		user.setUserId(1L);
		user.setUsername("admin");
		user.setPhone("17034642999");
		user.setEmail("Admin@Example.com");

		AuthAccount account = new AuthAccount();
		account.setAccountId(200L);
		account.setClientId("app");

		AuthAccountIdentifierManageVO usernameIdentifier = new AuthAccountIdentifierManageVO();
		usernameIdentifier.setIdentifierType("USERNAME");
		usernameIdentifier.setIdentifierValue("admin");

		AuthAccountIdentifierManageVO aliasIdentifier = new AuthAccountIdentifierManageVO();
		aliasIdentifier.setIdentifierType("USERNAME");
		aliasIdentifier.setIdentifierValue("admin.alias");

		when(userMapper.selectByIds(anyList())).thenReturn(List.of(user));
		when(authAccountService.listByUserId(1L)).thenReturn(List.of(account));
		when(authAccountService.listIdentifiers(200L)).thenReturn(List.of(usernameIdentifier, aliasIdentifier));

		Cache cache = cacheManager.getCache(CacheConstants.USER_DETAILS);
		cache.put("app::password::admin", "cached");
		cache.put("app::password::admin.alias", "cached");
		cache.put("Admin@Example.com", "cached");

		Boolean result = service.removeUserByIds(new Long[] { 1L });

		assertThat(result).isTrue();
		assertThat(cache.get("app::password::admin")).isNull();
		assertThat(cache.get("app::password::admin.alias")).isNull();
		assertThat(cache.get("Admin@Example.com")).isNull();
		verify(authAccountService).removeByUserIds(eq(List.of(1L)));
	}

	private SysUserServiceImpl createService(SysUserMapper userMapper, AuthAccountService authAccountService,
			CacheManager cacheManager) throws Exception {
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
