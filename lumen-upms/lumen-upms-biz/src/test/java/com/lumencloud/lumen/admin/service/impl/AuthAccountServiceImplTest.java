package com.lumencloud.lumen.admin.service.impl;

import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.mapper.AuthAccountCredentialMapper;
import com.lumencloud.lumen.admin.mapper.AuthAccountMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthAccountServiceImplTest {

	@Test
	void getCredentialFallsBackToAnyCredentialKeyForPasskeyStyleCredentials() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper);

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
		AuthAccountServiceImpl service = new AuthAccountServiceImpl(accountMapper, credentialMapper);

		AuthAccountCredential passwordCredential = new AuthAccountCredential();
		passwordCredential.setCredentialId(7L);
		passwordCredential.setCredentialType("PASSWORD");
		passwordCredential.setCredentialKey("");

		when(credentialMapper.selectOne(any(), anyBoolean())).thenReturn(passwordCredential);

		Optional<AuthAccountCredential> result = service.getCredential(200L, "PASSWORD");

		assertThat(result).isPresent();
		assertThat(result.get().getCredentialKey()).isEmpty();
	}
}
