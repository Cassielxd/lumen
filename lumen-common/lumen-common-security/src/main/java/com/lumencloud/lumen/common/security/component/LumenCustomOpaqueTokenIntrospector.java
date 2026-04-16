package com.lumencloud.lumen.common.security.component;

import cn.hutool.extra.spring.SpringUtil;
import com.lumencloud.lumen.admin.api.dto.AuthSessionLogoutDTO;
import com.lumencloud.lumen.admin.api.entity.AuthSession;
import com.lumencloud.lumen.admin.api.feign.RemoteAuthSessionService;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.util.RetOps;
import com.lumencloud.lumen.common.security.service.LumenUser;
import com.lumencloud.lumen.common.security.service.LumenUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import java.security.Principal;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Custom opaque token introspector with auth session validation.
 */
@Slf4j
@RequiredArgsConstructor
public class LumenCustomOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

	private final OAuth2AuthorizationService authorizationService;

	@Override
	public OAuth2AuthenticatedPrincipal introspect(String token) {
		OAuth2Authorization oldAuthorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
		if (Objects.isNull(oldAuthorization)) {
			throw new InvalidBearerTokenException(token);
		}

		if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(oldAuthorization.getAuthorizationGrantType())) {
			return new DefaultOAuth2AuthenticatedPrincipal(oldAuthorization.getPrincipalName(),
					Objects.requireNonNull(oldAuthorization.getAccessToken().getClaims()),
					AuthorityUtils.NO_AUTHORITIES);
		}

		AuthSession activeSession = requireActiveSession(token);
		Map<String, LumenUserDetailsService> userDetailsServiceMap = SpringUtil.getBeansOfType(LumenUserDetailsService.class);
		Optional<LumenUserDetailsService> optional = userDetailsServiceMap.values()
			.stream()
			.filter(service -> service.support(oldAuthorization.getRegisteredClientId(),
					oldAuthorization.getAuthorizationGrantType().getValue()))
			.max(Comparator.comparingInt(Ordered::getOrder));

		UserDetails userDetails;
		try {
			Object principal = oldAuthorization.getAttributes().get(Principal.class.getName());
			UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) principal;
			Object tokenPrincipal = usernamePasswordAuthenticationToken.getPrincipal();
			userDetails = optional.orElseThrow(() -> new InvalidBearerTokenException(token))
				.loadUserByUser((LumenUser) tokenPrincipal);
		}
		catch (UsernameNotFoundException notFoundException) {
			log.warn("User not found during token introspection: {}", notFoundException.getLocalizedMessage());
			throw notFoundException;
		}
		catch (InvalidBearerTokenException invalidBearerTokenException) {
			throw invalidBearerTokenException;
		}
		catch (Exception ex) {
			log.error("Resource server token introspection failed: {}", ex.getLocalizedMessage());
			throw new InvalidBearerTokenException(token);
		}

		LumenUser lumenUser = (LumenUser) userDetails;
		Objects.requireNonNull(lumenUser).getAttributes().put(SecurityConstants.CLIENT_ID, oldAuthorization.getRegisteredClientId());
		lumenUser.getAttributes().put(SecurityConstants.SESSION_ID, activeSession.getSid());
		return lumenUser;
	}

	private AuthSession requireActiveSession(String token) {
		AuthSessionLogoutDTO request = new AuthSessionLogoutDTO();
		request.setAccessToken(token);
		AuthSession session = RetOps.of(SpringUtil.getBean(RemoteAuthSessionService.class).getByAccessToken(request))
			.getData()
			.orElseThrow(() -> new InvalidBearerTokenException(token));
		if (!StrStatus.isActive(session)) {
			throw new InvalidBearerTokenException(token);
		}
		return session;
	}

	private static final class StrStatus {

		private static boolean isActive(AuthSession session) {
			return session != null && CommonConstants.STATUS_NORMAL.equals(session.getStatus())
					&& session.getLogoutTime() == null;
		}

		private StrStatus() {
		}

	}

}
