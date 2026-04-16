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

package com.lumencloud.lumen.auth.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.lumencloud.lumen.auth.support.CustomeOAuth2AccessTokenGenerator;
import com.lumencloud.lumen.auth.support.core.CustomeOAuth2TokenCustomizer;
import com.lumencloud.lumen.auth.support.core.FormIdentityLoginConfigurer;
import com.lumencloud.lumen.auth.support.core.LumenDaoAuthenticationProvider;
import com.lumencloud.lumen.auth.support.core.LumenRefreshTokenAuthenticationProvider;
import com.lumencloud.lumen.auth.support.core.OAuth2LoginGrantHandler;
import com.lumencloud.lumen.auth.support.filter.PasswordDecoderFilter;
import com.lumencloud.lumen.auth.support.filter.ValidateCodeFilter;
import com.lumencloud.lumen.auth.support.handler.LumenAuthenticationFailureEventHandler;
import com.lumencloud.lumen.auth.support.handler.LumenAuthenticationSuccessEventHandler;
import com.lumencloud.lumen.admin.api.feign.RemoteAuthSessionService;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.security.component.LumenBootCorsProperties;

import lombok.RequiredArgsConstructor;

/**
 * Authorization server security configuration.
 */
@Configuration
@RequiredArgsConstructor
public class AuthorizationServerConfiguration {

	private final OAuth2AuthorizationService authorizationService;

	private final PasswordDecoderFilter passwordDecoderFilter;

	private final ValidateCodeFilter validateCodeFilter;

	private final LumenBootCorsProperties lumenBootCorsProperties;

	private final List<OAuth2LoginGrantHandler> loginGrantHandlers;

	private final RemoteAuthSessionService remoteAuthSessionService;

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public SecurityFilterChain authorizationServer(HttpSecurity http) throws Exception {
		http.securityMatcher("/oauth2/**");
		OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();

		http.addFilterBefore(validateCodeFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(passwordDecoderFilter, UsernamePasswordAuthenticationFilter.class);

		http.with(authorizationServerConfigurer.tokenEndpoint(tokenEndpoint -> tokenEndpoint
			.accessTokenRequestConverter(accessTokenRequestConverter())
			.accessTokenResponseHandler(new LumenAuthenticationSuccessEventHandler())
			.errorResponseHandler(new LumenAuthenticationFailureEventHandler()))
			.clientAuthentication(oAuth2ClientAuthenticationConfigurer -> oAuth2ClientAuthenticationConfigurer
				.errorResponseHandler(new LumenAuthenticationFailureEventHandler()))
			.authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint
				.consentPage(SecurityConstants.CUSTOM_CONSENT_PAGE_URI)), Customizer.withDefaults())
			.authorizeHttpRequests(authorizeRequests -> authorizeRequests.anyRequest().authenticated());

		http.with(authorizationServerConfigurer.authorizationService(authorizationService)
			.authorizationServerSettings(
					AuthorizationServerSettings.builder().issuer(SecurityConstants.PROJECT_LICENSE).build()),
				Customizer.withDefaults());

		http.with(new FormIdentityLoginConfigurer(), Customizer.withDefaults());

		if (Boolean.TRUE.equals(lumenBootCorsProperties.getEnabled())) {
			http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
		}

		addCustomOAuth2GrantAuthenticationProvider(http);
		return http.build();
	}

	@Bean
	public OAuth2TokenGenerator oAuth2TokenGenerator() {
		CustomeOAuth2AccessTokenGenerator accessTokenGenerator = new CustomeOAuth2AccessTokenGenerator();
		accessTokenGenerator.setAccessTokenCustomizer(new CustomeOAuth2TokenCustomizer());
		return new DelegatingOAuth2TokenGenerator(accessTokenGenerator, new OAuth2RefreshTokenGenerator());
	}

	@Bean
	public AuthenticationConverter accessTokenRequestConverter() {
		List<AuthenticationConverter> converters = new ArrayList<>();
		orderedLoginGrantHandlers().stream()
			.map(OAuth2LoginGrantHandler::getAuthenticationConverter)
			.forEach(converters::add);
		converters.add(new OAuth2RefreshTokenAuthenticationConverter());
		converters.add(new OAuth2ClientCredentialsAuthenticationConverter());
		converters.add(new OAuth2AuthorizationCodeAuthenticationConverter());
		converters.add(new OAuth2AuthorizationCodeRequestAuthenticationConverter());
		return new DelegatingAuthenticationConverter(converters);
	}

	private void addCustomOAuth2GrantAuthenticationProvider(HttpSecurity http) {
		OAuth2AuthorizationService sharedAuthorizationService = http.getSharedObject(OAuth2AuthorizationService.class);
		LumenDaoAuthenticationProvider daoAuthenticationProvider = new LumenDaoAuthenticationProvider();
		AuthenticationManager authenticationManager = new ProviderManager(daoAuthenticationProvider);

		http.authenticationProvider(daoAuthenticationProvider);
		http.authenticationProvider(new LumenRefreshTokenAuthenticationProvider(
				new OAuth2RefreshTokenAuthenticationProvider(sharedAuthorizationService, oAuth2TokenGenerator()),
				remoteAuthSessionService));
		orderedLoginGrantHandlers().stream()
			.map(handler -> handler.getAuthenticationProvider(authenticationManager, sharedAuthorizationService,
					oAuth2TokenGenerator()))
			.forEach(http::authenticationProvider);
	}

	private List<OAuth2LoginGrantHandler> orderedLoginGrantHandlers() {
		List<OAuth2LoginGrantHandler> handlers = new ArrayList<>(loginGrantHandlers);
		AnnotationAwareOrderComparator.sort(handlers);
		return handlers;
	}

	private UrlBasedCorsConfigurationSource corsConfigurationSource() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		lumenBootCorsProperties.getAllowedOriginPatterns().forEach(corsConfiguration::addAllowedOriginPattern);
		lumenBootCorsProperties.getAllowedHeaders().forEach(corsConfiguration::addAllowedHeader);
		lumenBootCorsProperties.getAllowedMethods().forEach(corsConfiguration::addAllowedMethod);
		corsConfiguration.setAllowCredentials(lumenBootCorsProperties.getAllowCredentials());
		source.registerCorsConfiguration(lumenBootCorsProperties.getPathPattern(), corsConfiguration);
		return source;
	}

}
