package com.lumencloud.lumen.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumencloud.lumen.admin.api.dto.AuthSessionLogoutDTO;
import com.lumencloud.lumen.admin.api.dto.AuthSessionSaveDTO;
import com.lumencloud.lumen.admin.api.dto.PasskeyAccountLookupDTO;
import com.lumencloud.lumen.admin.api.dto.PasskeyCredentialCounterUpdateDTO;
import com.lumencloud.lumen.admin.api.dto.UserDTO;
import com.lumencloud.lumen.admin.api.dto.UserInfo;
import com.lumencloud.lumen.admin.api.entity.AuthSession;
import com.lumencloud.lumen.admin.api.entity.SysDictItem;
import com.lumencloud.lumen.admin.api.entity.SysLog;
import com.lumencloud.lumen.admin.api.entity.SysOauthClientDetails;
import com.lumencloud.lumen.admin.api.feign.RemoteAuthSessionService;
import com.lumencloud.lumen.admin.api.feign.RemoteClientDetailsService;
import com.lumencloud.lumen.admin.api.feign.RemoteDictService;
import com.lumencloud.lumen.admin.api.feign.RemoteLogService;
import com.lumencloud.lumen.admin.api.feign.RemoteParamService;
import com.lumencloud.lumen.admin.api.feign.RemotePasskeyService;
import com.lumencloud.lumen.admin.api.feign.RemoteTokenService;
import com.lumencloud.lumen.admin.api.feign.RemoteUserService;
import com.lumencloud.lumen.admin.api.vo.PasskeyAccountInfoVO;
import com.lumencloud.lumen.auth.endpoint.LumenTokenEndpoint;
import com.lumencloud.lumen.admin.service.AuthSessionService;
import com.lumencloud.lumen.admin.service.PasskeyService;
import com.lumencloud.lumen.admin.service.SysDictItemService;
import com.lumencloud.lumen.admin.service.SysLogService;
import com.lumencloud.lumen.admin.service.SysOauthClientDetailsService;
import com.lumencloud.lumen.admin.service.SysPublicParamService;
import com.lumencloud.lumen.admin.service.SysUserService;
import com.lumencloud.lumen.common.core.util.R;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

/**
 * Local replacements for internal remote services when running in monolith mode.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "security", name = "micro", havingValue = "false", matchIfMissing = true)
public class MonolithRemoteServiceConfiguration {

	@Bean
	@Primary
	public RemoteUserService remoteUserService(SysUserService sysUserService) {
		return new RemoteUserService() {
			@Override
			public R<UserInfo> info(UserDTO user) {
				return sysUserService.getUserInfo(user);
			}
		};
	}

	@Bean
	@Primary
	public RemoteClientDetailsService remoteClientDetailsService(SysOauthClientDetailsService clientDetailsService) {
		return new RemoteClientDetailsService() {
			@Override
			public R<SysOauthClientDetails> getClientDetailsById(String clientId) {
				SysOauthClientDetails client = clientDetailsService.getOne(
						Wrappers.<SysOauthClientDetails>lambdaQuery().eq(SysOauthClientDetails::getClientId, clientId),
						false);
				return R.ok(client);
			}
		};
	}

	@Bean
	@Primary
	public RemoteAuthSessionService remoteAuthSessionService(AuthSessionService authSessionService) {
		return new RemoteAuthSessionService() {
			@Override
			public R<AuthSession> saveSession(AuthSessionSaveDTO request) {
				return R.ok(authSessionService.saveSession(request));
			}

			@Override
			public R<Boolean> logout(AuthSessionLogoutDTO request) {
				return R.ok(authSessionService.logoutByAccessToken(request.getAccessToken()));
			}

			@Override
			public R<AuthSession> getByAccessToken(AuthSessionLogoutDTO request) {
				return R.ok(authSessionService.getByAccessToken(request.getAccessToken()));
			}

			@Override
			public R<AuthSession> getByRefreshToken(AuthSessionLogoutDTO request) {
				return R.ok(authSessionService.getByRefreshToken(request.getRefreshToken()));
			}
		};
	}

	@Bean
	@Primary
	public RemotePasskeyService remotePasskeyService(PasskeyService passkeyService) {
		return new RemotePasskeyService() {
			@Override
			public R<PasskeyAccountInfoVO> getAccount(PasskeyAccountLookupDTO request) {
				return R.ok(passkeyService.getAccount(request.getClientId(), request.getUsername()));
			}

			@Override
			public R<Boolean> updateSignCount(PasskeyCredentialCounterUpdateDTO request) {
				return R.ok(passkeyService.updateSignCount(request));
			}
		};
	}

	@Bean
	@Primary
	public RemoteDictService remoteDictService(SysDictItemService sysDictItemService) {
		return new RemoteDictService() {
			@Override
			public R<List<SysDictItem>> getDictByType(String type) {
				return R.ok(sysDictItemService
					.list(Wrappers.<SysDictItem>lambdaQuery().eq(SysDictItem::getDictType, type)));
			}
		};
	}

	@Bean
	@Primary
	public RemoteParamService remoteParamService(SysPublicParamService sysPublicParamService) {
		return new RemoteParamService() {
			@Override
			public R<String> getByKey(String key) {
				return R.ok(sysPublicParamService.getParamValue(key));
			}
		};
	}

	@Bean
	@Primary
	public RemoteLogService remoteLogService(SysLogService sysLogService) {
		return new RemoteLogService() {
			@Override
			public R<Boolean> saveLog(SysLog sysLog) {
				return R.ok(sysLogService.saveLog(sysLog));
			}
		};
	}

	@Bean
	@Primary
	public RemoteTokenService remoteTokenService(LumenTokenEndpoint tokenEndpoint,
			OAuth2AuthorizationService authorizationService) {
		return new RemoteTokenService() {
			@Override
			public R<Page> getTokenPage(Map<String, Object> params) {
				return tokenEndpoint.tokenList(params);
			}

			@Override
			public R<Boolean> removeTokenById(String token) {
				return tokenEndpoint.removeToken(token);
			}

			@Override
			public R<Map<String, Object>> queryToken(String token) {
				OAuth2Authorization authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
				if (authorization == null || authorization.getAccessToken() == null) {
					return R.ok(null);
				}
				Map<String, Object> payload = new LinkedHashMap<>(authorization.getAccessToken().getClaims());
				payload.put("client_id", authorization.getRegisteredClientId());
				payload.put("user_name", authorization.getPrincipalName());
				return R.ok(payload);
			}
		};
	}

}
