package com.lumencloud.lumen.admin.service.impl;

import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.entity.AuthAccountIdentifier;
import com.lumencloud.lumen.admin.api.entity.SysUser;
import com.lumencloud.lumen.admin.mapper.AuthAccountCredentialMapper;
import com.lumencloud.lumen.admin.mapper.AuthAccountIdentifierMapper;
import com.lumencloud.lumen.admin.mapper.AuthAccountMapper;
import com.lumencloud.lumen.admin.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthAccountServiceImplTest {

	@Test
	void resolveAccountUsesIdentifierModelFirst() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper,
				sysUserMapper);

		AuthAccountIdentifier identifier = new AuthAccountIdentifier();
		identifier.setAccountId(1001L);
		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setClientId("app");
		account.setLoginName("admin");

		when(identifierMapper.selectOne(any(), anyBoolean())).thenReturn(identifier);
		when(accountMapper.selectById(1001L)).thenReturn(account);

		Optional<AuthAccount> result = service.resolveAccount("app", "admin", null);

		assertThat(result).isPresent();
		assertThat(result.get().getAccountId()).isEqualTo(1001L);
	}

	@Test
	void getCredentialFallsBackToAnyCredentialKeyForPasskeyStyleCredentials() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper,
				sysUserMapper);

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
	void getCredentialPrefersDefaultCredentialKeyWhenPresent() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper,
				sysUserMapper);

		AuthAccountCredential passwordCredential = new AuthAccountCredential();
		passwordCredential.setCredentialId(7L);
		passwordCredential.setCredentialType("PASSWORD");
		passwordCredential.setCredentialKey("");

		when(credentialMapper.selectOne(any(), anyBoolean())).thenReturn(passwordCredential);

		Optional<AuthAccountCredential> result = service.getCredential(200L, "PASSWORD");

		assertThat(result).isPresent();
		assertThat(result.get().getCredentialKey()).isEmpty();
	}

	@Test
	void resetPasswordSynchronizesLegacyUserShadowAndOnlyCurrentAccountCredential() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper,
				sysUserMapper);

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setUserId(2002L);
		account.setClientId("app");
		account.setStatus("0");

		when(accountMapper.selectById(1001L)).thenReturn(account);
		when(credentialMapper.selectOne(any(), anyBoolean())).thenReturn(null);

		Boolean result = service.resetPassword(1001L, "123456", "admin");

		assertThat(result).isTrue();
		verify(sysUserMapper).updateById(any(SysUser.class));
		verify(credentialMapper).insert(any(AuthAccountCredential.class));
		verify(accountMapper, never()).selectList(any());
	}

	@Test
	void updatePasswordCredentialShouldOnlyUpdateSpecifiedAccount() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper,
				sysUserMapper);

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setClientId("app");
		account.setStatus("0");

		when(accountMapper.selectById(1001L)).thenReturn(account);
		when(credentialMapper.selectOne(any(), anyBoolean())).thenReturn(null);

		service.updatePasswordCredential(1001L, "$2a$10$encoded", "admin");

		verify(credentialMapper).insert(any(AuthAccountCredential.class));
		verify(sysUserMapper, never()).updateById(any(SysUser.class));
	}

	@Test
	void syncPasswordCredentialForClientsShouldOnlyAffectSelectedClients() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper,
				sysUserMapper);

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

		service.syncPasswordCredentialForClients(2002L, Set.of("app"), "$2a$10$encoded", "admin");

		verify(credentialMapper).insert(any(AuthAccountCredential.class));
		verify(credentialMapper, times(1)).insert(any(AuthAccountCredential.class));
	}

	@Test
	void updateOtpStatusShouldRejectEnableWhenPhoneIsMissing() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper,
				sysUserMapper);

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setStatus("0");

		when(accountMapper.selectById(1001L)).thenReturn(account);

		Boolean result = service.updateOtpStatus(1001L, "0", "admin");

		assertThat(result).isFalse();
	}

	@Test
	void clearPasskeysShouldLockOnlyNormalCredentialsOfCurrentAccount() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper,
				sysUserMapper);

		AuthAccountCredential passkey = new AuthAccountCredential();
		passkey.setCredentialId(9001L);
		passkey.setAccountId(1001L);
		passkey.setCredentialType("PASSKEY");
		passkey.setStatus("0");

		when(credentialMapper.selectList(any())).thenReturn(List.of(passkey));

		Boolean result = service.clearPasskeys(1001L, "admin");

		assertThat(result).isTrue();
		verify(credentialMapper).updateById(any(AuthAccountCredential.class));
	}

	@Test
	void saveIdentifierShouldCreateSecondaryAliasUnderSameClient() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper,
				sysUserMapper);

		AuthAccount account = new AuthAccount();
		account.setAccountId(1001L);
		account.setClientId("lumen");
		account.setStatus("0");

		when(accountMapper.selectById(1001L)).thenReturn(account);
		when(identifierMapper.selectOne(any(), anyBoolean())).thenReturn(null);

		Boolean result = service.saveIdentifier(1001L, "email", "Admin@Example.com", "admin");

		assertThat(result).isTrue();
		verify(identifierMapper).insert(any(AuthAccountIdentifier.class));
	}

	@Test
	void removeIdentifierShouldRejectPrimaryIdentifier() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper,
				sysUserMapper);

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
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountIdentifierMapper identifierMapper = mock(AuthAccountIdentifierMapper.class);
		SysUserMapper sysUserMapper = mock(SysUserMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper, identifierMapper,
				sysUserMapper);

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
}
