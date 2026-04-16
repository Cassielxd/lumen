package com.lumencloud.lumen.common.security.service;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.lumencloud.lumen.admin.api.entity.SysOauthClientDetails;
import com.lumencloud.lumen.admin.api.feign.RemoteClientDetailsService;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.util.RetOps;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import static com.lumencloud.lumen.common.core.constant.CacheConstants.CLIENT_DETAILS_KEY;

/**
 * Reads client configuration from UPMS and adapts it to Spring Authorization Server.
 *
 * In monolith mode the repository bypasses the cache so direct database changes
 * are reflected immediately. Microservice mode keeps the original cached behavior.
 */
@RequiredArgsConstructor
public class LumenRemoteRegisteredClientRepository implements RegisteredClientRepository {

	private static final int REFRESH_TOKEN_VALIDITY_SECONDS = 60 * 60 * 24 * 30;

	private static final int ACCESS_TOKEN_VALIDITY_SECONDS = 60 * 60 * 12;

	private final RemoteClientDetailsService clientDetailsService;

	private final CacheManager cacheManager;

	private boolean micro;

	@Value("${security.micro:false}")
	public void setMicro(boolean micro) {
		this.micro = micro;
	}

	@Override
	public void save(RegisteredClient registeredClient) {
	}

	@Override
	public RegisteredClient findById(String id) {
		throw new UnsupportedOperationException();
	}

	@Override
	@SneakyThrows
	public RegisteredClient findByClientId(String clientId) {
		if (!micro) {
			return loadRegisteredClient(clientId);
		}

		Cache cache = cacheManager.getCache(CLIENT_DETAILS_KEY);
		if (cache == null) {
			return loadRegisteredClient(clientId);
		}

		RegisteredClient cached = cache.get(clientId, RegisteredClient.class);
		if (cached != null) {
			return cached;
		}

		RegisteredClient loaded = loadRegisteredClient(clientId);
		cache.put(clientId, loaded);
		return loaded;
	}

	private RegisteredClient loadRegisteredClient(String clientId) {
		SysOauthClientDetails clientDetails = RetOps.of(clientDetailsService.getClientDetailsById(clientId))
			.getData()
			.orElseThrow(() -> new OAuth2AuthorizationCodeRequestAuthenticationException(
					new OAuth2Error("客户端查询异常，请检查数据源配置"), null));

		RegisteredClient.Builder builder = RegisteredClient.withId(clientDetails.getClientId())
			.clientId(clientDetails.getClientId())
			.clientSecret(SecurityConstants.NOOP + clientDetails.getClientSecret())
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

		for (String authorizedGrantType : clientDetails.getAuthorizedGrantTypes()) {
			builder.authorizationGrantType(new AuthorizationGrantType(authorizedGrantType));
		}

		Optional.ofNullable(clientDetails.getWebServerRedirectUri())
			.ifPresent(redirectUri -> Arrays.stream(redirectUri.split(StrUtil.COMMA))
				.filter(StrUtil::isNotBlank)
				.forEach(builder::redirectUri));

		Optional.ofNullable(clientDetails.getScope())
			.ifPresent(scope -> Arrays.stream(scope.split(StrUtil.COMMA))
				.filter(StrUtil::isNotBlank)
				.forEach(builder::scope));

		return builder
			.tokenSettings(TokenSettings.builder()
				.accessTokenFormat(OAuth2TokenFormat.REFERENCE)
				.accessTokenTimeToLive(Duration.ofSeconds(
						Optional.ofNullable(clientDetails.getAccessTokenValidity())
							.orElse(ACCESS_TOKEN_VALIDITY_SECONDS)))
				.refreshTokenTimeToLive(Duration.ofSeconds(
						Optional.ofNullable(clientDetails.getRefreshTokenValidity())
							.orElse(REFRESH_TOKEN_VALIDITY_SECONDS)))
				.build())
			.clientSettings(ClientSettings.builder()
				.requireAuthorizationConsent(!BooleanUtil.toBoolean(clientDetails.getAutoapprove()))
				.build())
			.build();
	}

}
