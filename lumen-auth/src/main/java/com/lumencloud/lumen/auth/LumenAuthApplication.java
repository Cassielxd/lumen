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

package com.lumencloud.lumen.auth;

import com.lumencloud.lumen.common.feign.annotation.EnableLumenFeignClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 认证授权中心应用启动类
 *
 * @author lengleng
 * @date 2025/05/30
 */
@ConditionalOnProperty(prefix = "security", name = "micro", havingValue = "true", matchIfMissing = true)
@EnableLumenFeignClients
@SpringBootApplication
public class LumenAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(LumenAuthApplication.class, args);
	}

}
