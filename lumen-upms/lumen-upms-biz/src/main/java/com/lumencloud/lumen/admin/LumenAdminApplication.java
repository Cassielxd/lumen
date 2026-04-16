/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the lumencloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.lumencloud.lumen.admin;

import com.lumencloud.lumen.common.feign.annotation.EnableLumenFeignClients;
import com.lumencloud.lumen.common.security.annotation.EnableLumenResourceServer;
import com.lumencloud.lumen.common.swagger.annotation.EnableLumenDoc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 用户统一管理系统
 *
 * @author lengleng
 * @date 2025/05/30
 */
@ConditionalOnProperty(prefix = "security", name = "micro", havingValue = "true", matchIfMissing = true)
@EnableLumenDoc(value = "admin")
@EnableLumenFeignClients
@EnableLumenResourceServer
@SpringBootApplication
public class LumenAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(LumenAdminApplication.class, args);
	}

}
