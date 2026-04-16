package com.lumencloud.lumen.auth.support.filter;

import java.io.IOException;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.exception.ValidateCodeException;
import com.lumencloud.lumen.common.core.util.RedisUtils;
import com.lumencloud.lumen.common.core.util.WebUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates captcha / OTP code before the OAuth2 token endpoint processes the request.
 */
@Component
@RequiredArgsConstructor
public class ValidateCodeFilter extends OncePerRequestFilter {

	private final AuthSecurityConfigProperties authSecurityConfigProperties;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!SecurityConstants.OAUTH_TOKEN_URL.equals(request.getServletPath())) {
			filterChain.doFilter(request, response);
			return;
		}

		// Only inspect real token submissions. Browser GET access should fall through to
		// the authorization server endpoint instead of turning into a 500.
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}

		String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
		if (StrUtil.isBlank(grantType)) {
			filterChain.doFilter(request, response);
			return;
		}

		if (StrUtil.equalsAnyIgnoreCase(grantType, SecurityConstants.REFRESH_TOKEN, SecurityConstants.PASSKEY)) {
			filterChain.doFilter(request, response);
			return;
		}

		String clientId = WebUtils.findClientId(request).orElse(null);
		boolean isIgnoreClient = StrUtil.isNotBlank(clientId)
				&& authSecurityConfigProperties.getIgnoreClients() != null
				&& authSecurityConfigProperties.getIgnoreClients().contains(clientId);
		if (StrUtil.equalsAnyIgnoreCase(grantType, SecurityConstants.PASSWORD, SecurityConstants.CLIENT_CREDENTIALS,
				SecurityConstants.AUTHORIZATION_CODE) && isIgnoreClient) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			checkCode();
			filterChain.doFilter(request, response);
		}
		catch (ValidateCodeException validateCodeException) {
			throw new OAuth2AuthenticationException(validateCodeException.getMessage());
		}
	}

	private void checkCode() throws ValidateCodeException {
		Optional<HttpServletRequest> request = WebUtils.getRequest();
		String code = request.get().getParameter("code");

		if (StrUtil.isBlank(code)) {
			throw new ValidateCodeException("验证码不能为空");
		}

		String randomStr = request.get().getParameter("randomStr");
		String mobile = request.get().getParameter("mobile");
		if (StrUtil.isNotBlank(mobile)) {
			randomStr = mobile;
		}

		String key = CacheConstants.DEFAULT_CODE_KEY + randomStr;
		if (!RedisUtils.hasKey(key)) {
			throw new ValidateCodeException("验证码不合法");
		}

		String saveCode = RedisUtils.get(key);
		if (StrUtil.isBlank(saveCode)) {
			RedisUtils.delete(key);
			throw new ValidateCodeException("验证码不合法");
		}

		if (!StrUtil.equals(saveCode, code)) {
			RedisUtils.delete(key);
			throw new ValidateCodeException("验证码不合法");
		}
	}

}
