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

package com.lumencloud.lumen.auth.support.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.servlet.RepeatBodyRequestWrapper;
import com.lumencloud.lumen.common.core.util.WebUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Decrypts password parameters before the OAuth2 token endpoint handles the request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordDecoderFilter extends OncePerRequestFilter {

	private static final String PASSWORD = "password";

	private static final String KEY_ALGORITHM = "AES";

	private final AuthSecurityConfigProperties authSecurityConfigProperties;

	static {
		SecureUtil.disableBouncyCastle();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (!StrUtil.containsAnyIgnoreCase(request.getRequestURI(), SecurityConstants.OAUTH_TOKEN_URL)) {
			chain.doFilter(request, response);
			return;
		}
		String clientId = WebUtils.findClientId(request).orElse(null);
		if (StrUtil.isNotBlank(clientId) && authSecurityConfigProperties.getIgnoreClients() != null
				&& authSecurityConfigProperties.getIgnoreClients().contains(clientId)) {
			chain.doFilter(request, response);
			return;
		}

		RepeatBodyRequestWrapper requestWrapper = new RepeatBodyRequestWrapper(request);
		Map<String, String[]> parameterMap = requestWrapper.getParameterMap();
		AES aes = createAes();

		parameterMap.forEach((key, value) -> {
			String[] values = parameterMap.get(key);
			if (!PASSWORD.equals(key) || ArrayUtil.isEmpty(values)) {
				return;
			}
			parameterMap.put(key, new String[] { decodePassword(aes, values[0]) });
		});

		chain.doFilter(requestWrapper, response);
	}

	private AES createAes() {
		byte[] keyBytes = StrUtil.nullToEmpty(authSecurityConfigProperties.getEncodeKey())
			.getBytes(StandardCharsets.UTF_8);
		byte[] normalized = Arrays.copyOf(keyBytes, 16);
		return new AES(Mode.CFB, Padding.NoPadding, new SecretKeySpec(normalized, KEY_ALGORITHM),
				new IvParameterSpec(normalized));
	}

	private String decodePassword(AES aes, String rawPassword) {
		if (StrUtil.isBlank(rawPassword)) {
			return rawPassword;
		}
		try {
			return aes.decryptStr(rawPassword);
		}
		catch (CryptoException | IllegalArgumentException ex) {
			log.debug("Password payload is not decryptable, fallback to raw value");
			return rawPassword;
		}
	}

}
