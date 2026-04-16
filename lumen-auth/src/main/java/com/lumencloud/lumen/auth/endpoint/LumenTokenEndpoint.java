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

package com.lumencloud.lumen.auth.endpoint;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.TemporalAccessorUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumencloud.lumen.admin.api.dto.AuthSessionLogoutDTO;
import com.lumencloud.lumen.admin.api.entity.SysOauthClientDetails;
import com.lumencloud.lumen.admin.api.feign.RemoteAuthSessionService;
import com.lumencloud.lumen.admin.api.feign.RemoteClientDetailsService;
import com.lumencloud.lumen.admin.api.vo.TokenVo;
import com.lumencloud.lumen.auth.support.handler.LumenAuthenticationFailureEventHandler;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.core.util.RedisUtils;
import com.lumencloud.lumen.common.core.util.RetOps;
import com.lumencloud.lumen.common.core.util.SpringContextHolder;
import com.lumencloud.lumen.common.security.annotation.Inner;
import com.lumencloud.lumen.common.security.util.OAuth2EndpointUtils;
import com.lumencloud.lumen.common.security.util.OAuth2ErrorCodesExpand;
import com.lumencloud.lumen.common.security.util.OAuthClientException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * OAuth2 token endpoint controller.
 *
 * @author lengleng
 * @date 2025/05/30
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(description = "oauth", name = "OAuth2 Token Endpoint")
public class LumenTokenEndpoint {

	private static final String TOKEN_DEMO_CLIENT_ID = "test";

	private static final String TOKEN_DEMO_CLIENT_SECRET = "test";

	private final HttpMessageConverter<OAuth2AccessTokenResponse> accessTokenHttpResponseConverter = new OAuth2AccessTokenResponseHttpMessageConverter();

	private final AuthenticationFailureHandler authenticationFailureHandler = new LumenAuthenticationFailureEventHandler();

	private final OAuth2AuthorizationService authorizationService;

	private final RemoteClientDetailsService clientDetailsService;

	private final RemoteAuthSessionService remoteAuthSessionService;

	private final CacheManager cacheManager;

	@GetMapping("/token/login")
	@Operation(summary = "Authorization login page", description = "Authorization login page")
	public ModelAndView require(ModelAndView modelAndView, @RequestParam(required = false) String error) {
		modelAndView.setViewName("ftl/login");
		modelAndView.addObject("error", error);
		modelAndView.addObject("tokenDemoClientId", TOKEN_DEMO_CLIENT_ID);
		modelAndView.addObject("tokenDemoBasicAuth", buildDemoBasicAuth());
		return modelAndView;
	}

	@GetMapping("/oauth2/confirm_access")
	@Operation(summary = "Authorization confirm page", description = "Authorization confirm page")
	public ModelAndView confirm(Principal principal, ModelAndView modelAndView,
			@RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
			@RequestParam(OAuth2ParameterNames.SCOPE) String scope,
			@RequestParam(OAuth2ParameterNames.STATE) String state) {
		SysOauthClientDetails clientDetails = RetOps.of(clientDetailsService.getClientDetailsById(clientId))
			.getData()
			.orElseThrow(() -> new OAuthClientException("clientId is invalid"));

		Set<String> authorizedScopes = StringUtils.commaDelimitedListToSet(clientDetails.getScope());
		modelAndView.addObject("clientId", clientId);
		modelAndView.addObject("state", state);
		modelAndView.addObject("scopeList", authorizedScopes);
		modelAndView.addObject("principalName", principal.getName());
		modelAndView.setViewName("ftl/confirm");
		return modelAndView;
	}

	@DeleteMapping("/token/logout")
	@Operation(summary = "Logout and remove token", description = "Logout and remove token")
	public R<Boolean> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
		if (StrUtil.isBlank(authHeader)) {
			return R.ok();
		}

