package com.lumencloud.lumen.admin.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.vo.SmsCodeSendVO;
import com.lumencloud.lumen.admin.mapper.SysUserMapper;
import com.lumencloud.lumen.admin.service.AuthAccountService;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.exception.ErrorCodes;
import com.lumencloud.lumen.common.core.util.MsgUtils;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.core.util.RedisUtils;
import com.lumencloud.lumen.common.core.util.WebUtils;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SysMobileServiceImplTest {

	@Test
	void sendSmsCodeShouldReturnGeneratedCodeForDemoWhenSmsProviderIsUnavailable() {
		SysUserMapper userMapper = mock(SysUserMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		SysMobileServiceImpl service = new SysMobileServiceImpl(userMapper, authAccountService);

		AuthAccount account = new AuthAccount();
		account.setAccountId(100001L);
		account.setClientId("app");
		account.setStatus(CommonConstants.STATUS_NORMAL);
		AuthAccountCredential credential = new AuthAccountCredential();
		credential.setStatus(CommonConstants.STATUS_NORMAL);

		when(authAccountService.resolveAccount("app", null, "17034642999")).thenReturn(Optional.of(account));
		when(authAccountService.getCredential(100001L, "OTP")).thenReturn(Optional.of(credential));

		try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class);
				MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class);
				MockedStatic<RandomUtil> randomUtil = mockStatic(RandomUtil.class);
				MockedStatic<SmsFactory> smsFactory = mockStatic(SmsFactory.class)) {
			webUtils.when(WebUtils::findClientId).thenReturn(Optional.of("app"));
			redisUtils.when(() -> RedisUtils.get(CacheConstants.DEFAULT_CODE_KEY + "17034642999")).thenReturn(null);
			randomUtil.when(() -> RandomUtil.randomNumbers(6)).thenReturn("123456");
			smsFactory.when(SmsFactory::getSmsBlend).thenReturn(null);

			R<SmsCodeSendVO> result = service.sendSmsCode("17034642999");

			assertThat(result.getCode()).isEqualTo(0);
			assertThat(result.getData().getCode()).isEqualTo("123456");
			assertThat(result.getData().getReused()).isFalse();
			assertThat(result.getData().getDelivered()).isFalse();
			assertThat(result.getData().getExpiresInSeconds()).isEqualTo(SecurityConstants.CODE_TIME);
			redisUtils.verify(() -> RedisUtils.set(
					CacheConstants.DEFAULT_CODE_KEY + "17034642999",
					"123456",
					SecurityConstants.CODE_TIME,
					TimeUnit.SECONDS));
		}
	}

	@Test
	void sendSmsCodeShouldReuseExistingCodeInsteadOfFailingForDemo() {
		SysUserMapper userMapper = mock(SysUserMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		SysMobileServiceImpl service = new SysMobileServiceImpl(userMapper, authAccountService);

		AuthAccount account = new AuthAccount();
		account.setAccountId(100001L);
		account.setClientId("app");
		account.setStatus(CommonConstants.STATUS_NORMAL);
		AuthAccountCredential credential = new AuthAccountCredential();
		credential.setStatus(CommonConstants.STATUS_NORMAL);

		when(authAccountService.resolveAccount("app", null, "17034642999")).thenReturn(Optional.of(account));
		when(authAccountService.getCredential(100001L, "OTP")).thenReturn(Optional.of(credential));

		try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class);
				MockedStatic<RedisUtils> redisUtils = mockStatic(RedisUtils.class)) {
			webUtils.when(WebUtils::findClientId).thenReturn(Optional.of("app"));
			redisUtils.when(() -> RedisUtils.get(CacheConstants.DEFAULT_CODE_KEY + "17034642999")).thenReturn("654321");

			R<SmsCodeSendVO> result = service.sendSmsCode("17034642999");

			assertThat(result.getCode()).isEqualTo(0);
			assertThat(result.getData().getCode()).isEqualTo("654321");
			assertThat(result.getData().getReused()).isTrue();
			assertThat(result.getData().getDelivered()).isFalse();
		}
	}

	@Test
	void sendSmsCodeShouldRejectLockedAccountForClientScopedOtp() {
		SysUserMapper userMapper = mock(SysUserMapper.class);
		AuthAccountService authAccountService = mock(AuthAccountService.class);
		SysMobileServiceImpl service = new SysMobileServiceImpl(userMapper, authAccountService);

		AuthAccount account = new AuthAccount();
		account.setAccountId(100001L);
		account.setClientId("app");
		account.setStatus(CommonConstants.STATUS_LOCK);

		when(authAccountService.resolveAccount("app", null, "17034642999")).thenReturn(Optional.of(account));

		try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class);
				MockedStatic<MsgUtils> msgUtils = mockStatic(MsgUtils.class)) {
			webUtils.when(WebUtils::findClientId).thenReturn(Optional.of("app"));
			msgUtils.when(() -> MsgUtils.getMessage(ErrorCodes.SYS_APP_PHONE_UNREGISTERED, "17034642999"))
				.thenReturn("手机号未注册");

			R<SmsCodeSendVO> result = service.sendSmsCode("17034642999");

			assertThat(result.getCode()).isEqualTo(1);
			assertThat(result.getData()).isNull();
		}
	}
}
