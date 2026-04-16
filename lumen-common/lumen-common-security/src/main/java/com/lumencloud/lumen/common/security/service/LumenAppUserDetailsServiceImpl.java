/*
 * Copyright (c) 2020 lumencloud Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lumencloud.lumen.common.security.service;

import com.lumencloud.lumen.common.core.util.WebUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import com.lumencloud.lumen.admin.api.dto.UserDTO;
import com.lumencloud.lumen.admin.api.dto.UserInfo;
import com.lumencloud.lumen.admin.api.feign.RemoteUserService;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.util.R;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * 用户详细信息服务实现类，提供基于手机号的用户信息加载功能
 *
 * @author lengleng hccake
 * @date 2025/05/31
 */
@RequiredArgsConstructor
public class LumenAppUserDetailsServiceImpl implements LumenUserDetailsService {

	private final RemoteUserService remoteUserService;

	private final CacheManager cacheManager;

	/**
	 * 根据手机号加载用户信息
	 * @param phone 用户手机号
	 * @return 用户详细信息
	 * @throws Exception 获取用户信息过程中可能抛出的异常
	 */
	@Override
	@SneakyThrows
	public UserDetails loadUserByUsername(String phone) {
		String clientId = WebUtils.findClientId().orElse(null);
		String grantType = resolveGrantType();
		String cacheKey = buildCacheKey(clientId, phone, grantType);
		Cache cache = cacheManager.getCache(CacheConstants.USER_DETAILS);
		if (cache != null && cache.get(cacheKey) != null) {
			return (LumenUser) cache.get(cacheKey).get();
		}

		UserDTO userDTO = new UserDTO();
		userDTO.setPhone(phone);
		userDTO.setClientId(clientId);
		userDTO.setGrantType(grantType);
		R<UserInfo> result = remoteUserService.info(userDTO);

		UserDetails userDetails = getUserDetails(result);
		if (cache != null) {
			cache.put(cacheKey, userDetails);
		}
		return userDetails;
	}

	/**
	 * 根据用户信息加载用户详情
	 * @param lumenUser 用户信息对象
	 * @return 用户详情
	 */
	@Override
	public UserDetails loadUserByUser(LumenUser lumenUser) {
		String clientId = lumenUser.getAccountClientId();
		String grantType = resolveGrantType();
		String cacheKey = buildCacheKey(clientId, lumenUser.getPhone(), grantType);
		Cache cache = cacheManager.getCache(CacheConstants.USER_DETAILS);
		if (cache != null && cache.get(cacheKey) != null) {
			return (LumenUser) cache.get(cacheKey).get();
		}
		UserDTO userDTO = new UserDTO();
		userDTO.setPhone(lumenUser.getPhone());
		userDTO.setClientId(clientId);
		userDTO.setGrantType(grantType);
		R<UserInfo> result = remoteUserService.info(userDTO);
		UserDetails userDetails = getUserDetails(result);
		if (cache != null) {
			cache.put(cacheKey, userDetails);
		}
		return userDetails;
	}

	/**
	 * 是否支持此客户端校验
	 * @param clientId 目标客户端
	 * @return true/false
	 */
	@Override
	public boolean support(String clientId, String grantType) {
		return SecurityConstants.MOBILE.equals(grantType) || SecurityConstants.OTP.equals(grantType);
	}

	private String buildCacheKey(String clientId, String phone, String grantType) {
		if (clientId == null) {
			return phone;
		}
		return grantType == null ? clientId + "::" + phone : clientId + "::" + grantType + "::" + phone;
	}

	private String resolveGrantType() {
		return WebUtils.getRequest().map(request -> request.getParameter(OAuth2ParameterNames.GRANT_TYPE)).orElse(null);
	}

}
