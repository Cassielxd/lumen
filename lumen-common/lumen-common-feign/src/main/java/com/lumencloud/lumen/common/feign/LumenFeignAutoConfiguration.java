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

package com.lumencloud.lumen.common.feign;

import org.springframework.cloud.openfeign.LumenFeignClientsRegistrar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.lumencloud.lumen.common.feign.core.LumenFeignInnerRequestInterceptor;
import com.lumencloud.lumen.common.feign.core.LumenFeignRequestCloseInterceptor;

/**
 * Sentinel Feign 自动配置类
 *
 * @author lengleng
 * @date 2025/05/31
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "security", name = "micro", havingValue = "true", matchIfMissing = true)
@Import(LumenFeignClientsRegistrar.class)
public class LumenFeignAutoConfiguration {

	/**
	 * 创建并返回LumenFeignRequestCloseInterceptor实例
	 * @return LumenFeignRequestCloseInterceptor实例
	 */
	@Bean
	public LumenFeignRequestCloseInterceptor lumenFeignRequestCloseInterceptor() {
		return new LumenFeignRequestCloseInterceptor();
	}

	/**
	 * 创建并返回LumenFeignInnerRequestInterceptor实例
	 * @return LumenFeignInnerRequestInterceptor 内部请求拦截器实例
	 */
	@Bean
	public LumenFeignInnerRequestInterceptor lumenFeignInnerRequestInterceptor() {
		return new LumenFeignInnerRequestInterceptor();
	}

}
