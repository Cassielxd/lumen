/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the lumencloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.lumencloud.lumen.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.entity.SysUser;
import com.lumencloud.lumen.admin.api.vo.SmsCodeSendVO;
import com.lumencloud.lumen.admin.mapper.SysUserMapper;
import com.lumencloud.lumen.admin.service.AuthAccountService;
import com.lumencloud.lumen.admin.service.SysMobileService;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.exception.ErrorCodes;
import com.lumencloud.lumen.common.core.util.MsgUtils;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.core.util.RedisUtils;
import com.lumencloud.lumen.common.core.util.WebUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * SMS login support service.
 */
@Slf4j
@Service
@AllArgsConstructor
public class SysMobileServiceImpl implements SysMobileService {

	private static final String OTP_CREDENTIAL_TYPE = "OTP";

	private final SysUserMapper userMapper;

	private final AuthAccountService authAccountService;

	@Override
	public R<SmsCodeSendVO> sendSmsCode(String mobile) {
		String clientId = resolveClientId();
		if (StrUtil.isNotBlank(clientId)) {
			Optional<AuthAccount> accountOptional = authAccountService.resolveAccount(clientId, null, mobile);
			if (accountOptional.isEmpty() || !StrUtil.equals(CommonConstants.STATUS_NORMAL, accountOptional.get().getStatus())
					|| !hasAvailableOtpCredential(accountOptional.get())) {
				log.info("Phone is not registered for client {}: {}", clientId, mobile);
				return R.failed(MsgUtils.getMessage(ErrorCodes.SYS_APP_PHONE_UNREGISTERED, mobile));
			}
		}
		else if (!existsLegacyUser(mobile)) {
			log.info("Phone is not registered: {}", mobile);
			return R.failed(MsgUtils.getMessage(ErrorCodes.SYS_APP_PHONE_UNREGISTERED, mobile));
		}

		return issueSmsCode(mobile);
	}

	private SmsCodeSendVO buildResult(String mobile, String code, boolean reused, boolean delivered) {
		return new SmsCodeSendVO().setMobile(mobile)
			.setCode(code)
			.setReused(reused)
			.setDelivered(delivered)
			.setExpiresInSeconds(SecurityConstants.CODE_TIME);
	}

	private boolean existsLegacyUser(String mobile) {
		List<SysUser> userList = userMapper.selectList(Wrappers.<SysUser>query().lambda().eq(SysUser::getPhone, mobile));
		return CollUtil.isNotEmpty(userList);
	}

	private boolean hasAvailableOtpCredential(AuthAccount account) {
		AuthAccountCredential otpCredential = authAccountService.getCredential(account.getAccountId(), OTP_CREDENTIAL_TYPE)
			.orElse(null);
		return otpCredential != null && StrUtil.equals(CommonConstants.STATUS_NORMAL, otpCredential.getStatus());
	}

	private R<SmsCodeSendVO> issueSmsCode(String mobile) {
		String cacheKey = CacheConstants.DEFAULT_CODE_KEY + mobile;
		String cachedCode = RedisUtils.get(cacheKey);
		if (cachedCode != null) {
			log.info("SMS code is still valid for {}: {}", mobile, cachedCode);
			return R.ok(buildResult(mobile, cachedCode, true, false), "Verification code is still valid and was reused");
		}

		String code = RandomUtil.randomNumbers(Integer.parseInt(SecurityConstants.CODE_SIZE));
		log.info("Generated SMS code for {}: {}", mobile, code);
		RedisUtils.set(cacheKey, code, SecurityConstants.CODE_TIME, TimeUnit.SECONDS);

		SmsBlend smsBlend = SmsFactory.getSmsBlend();
		if (Objects.isNull(smsBlend)) {
			log.info("SMS provider is unavailable, returning OTP code directly for demo: {}", mobile);
			return R.ok(buildResult(mobile, code, false, false), "Verification code was generated and returned for demo");
		}

		try {
			SmsResponse smsResponse = smsBlend.sendMessage(mobile, new LinkedHashMap<>(Map.of("code", code)));
			log.debug("SMS provider response: {}", smsResponse);
			return R.ok(buildResult(mobile, code, false, true), "Verification code was sent and returned to the demo");
		}
		catch (Exception exception) {
			log.warn("SMS provider send failed, returning OTP code directly for demo: {}", mobile, exception);
			return R.ok(buildResult(mobile, code, false, false),
					"SMS channel is unavailable, verification code returned for demo");
		}
	}

	private String resolveClientId() {
		return WebUtils.findClientId().orElse(null);
	}

}
