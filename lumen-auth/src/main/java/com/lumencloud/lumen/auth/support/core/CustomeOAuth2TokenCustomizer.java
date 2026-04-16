package com.lumencloud.lumen.auth.support.core;

import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.security.service.LumenUser;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsSet;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * OAuth2 Token 自定义增强实现类
 *
 * @author lengleng
 * @date 2025/05/30
 */
public class CustomeOAuth2TokenCustomizer implements OAuth2TokenCustomizer<OAuth2TokenClaimsContext> {

	/**
	 * 自定义OAuth 2.0 Token属性
	 * @param context 包含OAuth 2.0 Token属性的上下文
	 */
	@Override
	public void customize(OAuth2TokenClaimsContext context) {
		OAuth2TokenClaimsSet.Builder claims = context.getClaims();
		claims.claim(SecurityConstants.DETAILS_LICENSE, SecurityConstants.PROJECT_LICENSE);
		String clientId = context.getRegisteredClient().getClientId();
		claims.claim(SecurityConstants.CLIENT_ID, clientId);
		// 客户端模式不返回具体用户信息
		if (SecurityConstants.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType().getValue())) {
			return;
		}

		LumenUser lumenUser = (LumenUser) context.getPrincipal().getPrincipal();
		claims.claim(SecurityConstants.DETAILS_USER, lumenUser);
		claims.claim(SecurityConstants.DETAILS_USER_ID, lumenUser.getId());
		claims.claim(SecurityConstants.DETAILS_ACCOUNT_ID, lumenUser.getAccountId());
		claims.claim(SecurityConstants.DETAILS_ACCOUNT_CLIENT_ID, lumenUser.getAccountClientId());
		claims.claim(SecurityConstants.USERNAME, lumenUser.getUsername());
	}

}
