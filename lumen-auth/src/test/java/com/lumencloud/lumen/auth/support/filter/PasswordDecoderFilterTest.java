package com.lumencloud.lumen.auth.support.filter;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.AES;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordDecoderFilterTest {

	@Test
	void keepsPlaintextPasswordWhenPayloadIsNotEncrypted() throws ServletException, IOException {
		PasswordDecoderFilter filter = new PasswordDecoderFilter(properties("thanks,lumencloud"));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
		request.addParameter("grant_type", "password");
		request.addParameter("client_id", "test");
		request.addParameter("password", "123456");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest().getParameter("password")).isEqualTo("123456");
	}

	@Test
	void decryptsCiphertextPasswordWhenPayloadIsEncrypted() throws ServletException, IOException {
		String encodeKey = "thanks,lumencloud";
		PasswordDecoderFilter filter = new PasswordDecoderFilter(properties(encodeKey));
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
		request.addParameter("grant_type", "password");
		request.addParameter("client_id", "lumen");
		request.addParameter("password", encrypt(encodeKey, "123456"));
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest().getParameter("password")).isEqualTo("123456");
	}

	private static String encrypt(String encodeKey, String rawPassword) {
		byte[] keyBytes = Arrays.copyOf(encodeKey.getBytes(StandardCharsets.UTF_8), 16);
		AES aes = new AES(Mode.CFB, Padding.NoPadding, new SecretKeySpec(keyBytes, "AES"),
				new IvParameterSpec(keyBytes));
		return aes.encryptHex(rawPassword);
	}

	private static AuthSecurityConfigProperties properties(String encodeKey) {
		AuthSecurityConfigProperties properties = new AuthSecurityConfigProperties();
		properties.setEncodeKey(encodeKey);
		properties.setIgnoreClients(java.util.List.of("test"));
		return properties;
	}

}
