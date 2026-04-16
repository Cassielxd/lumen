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

package com.lumencloud.lumen.admin.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumencloud.lumen.admin.api.dto.SysLogDTO;
import com.lumencloud.lumen.admin.api.entity.SysLog;
import com.lumencloud.lumen.admin.service.SysLogService;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.security.annotation.HasPermission;
import com.lumencloud.lumen.common.security.annotation.Inner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 绯荤粺鏃ュ織鍓嶇鎺у埗鍣?
 *
 * @author lengleng
 * @since 2017-11-20
 */
@RestController
@AllArgsConstructor
@RequestMapping("/log")
@Tag(description = "log", name = "鏃ュ織绠＄悊妯″潡")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SysLogController {

	private final SysLogService sysLogService;

	/**
	 * 鍒嗛〉鏌ヨ绯荤粺鏃ュ織
	 * @param page 鍒嗛〉鍙傛暟瀵硅薄
	 * @param sysLog 绯荤粺鏃ュ織鏌ヨ鏉′欢
	 * @return 鍖呭惈鍒嗛〉缁撴灉鐨勫搷搴斿璞?
	 */

}
