package com.lumencloud.lumen.admin.service.impl;

import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.entity.AuthAccountIdentifier;
import com.lumencloud.lumen.admin.api.entity.SysUser;
import com.lumencloud.lumen.admin.mapper.AuthAccountCredentialMapper;
import com.lumencloud.lumen.admin.mapper.AuthAccountIdentifierMapper;
import com.lumencloud.lumen.admin.mapper.AuthAccountMapper;
import com.lumencloud.lumen.admin.mapper.SysUserMapper;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthAccountServiceImplTest {

	@Test
	void resolveAccountUsesIdentifierModelFirst() {
		AuthAccountServiceImpl service = createService(mock(AuthAccountMapper.class), mock(AuthAccountCredentialMapper.class),
				mock(AuthAccountIdentifierMapper.class), mock(SysUserMapper.class));

		AuthAccountIdentifier identifier = new AuthAccountIdentifier();
		identifier.setAccountId(1001L);
		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setClientId("app");

		when(serviceResolveIdentifierMapper(service).selectOne(any(), anyBoolean())).thenReturn(identifier);
		when(serviceAccountMapper(service).selectById(1001L)).thenReturn(account);

		Optional<AuthAccount> result = service.resolveAccount("app", "admin", null);

		assertThat(result).isPresent();
		assertThat(result.get().getAccountId()).isEqualTo(1001L);
	}

	@Test
	void getCredentialFallsBackToAnyCredentialKeyForPasskeyStyleCredentials() {
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountServiceImpl service = createService(mock(AuthAccountMapper.class), credentialMapper,
				mock(AuthAccountIdentifierMapper.class), mock(SysUserMapper.class));

		AuthAccountCredential passkeyCredential = new AuthAccountCredential();
		passkeyCredential.setCredentialId(12L);
		passkeyCredential.setCredentialType("PASSKEY");
		passkeyCredential.setCredentialKey("credential-id-1");

		when(credentialMapper.selectOne(any(), anyBoolean())).thenReturn(null, passkeyCredential);

		Optional<AuthAccountCredential> result = service.getCredential(100L, "PASSKEY");

		assertThat(result).isPresent();
		assertThat(result.get().getCredentialKey()).isEqualTo("credential-id-1");
	}

	@Test
	void resetPasswordShouldOnlyUpdateCurrentAccountCredential() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		AuthAccountServiceImpl service = createService(accountMapper, credentialMapper, identifierMapper, sysUserMapper);

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setUserId(2002L);
		account.setClientId("app");
		account.setStatus("0");

		when(accountMapper.selectById(1001L)).thenReturn(account);
		when(credentialMapper.selectOne(any(), anyBoolean())).thenReturn(null);
		when(identifierMapper.selectList(any())).thenReturn(List.of());

		Boolean result = service.resetPassword(1001L, "123456", "admin");

		assertThat(result).isTrue();
		verify(credentialMapper).insert(any(AuthAccountCredential.class));
		verify(sysUserMapper, never()).updateById(any(SysUser.class));
		verify(accountMapper, never()).selectList(any());
	}

	@Test
	void syncPasswordCredentialForClientsShouldOnlyAffectSelectedClients() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		AuthAccountServiceImpl service = createService(accountMapper, credentialMapper, identifierMapper,
				mock(SysUserMapper.class));

		AuthAccount appAccount = new AuthAccount();
		appAccount.setAccountId(1001L);
		appAccount.setUserId(2002L);
		appAccount.setClientId("app");
		appAccount.setStatus("0");

		AuthAccount lumenAccount = new AuthAccount();
		lumenAccount.setAccountId(1002L);
		lumenAccount.setUserId(2002L);
		lumenAccount.setClientId("lumen");
		lumenAccount.setStatus("0");

		when(accountMapper.selectList(any())).thenReturn(List.of(appAccount, lumenAccount));
		when(credentialMapper.selectOne(any(), anyBoolean())).thenReturn(null);
		when(identifierMapper.selectList(any())).thenReturn(List.of());

		service.syncPasswordCredentialForClients(2002L, Set.of("app"), "$2a$10$encoded", "admin");

		verify(credentialMapper, times(1)).insert(any(AuthAccountCredential.class));
	}

	@Test
	void updateOtpStatusShouldRejectEnableWhenPhoneIdentifierIsMissing() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		AuthAccountServiceImpl service = createService(accountMapper, mock(AuthAccountCredentialMapper.class),
				identifierMapper, mock(SysUserMapper.class));

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setStatus("0");
		when(accountMapper.selectById(1001L)).thenReturn(account);
		when(identifierMapper.selectOne(any(), anyBoolean())).thenReturn(null, null);

		Boolean result = service.updateOtpStatus(1001L, "0", "admin");

		assertThat(result).isFalse();
	}

	@Test
	void saveIdentifierShouldCreateSecondaryAliasUnderSameClient() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		AuthAccountServiceImpl service = createService(accountMapper, mock(AuthAccountCredentialMapper.class),
				identifierMapper, mock(SysUserMapper.class));

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setClientId("lumen");
		account.setStatus("0");

		when(accountMapper.selectById(1001L)).thenReturn(account);
		when(identifierMapper.selectOne(any(), anyBoolean())).thenReturn(null);
		when(identifierMapper.selectList(any())).thenReturn(List.of());

		Boolean result = service.saveIdentifier(1001L, "email", "Admin@Example.com", "admin");

		assertThat(result).isTrue();
		verify(identifierMapper).insert(any(AuthAccountIdentifier.class));
	}

	@Test
	void removeIdentifierShouldRejectPrimaryIdentifier() {
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		AuthAccountServiceImpl service = createService(mock(AuthAccountMapper.class), mock(AuthAccountCredentialMapper.class),
				identifierMapper, mock(SysUserMapper.class));

		AuthAccountIdentifier identifier = new AuthAccountIdentifier();
		identifier.setIdentifierId(2001L);
		identifier.setPrimaryFlag("1");
		when(identifierMapper.selectById(2001L)).thenReturn(identifier);

		Boolean result = service.removeIdentifier(2001L, "admin");

		assertThat(result).isFalse();
		verify(identifierMapper, never()).updateById(any(AuthAccountIdentifier.class));
	}

	@Test
	void syncUserProfileShouldKeepSecondaryAliasesWhileRefreshingPrimaryUsername() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		AuthAccountServiceImpl service = createService(accountMapper, mock(AuthAccountCredentialMapper.class),
				identifierMapper, mock(SysUserMapper.class));

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setUserId(2002L);
		account.setClientId("lumen");
		account.setStatus("0");

		AuthAccountIdentifier primaryUsername = new AuthAccountIdentifier();
		primaryUsername.setIdentifierId(3001L);
		primaryUsername.setAccountId(1001L);
		primaryUsername.setIdentifierType("USERNAME");
		primaryUsername.setIdentifierValue("old-admin");
		primaryUsername.setPrimaryFlag("1");

		AuthAccountIdentifier aliasUsername = new AuthAccountIdentifier();
		aliasUsername.setIdentifierId(3002L);
		aliasUsername.setAccountId(1001L);
		aliasUsername.setIdentifierType("USERNAME");
		aliasUsername.setIdentifierValue("admin.alias");
		aliasUsername.setPrimaryFlag("0");

		when(accountMapper.selectList(any())).thenReturn(List.of(account));
		when(identifierMapper.selectList(any())).thenReturn(List.of(primaryUsername, aliasUsername), List.of(), List.of());

		SysUser user = new SysUser();
		user.setUserId(2002L);
		user.setUsername("new-admin");
		user.setPhone("17034642999");
		user.setEmail("admin@example.com");
		user.setLockFlag("0");
		user.setUpdateBy("admin");

		service.syncUserProfile(user);

		verify(identifierMapper, times(2)).updateById(any(AuthAccountIdentifier.class));
		verify(identifierMapper, times(2)).insert(any(AuthAccountIdentifier.class));
		verify(identifierMapper, never()).deleteById(3002L);
	}

	@Test
	void resolveAccountShouldSupportEmailIdentifierLogin() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		AuthAccountServiceImpl service = createService(accountMapper, mock(AuthAccountCredentialMapper.class),
				identifierMapper, mock(SysUserMapper.class));

		AuthAccountIdentifier emailIdentifier = new AuthAccountIdentifier();
		emailIdentifier.setAccountId(1001L);

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setClientId("app");

		when(identifierMapper.selectOne(any(), anyBoolean())).thenReturn(null, emailIdentifier);
		when(accountMapper.selectById(1001L)).thenReturn(account);

		Optional<AuthAccount> result = service.resolveAccount("app", "Admin@Example.com", null);

		assertThat(result).isPresent();
		assertThat(result.get().getAccountId()).isEqualTo(1001L);
		verify(identifierMapper, times(2)).selectOne(any(), anyBoolean());
	}

	@Test
	void resetPasswordShouldEvictCachedUserDetailsForKnownPrincipals() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		CacheManager cacheManager = new ConcurrentMapCacheManager(CacheConstants.USER_DETAILS);
		AuthAccountServiceImpl service = createService(accountMapper, credentialMapper, identifierMapper, sysUserMapper,
				cacheManager);

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setUserId(2002L);
		account.setClientId("app");
		account.setStatus("0");

		SysUser user = new SysUser();
		user.setUserId(2002L);
		user.setUsername("admin");
		user.setPhone("17034642999");
		user.setEmail("Admin@Example.com");

		AuthAccountIdentifier primaryUsername = identifier(1001L, "USERNAME", "admin", "1");
		AuthAccountIdentifier emailIdentifier = identifier(1001L, "EMAIL", "admin@example.com", "0");
		AuthAccountIdentifier aliasIdentifier = identifier(1001L, "USERNAME", "admin.alias", "0");

		when(accountMapper.selectById(1001L)).thenReturn(account);
		when(credentialMapper.selectOne(any(), anyBoolean())).thenReturn(null);
		when(sysUserMapper.selectById(2002L)).thenReturn(user);
		when(identifierMapper.selectList(any())).thenReturn(List.of(primaryUsername, emailIdentifier, aliasIdentifier));

		Cache cache = cacheManager.getCache(CacheConstants.USER_DETAILS);
		cache.put("app::password::admin", "cached");
		cache.put("app::otp::17034642999", "cached");
		cache.put("app::passkey::admin@example.com", "cached");
		cache.put("app::password::admin.alias", "cached");
		cache.put("Admin@Example.com", "cached");

		Boolean result = service.resetPassword(1001L, "123456", "admin");

		assertThat(result).isTrue();
		assertThat(cache.get("app::password::admin")).isNull();
		assertThat(cache.get("app::otp::17034642999")).isNull();
		assertThat(cache.get("app::passkey::admin@example.com")).isNull();
		assertThat(cache.get("app::password::admin.alias")).isNull();
		assertThat(cache.get("Admin@Example.com")).isNull();
	}

	@Test
	void updateOtpStatusShouldEvictCachedUserDetails() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		CacheManager cacheManager = new ConcurrentMapCacheManager(CacheConstants.USER_DETAILS);
		AuthAccountServiceImpl service = createService(accountMapper, credentialMapper, identifierMapper, sysUserMapper,
				cacheManager);

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setUserId(2002L);
		account.setClientId("app");
		account.setStatus("0");

		SysUser user = new SysUser();
		user.setUserId(2002L);
		user.setUsername("admin");
		user.setPhone("17034642999");

		AuthAccountIdentifier phoneIdentifier = identifier(1001L, "PHONE", "17034642999", "1");

		when(accountMapper.selectById(1001L)).thenReturn(account);
		when(credentialMapper.selectOne(any(), anyBoolean())).thenReturn(null);
		when(sysUserMapper.selectById(2002L)).thenReturn(user);
		when(identifierMapper.selectOne(any(), anyBoolean())).thenReturn(phoneIdentifier);
		when(identifierMapper.selectList(any())).thenReturn(List.of(phoneIdentifier));

		Cache cache = cacheManager.getCache(CacheConstants.USER_DETAILS);
		cache.put("app::otp::17034642999", "cached");
		cache.put("17034642999", "cached");

		Boolean result = service.updateOtpStatus(1001L, "9", "admin");

		assertThat(result).isTrue();
		assertThat(cache.get("app::otp::17034642999")).isNull();
		assertThat(cache.get("17034642999")).isNull();
	}

	@Test
	void syncUserProfileShouldFailFastWhenPrimaryIdentifierCollides() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		AuthAccountServiceImpl service = createService(accountMapper, mock(AuthAccountCredentialMapper.class),
				identifierMapper, mock(SysUserMapper.class));

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setUserId(2002L);
		account.setClientId("lumen");
		account.setStatus("0");

		AuthAccountIdentifier duplicateEmail = new AuthAccountIdentifier();
		duplicateEmail.setAccountId(3003L);
		duplicateEmail.setIdentifierType("EMAIL");
		duplicateEmail.setIdentifierValue("admin@example.com");

		when(accountMapper.selectList(any())).thenReturn(List.of(account));
		when(identifierMapper.selectOne(any(), anyBoolean())).thenReturn(null, null, duplicateEmail);
		when(identifierMapper.selectList(any())).thenReturn(List.of(), List.of());

		SysUser user = new SysUser();
		user.setUserId(2002L);
		user.setUsername("new-admin");
		user.setPhone("17034642999");
		user.setEmail("admin@example.com");
		user.setLockFlag("0");
		user.setUpdateBy("admin");

		assertThatThrownBy(() -> service.syncUserProfile(user)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("identifier already bound");
	}

	private AuthAccountIdentifier identifier(Long accountId, String type, String value, String primaryFlag) {
		AuthAccountIdentifier identifier = new AuthAccountIdentifier();
		identifier.setAccountId(accountId);
		identifier.setIdentifierType(type);
		identifier.setIdentifierValue(value);
		identifier.setPrimaryFlag(primaryFlag);
		return identifier;
	}

	private AuthAccountMapper serviceAccountMapper(AuthAccountServiceImpl service) {
		return readField(service, "authAccountMapper");
	}

	private AuthAccountIdentifierMapper serviceResolveIdentifierMapper(AuthAccountServiceImpl service) {
		return readField(service, "authAccountIdentifierMapper");
	}

	@SuppressWarnings("unchecked")
	private <T> T readField(Object target, String name) {
		try {
			var field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return (T) field.get(target);
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private AuthAccountServiceImpl createService(AuthAccountMapper accountMapper,
			AuthAccountCredentialMapper credentialMapper, AuthAccountIdentifierMapper identifierMapper,
			SysUserMapper sysUserMapper) {
		return createService(accountMapper, credentialMapper, identifierMapper, sysUserMapper,
				new ConcurrentMapCacheManager(CacheConstants.USER_DETAILS));
	}

	private AuthAccountServiceImpl createService(AuthAccountMapper accountMapper,
			AuthAccountCredentialMapper credentialMapper, AuthAccountIdentifierMapper identifierMapper,
			SysUserMapper sysUserMapper, CacheManager cacheManager) {
		return new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper, sysUserMapper, cacheManager);
	}

}
