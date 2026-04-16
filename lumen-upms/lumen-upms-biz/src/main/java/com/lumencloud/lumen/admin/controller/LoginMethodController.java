package com.lumencloud.lumen.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lumencloud.lumen.admin.api.entity.SysDict;
import com.lumencloud.lumen.admin.api.entity.SysDictItem;
import com.lumencloud.lumen.admin.service.SysDictItemService;
import com.lumencloud.lumen.admin.service.SysDictService;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.log.annotation.SysLog;
import com.lumencloud.lumen.common.security.annotation.HasPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Login method dictionary management endpoints.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/login-method")
@Tag(name = "loginMethod", description = "Login method management")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class LoginMethodController {

	private static final String GRANT_TYPES_DICT = "grant_types";

	private final SysDictService sysDictService;

	private final SysDictItemService sysDictItemService;

	@GetMapping("/list")
	@Operation(summary = "List login methods")
	public R<List<SysDictItem>> list() {
		return R.ok(sysDictItemService.list(Wrappers.<SysDictItem>lambdaQuery()
			.eq(SysDictItem::getDictType, GRANT_TYPES_DICT)
			.orderByAsc(SysDictItem::getSortOrder)
			.orderByAsc(SysDictItem::getCreateTime)));
	}

	@GetMapping("/details/{id}")
	@Operation(summary = "Get login method details")
	public R<SysDictItem> getById(@PathVariable Long id) {
		SysDictItem item = sysDictItemService.getById(id);
		if (item == null || !GRANT_TYPES_DICT.equals(item.getDictType())) {
			return R.failed("登录方式不存在");
		}
		return R.ok(item);
	}

	@SysLog("新增登录方式")
	@PostMapping
	@HasPermission("sys_dict_add")
	@CacheEvict(value = CacheConstants.DICT_DETAILS, key = "'grant_types'")
	@Operation(summary = "Create login method")
	public R<Boolean> save(@Valid @RequestBody SysDictItem item) {
		SysDict dict = requireGrantTypeDict();
		item.setDictId(dict.getId());
		item.setDictType(GRANT_TYPES_DICT);
		return R.ok(sysDictItemService.save(item));
	}

	@SysLog("修改登录方式")
	@PutMapping
	@HasPermission("sys_dict_edit")
	@CacheEvict(value = CacheConstants.DICT_DETAILS, key = "'grant_types'")
	@Operation(summary = "Update login method")
	public R update(@Valid @RequestBody SysDictItem item) {
		SysDictItem current = sysDictItemService.getById(item.getId());
		if (current == null || !GRANT_TYPES_DICT.equals(current.getDictType())) {
			return R.failed("登录方式不存在");
		}
		item.setDictId(current.getDictId());
		item.setDictType(GRANT_TYPES_DICT);
		return sysDictItemService.updateDictItem(item);
	}

	@SysLog("删除登录方式")
	@DeleteMapping("/{id}")
	@HasPermission("sys_dict_del")
	@CacheEvict(value = CacheConstants.DICT_DETAILS, key = "'grant_types'")
	@Operation(summary = "Delete login method")
	public R remove(@PathVariable Long id) {
		SysDictItem current = sysDictItemService.getById(id);
		if (current == null || !GRANT_TYPES_DICT.equals(current.getDictType())) {
			return R.failed("登录方式不存在");
		}
		return sysDictItemService.removeDictItem(id);
	}

	private SysDict requireGrantTypeDict() {
		SysDict dict = sysDictService
			.getOne(Wrappers.<SysDict>lambdaQuery().eq(SysDict::getDictType, GRANT_TYPES_DICT), false);
		if (dict == null) {
			throw new IllegalStateException("grant_types dictionary is missing");
		}
		return dict;
	}

}