		String tokenValue = authHeader.replace(OAuth2AccessToken.TokenType.BEARER.getValue(), StrUtil.EMPTY).trim();
		return removeToken(tokenValue);
	}

	@SneakyThrows
	@GetMapping("/token/check_token")
	@Operation(summary = "Check token", description = "Check token")
	public void checkToken(String token, HttpServletResponse response, HttpServletRequest request) {
		ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);

		if (StrUtil.isBlank(token)) {
			httpResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
			this.authenticationFailureHandler.onAuthenticationFailure(request, response,
					new InvalidBearerTokenException(OAuth2ErrorCodesExpand.TOKEN_MISSING));
			return;
		}
		OAuth2Authorization authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
		if (authorization == null || authorization.getAccessToken() == null) {
			this.authenticationFailureHandler.onAuthenticationFailure(request, response,
					new InvalidBearerTokenException(OAuth2ErrorCodesExpand.INVALID_BEARER_TOKEN));
			return;
		}
		if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(authorization.getAuthorizationGrantType())
				&& !isActiveSession(token)) {
			this.authenticationFailureHandler.onAuthenticationFailure(request, response,
					new InvalidBearerTokenException(OAuth2ErrorCodesExpand.INVALID_BEARER_TOKEN));
			return;
		}

		Map<String, Object> claims = authorization.getAccessToken().getClaims();
		OAuth2AccessTokenResponse sendAccessTokenResponse = OAuth2EndpointUtils.sendAccessTokenResponse(authorization,
				claims);
		this.accessTokenHttpResponseConverter.write(sendAccessTokenResponse, MediaType.APPLICATION_JSON, httpResponse);
	}

	@Inner
	@DeleteMapping("/token/remove/{token}")
	@Operation(summary = "Remove token", description = "Remove token")
	public R<Boolean> removeToken(@PathVariable("token") String token) {
		OAuth2Authorization authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
		if (authorization == null) {
			return R.ok();
		}

		OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
		if (accessToken == null || StrUtil.isBlank(accessToken.getToken().getTokenValue())) {
			return R.ok();
		}

		evictUserDetailsCache(authorization);
		AuthSessionLogoutDTO logoutRequest = new AuthSessionLogoutDTO();
		logoutRequest.setAccessToken(token);
		remoteAuthSessionService.logout(logoutRequest);
		authorizationService.remove(authorization);
		SpringContextHolder.publishEvent(new LogoutSuccessEvent(new PreAuthenticatedAuthenticationToken(
				authorization.getPrincipalName(), authorization.getRegisteredClientId())));
		return R.ok();
	}

	@Inner
	@PostMapping("/token/page")
	@Operation(summary = "Token page", description = "Token page")
	public R<Page> tokenList(@RequestBody Map<String, Object> params) {
		String username = MapUtil.getStr(params, SecurityConstants.USERNAME);
		String pattern = String.format("%s::*", CacheConstants.PROJECT_OAUTH_ACCESS);
		int current = MapUtil.getInt(params, CommonConstants.CURRENT);
		int size = MapUtil.getInt(params, CommonConstants.SIZE);
		Page result = new Page(current, size);

		List<String> allKeys = RedisUtils.scan(pattern);
		result.setTotal(allKeys.size());

		List<String> pageKeys = RedisUtils.findKeysForPage(pattern, current - 1, size);
		List<OAuth2Authorization> pagedAuthorizations = RedisUtils.multiGet(pageKeys);

		List<TokenVo> tokenVoList = pagedAuthorizations.stream()
			.filter(Objects::nonNull)
			.map(this::convertToTokenVo)
			.filter(tokenVo -> {
				if (StrUtil.isBlank(username)) {
					return true;
				}
				return StrUtil.startWithAnyIgnoreCase(tokenVo.getUsername(), username);
			})
			.toList();

		if (StrUtil.isNotBlank(username)) {
			result.setTotal(tokenVoList.size());
		}

		result.setRecords(tokenVoList);
		return R.ok(result);
	}

	private TokenVo convertToTokenVo(OAuth2Authorization authorization) {
		TokenVo tokenVo = new TokenVo();
		tokenVo.setClientId(authorization.getRegisteredClientId());
		tokenVo.setId(authorization.getId());
		tokenVo.setUsername(authorization.getPrincipalName());
		OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
		tokenVo.setAccessToken(accessToken.getToken().getTokenValue());

		String expiresAt = TemporalAccessorUtil.format(accessToken.getToken().getExpiresAt(),
				DatePattern.NORM_DATETIME_PATTERN);
		tokenVo.setExpiresAt(expiresAt);

		String issuedAt = TemporalAccessorUtil.format(accessToken.getToken().getIssuedAt(),
				DatePattern.NORM_DATETIME_PATTERN);
		tokenVo.setIssuedAt(issuedAt);
		return tokenVo;
	}

	private void evictUserDetailsCache(OAuth2Authorization authorization) {
		Cache userDetailsCache = cacheManager.getCache(CacheConstants.USER_DETAILS);
		if (userDetailsCache == null) {
			return;
		}
		userDetailsCache.evictIfPresent(authorization.getPrincipalName());
		userDetailsCache
			.evictIfPresent(authorization.getRegisteredClientId() + "::" + authorization.getPrincipalName());
		if (authorization.getAuthorizationGrantType() != null) {
			userDetailsCache.evictIfPresent(authorization.getRegisteredClientId() + "::"
					+ authorization.getAuthorizationGrantType().getValue() + "::" + authorization.getPrincipalName());
		}
	}

	private boolean isActiveSession(String accessToken) {
		AuthSessionLogoutDTO request = new AuthSessionLogoutDTO();
		request.setAccessToken(accessToken);
		return RetOps.of(remoteAuthSessionService.getByAccessToken(request))
			.getData()
			.map(session -> CommonConstants.STATUS_NORMAL.equals(session.getStatus()) && session.getLogoutTime() == null)
			.orElse(false);
	}

	private String buildDemoBasicAuth() {
		String credential = TOKEN_DEMO_CLIENT_ID + ":" + TOKEN_DEMO_CLIENT_SECRET;
		return "Basic " + Base64.getEncoder().encodeToString(credential.getBytes(StandardCharsets.UTF_8));
	}

}
