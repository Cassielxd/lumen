package com.lumencloud.lumen.auth.endpoint;

import com.lumencloud.lumen.admin.api.dto.PasskeyCredentialPayload;
import com.lumencloud.lumen.admin.api.feign.RemotePasskeyService;
import com.lumencloud.lumen.admin.api.vo.PasskeyAccountInfoVO;
import com.lumencloud.lumen.admin.api.vo.PasskeyCredentialVO;
import com.lumencloud.lumen.auth.support.passkey.PasskeyAssertionOptionsRequest;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.security.annotation.Inner;
import com.lumencloud.lumen.common.security.passkey.PasskeyChallengeContext;
import com.lumencloud.lumen.common.security.passkey.PasskeyChallengeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasskeyEndpointTest {

	@Test
	void assertionOptionsCreatesChallengeAndAllowCredentials() {
		RemotePasskeyService remotePasskeyService = mock(RemotePasskeyService.class);
		PasskeyChallengeService passkeyChallengeService = mock(PasskeyChallengeService.class);
		PasskeyEndpoint endpoint = new PasskeyEndpoint(remotePasskeyService, passkeyChallengeService);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("https");
		request.setServerName("auth.example.com");
		request.setServerPort(443);
		request.addHeader("Authorization", "Basic bHVtZW46bnVsbA==");

		PasskeyAssertionOptionsRequest optionsRequest = new PasskeyAssertionOptionsRequest();
		optionsRequest.setUsername("alice");

		PasskeyCredentialPayload payload = new PasskeyCredentialPayload();
		payload.setTransports(List.of("internal"));
		PasskeyCredentialVO credential = new PasskeyCredentialVO();
		credential.setCredentialKey("credential-1");
		credential.setPayload(payload);
		PasskeyAccountInfoVO account = new PasskeyAccountInfoVO();
		account.setAccountId(10L);
		account.setUserId(20L);
		account.setClientId("lumen");
		account.setUsername("alice");
		account.setCredentials(List.of(credential));
		when(remotePasskeyService.getAccount(any())).thenReturn(R.ok(account));
		when(passkeyChallengeService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		R<Map<String, Object>> result = endpoint.assertionOptions(request, optionsRequest);

		assertThat(result.getCode()).isZero();
		assertThat(result.getData()).containsEntry("rpId", "auth.example.com")
			.containsEntry("userVerification", "preferred")
			.containsKey("challenge");
		assertThat(((Number) result.getData().get("timeout")).longValue())
			.isEqualTo(PasskeyChallengeService.DEFAULT_TTL_SECONDS * 1000);
		assertThat((List<?>) result.getData().get("allowCredentials")).singleElement().satisfies(item -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> descriptor = (Map<String, Object>) item;
			assertThat(descriptor).containsEntry("type", "public-key").containsEntry("id", "credential-1");
			assertThat(descriptor.get("transports")).isEqualTo(List.of("internal"));
		});

		ArgumentCaptor<PasskeyChallengeContext> challengeCaptor = ArgumentCaptor.forClass(PasskeyChallengeContext.class);
		verify(passkeyChallengeService).save(challengeCaptor.capture());
		PasskeyChallengeContext challengeContext = challengeCaptor.getValue();
		assertThat(challengeContext.getType()).isEqualTo(PasskeyChallengeContext.TYPE_ASSERTION);
		assertThat(challengeContext.getClientId()).isEqualTo("lumen");
		assertThat(challengeContext.getAccountId()).isEqualTo(10L);
		assertThat(challengeContext.getUserId()).isEqualTo(20L);
		assertThat(challengeContext.getUsername()).isEqualTo("alice");
		assertThat(challengeContext.getOrigin()).isEqualTo("https://auth.example.com");
		assertThat(challengeContext.getRpId()).isEqualTo("auth.example.com");
		assertThat(challengeContext.getChallenge()).isNotBlank();
	}

	@Test
	void assertionOptionsFailsWhenAccountHasNoPasskeys() {
		RemotePasskeyService remotePasskeyService = mock(RemotePasskeyService.class);
		PasskeyChallengeService passkeyChallengeService = mock(PasskeyChallengeService.class);
		PasskeyEndpoint endpoint = new PasskeyEndpoint(remotePasskeyService, passkeyChallengeService);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic bHVtZW46bnVsbA==");

		PasskeyAssertionOptionsRequest optionsRequest = new PasskeyAssertionOptionsRequest();
		optionsRequest.setUsername("alice");
		when(remotePasskeyService.getAccount(any())).thenReturn(R.ok(new PasskeyAccountInfoVO()));

		R<Map<String, Object>> result = endpoint.assertionOptions(request, optionsRequest);

		assertThat(result.getCode()).isEqualTo(1);
		assertThat(result.getMsg()).isEqualTo("passkey is unavailable for this account");
	}

	@Test
	void publicEndpointsAreMarkedPermitAll() throws NoSuchMethodException {
		assertThat(PasskeyEndpoint.class.getAnnotation(Inner.class)).isNotNull();
		assertThat(PasskeyEndpoint.class.getAnnotation(Inner.class).value()).isFalse();
		assertThat(ImageCodeEndpoint.class.getAnnotation(Inner.class)).isNotNull();
		assertThat(ImageCodeEndpoint.class.getAnnotation(Inner.class).value()).isFalse();

		Method method = PasskeyEndpoint.class.getMethod("assertionOptions", jakarta.servlet.http.HttpServletRequest.class,
				PasskeyAssertionOptionsRequest.class);
		assertThat(method.getAnnotation(Inner.class)).isNull();
	}

}
