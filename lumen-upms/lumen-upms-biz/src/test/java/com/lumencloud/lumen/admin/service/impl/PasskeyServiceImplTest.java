package com.lumencloud.lumen.admin.service.impl;

import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.mapper.AuthAccountCredentialMapper;
import com.lumencloud.lumen.admin.mapper.AuthAccountMapper;
import com.lumencloud.lumen.admin.service.AuthAccountService;
import com.lumencloud.lumen.common.security.passkey.PasskeyChallengeContext;
import com.lumencloud.lumen.common.security.passkey.PasskeyChallengeService;
import com.lumencloud.lumen.common.security.passkey.PasskeyWebAuthnUtils;
import com.lumencloud.lumen.common.security.service.LumenUser;
import com.lumencloud.lumen.common.security.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasskeyServiceImplTest {

	@Test
	void createCurrentRegistrationOptionsUsesIdentifierScopedUserHandleAndDisplayName() {
		AuthAccountMapper accountMapper = mock(AuthAccountMapper.class);
		AuthAccountCredentialMapper credentialMapper = mock(AuthAccountCredentialMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		PasskeyChallengeService challengeService = mock(PasskeyChallengeService.class);
		PasskeyServiceImpl service = new PasskeyServiceImpl(accountMapper, credentialMapper, authAccountService,
				challengeService);

		AuthAccount account = new AuthAccount();
		account.setAccountId(100001L);
		account.setUserId(1L);
		account.setClientId("app");
		when(accountMapper.selectById(100001L)).thenReturn(account);
		when(credentialMapper.selectList(any())).thenReturn(Collections.emptyList());
		when(authAccountService.getPrimaryIdentifierValue(100001L, "USERNAME")).thenReturn(Optional.of("admin"));

		LumenUser currentUser = new LumenUser(1L, 1L, 100001L, "app", "admin", "{noop}ignored", "17034642999", true,
				true, true, true, Collections.emptyList());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(9999);

		try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
			securityUtils.when(SecurityUtils::getUser).thenReturn(currentUser);

			Map<String, Object> options = service.createCurrentRegistrationOptions(request);

			@SuppressWarnings("unchecked")
			Map<String, Object> user = (Map<String, Object>) options.get("user");
			assertThat(ByteBuffer.wrap(PasskeyWebAuthnUtils.decodeBase64Url((String) user.get("id"))).getLong())
				.isEqualTo(100001L);
			assertThat(user.get("name")).isEqualTo("app:admin");
			assertThat(user.get("displayName")).isEqualTo("app / admin");
		}

		ArgumentCaptor<PasskeyChallengeContext> challengeCaptor = ArgumentCaptor.forClass(PasskeyChallengeContext.class);
		verify(challengeService).save(challengeCaptor.capture());
		assertThat(challengeCaptor.getValue().getDisplayName()).isEqualTo("app / admin");
	}

}
