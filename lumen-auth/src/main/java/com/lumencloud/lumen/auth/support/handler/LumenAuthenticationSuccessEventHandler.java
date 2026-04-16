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

package com.lumencloud.lumen.auth.support.handler;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.lumencloud.lumen.admin.api.dto.AuthSessionSaveDTO;
import com.lumencloud.lumen.admin.api.entity.AuthSession;
import com.lumencloud.lumen.admin.api.feign.RemoteAuthSessionService;
import com.lumencloud.lumen.admin.api.entity.SysLog;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.util.RetOps;
import com.lumencloud.lumen.common.core.util.SpringContextHolder;
import com.lumencloud.lumen.common.log.event.SysLogEvent;
import com.lumencloud.lumen.common.log.util.SysLogUtils;
import com.lumencloud.lumen.common.security.component.LumenCustomOAuth2AccessTokenResponseHttpMessageConverter;
import com.lumencloud.lumen.common.security.service.LumenUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles OAuth2 authentication success events.
 *
 * @author lengleng
 * @date 2025/05/30
 */
@Slf4j
public class LumenAuthenticationSuccessEventHandler implements AuthenticationSuccessHandler {

	private final HttpMessageConverter<OAuth2AccessTokenResponse> accessTokenHttpResponseConverter = new LumenCustomOAuth2AccessTokenResponseHttpMessageConverter();

	@SneakyThrows
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) {
		OAuth2AccessTokenAuthenticationToken accessTokenAuthentication = (OAuth2AccessTokenAuthenticationToken) authentication;
		Map<String, Object> additionalParameters = accessTokenAuthentication.getAdditionalParameters();
		Map<String, Object> responseParameters = new LinkedHashMap<>();
		if (!CollectionUtils.isEmpty(additionalParameters)) {
			responseParameters.putAll(additionalParameters);
		}
		String sessionId = recordSession(request, accessTokenAuthentication, additionalParameters);
		if (StrUtil.isNotBlank(sessionId)) {
			responseParameters.put(SecurityConstants.SESSION_ID, sessionId);
		}
		recordLoginLog(request, accessTokenAuthentication, additionalParameters);
		sendAccessTokenResponse(response, accessTokenAuthentication, responseParameters);
	}

	private void recordLoginLog(HttpServletRequest request,
			OAuth2AccessTokenAuthenticationToken accessTokenAuthentication, Map<String, Object> additionalParameters) {
		if (MapUtil.isEmpty(additionalParameters)) {
			return;
		}

		Object userObj = additionalParameters.get(SecurityConstants.DETAILS_USER);
		if (!(userObj instanceof LumenUser userInfo)) {
			return;
		}

		log.info("User {} login succeeded", userInfo.getName());
		SecurityContextHolder.getContext().setAuthentication(accessTokenAuthentication);
		SysLog logVo = SysLogUtils.getSysLog();
		logVo.setTitle("Login Success");
		String startTimeStr = request.getHeader(CommonConstants.REQUEST_START_TIME);
		if (StrUtil.isNotBlank(startTimeStr)) {
			Long startTime = Long.parseLong(startTimeStr);
			Long endTime = System.currentTimeMillis();
			logVo.setTime(endTime - startTime);
		}
		logVo.setCreateBy(userInfo.getName());
		SpringContextHolder.publishEvent(new SysLogEvent(logVo));
	}

	private void sendAccessTokenResponse(HttpServletResponse response,
			OAuth2AccessTokenAuthenticationToken accessTokenAuthentication, Map<String, Object> responseParameters)
			throws IOException {
		OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();
		OAuth2RefreshToken refreshToken = accessTokenAuthentication.getRefreshToken();

		OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse.withToken(accessToken.getTokenValue())
			.tokenType(accessToken.getTokenType())
			.scopes(accessToken.getScopes());
		if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
			builder.expiresIn(ChronoUnit.SECONDS.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()));
		}
		if (refreshToken != null) {
			builder.refreshToken(refreshToken.getTokenValue());
		}
		if (!CollectionUtils.isEmpty(responseParameters)) {
			builder.additionalParameters(responseParameters);
		}

		SecurityContextHolder.clearContext();
		OAuth2AccessTokenResponse accessTokenResponse = builder.build();
		this.accessTokenHttpResponseConverter.write(accessTokenResponse, null, new ServletServerHttpResponse(response));
	}

	private String recordSession(HttpServletRequest request, OAuth2AccessTokenAuthenticationToken accessTokenAuthentication,
			Map<String, Object> additionalParameters) {
		if (MapUtil.isEmpty(additionalParameters)) {
			return null;
		}
		Object userObj = additionalParameters.get(SecurityConstants.DETAILS_USER);
		if (!(userObj instanceof LumenUser userInfo) || userInfo.getAccountId() == null) {
			return null;
		}

		AuthSessionSaveDTO session = new AuthSessionSaveDTO();
		session.setAccountId(userInfo.getAccountId());
		session.setUserId(userInfo.getId());
		session.setClientId(accessTokenAuthentication.getRegisteredClient().getClientId());
		session.setPrincipalName(userInfo.getUsername());
		session.setGrantType(request.getParameter("grant_type"));
		session.setAccessToken(accessTokenAuthentication.getAccessToken().getTokenValue());
		if (accessTokenAuthentication.getAccessToken().getExpiresAt() != null) {
			session.setAccessTokenExpiresAt(LocalDateTime.ofInstant(
					accessTokenAuthentication.getAccessToken().getExpiresAt(), ZoneId.systemDefault()));
		}
		if (accessTokenAuthentication.getRefreshToken() != null) {
			session.setRefreshToken(accessTokenAuthentication.getRefreshToken().getTokenValue());
			if (accessTokenAuthentication.getRefreshToken().getExpiresAt() != null) {
				session.setRefreshTokenExpiresAt(LocalDateTime.ofInstant(
						accessTokenAuthentication.getRefreshToken().getExpiresAt(), ZoneId.systemDefault()));
			}
		}
		session.setIpAddress(resolveIp(request));
		session.setUserAgent(request.getHeader("User-Agent"));
		return RetOps.of(SpringContextHolder.getBean(RemoteAuthSessionService.class).saveSession(session))
			.getData()
			.map(AuthSession::getSid)
			.orElse(null);
	}

	private String resolveIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (StrUtil.isNotBlank(forwardedFor)) {
			return StrUtil.splitTrim(forwardedFor, ',').get(0);
		}
		String realIp = request.getHeader("X-Real-IP");
		if (StrUtil.isNotBlank(realIp)) {
			return realIp;
		}
		return request.getRemoteAddr();
	}

}
