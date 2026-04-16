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

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumencloud.lumen.admin.api.entity.SysDict;
import com.lumencloud.lumen.admin.api.entity.SysDictItem;
import com.lumencloud.lumen.admin.service.SysDictItemService;
import com.lumencloud.lumen.admin.service.SysDictService;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.log.annotation.SysLog;
import com.lumencloud.lumen.common.security.annotation.Inner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 瀛楀吀琛ㄥ墠绔帶鍒跺櫒
 *
 * @author lengleng
 * @date 2025/05/30
 * @since 2019-03-19
 */
@RestController
@AllArgsConstructor
@RequestMapping("/dict")
@Tag(description = "dict", name = "瀛楀吀绠＄悊妯″潡")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SysDictController {

	private final SysDictService sysDictService;

	private final SysDictItemService sysDictItemService;

	/**
	 * 閫氳繃ID鏌ヨ瀛楀吀淇℃伅
	 * @param id 瀛楀吀ID
	 * @return 鍖呭惈瀛楀吀淇℃伅鐨勫搷搴斿璞?
	 */

	/**
	 * 閫氳繃瀛楀吀绫诲瀷鏌ユ壘瀛楀吀
	 * @param type 绫诲瀷
	 * @return 鍚岀被鍨嬪瓧鍏?
	 */
	@GetMapping("/type/{type}")
	@Operation(summary = "閫氳繃瀛楀吀绫诲瀷鏌ユ壘瀛楀吀", description = "閫氳繃瀛楀吀绫诲瀷鏌ユ壘瀛楀吀")
	@Cacheable(value = CacheConstants.DICT_DETAILS, key = "#type", unless = "#result.data.isEmpty()")
	public R<List<SysDictItem>> getDictByType(@PathVariable String type) {
		return R.ok(sysDictItemService.list(Wrappers.<SysDictItem>query().lambda().eq(SysDictItem::getDictType, type)));
	}

	/**
	 * 閫氳繃瀛楀吀绫诲瀷鏌ユ壘瀛楀吀 (閽堝feign璋冪敤) TODO: 鍏煎鎬ф柟妗堬紝浠ｇ爜閲嶅
	 * @param type 绫诲瀷
	 * @return 鍚岀被鍨嬪瓧鍏?
	 */
	@Inner
	@GetMapping("/remote/type/{type}")
	@Operation(summary = "閫氳繃瀛楀吀绫诲瀷鏌ユ壘瀛楀吀(閽堝feign璋冪敤)", description = "閫氳繃瀛楀吀绫诲瀷鏌ユ壘瀛楀吀(閽堝feign璋冪敤)", hidden = true)
	@Cacheable(value = CacheConstants.DICT_DETAILS, key = "#type", unless = "#result.data.isEmpty()")
	public R<List<SysDictItem>> getRemoteDictByType(@PathVariable String type) {
		return R.ok(sysDictItemService.list(Wrappers.<SysDictItem>query().lambda().eq(SysDictItem::getDictType, type)));
	}

}
