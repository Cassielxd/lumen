package com.lumencloud.lumen.admin.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumencloud.lumen.admin.api.dto.SysLogDTO;
import com.lumencloud.lumen.admin.api.entity.SysLog;
import com.lumencloud.lumen.admin.service.SysLogService;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.security.annotation.Inner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * System log endpoints.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/log")
@Tag(name = "log", description = "System log management")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SysLogController {

	private final SysLogService sysLogService;

	@GetMapping("/page")
	@Operation(summary = "Page query logs", description = "Page query system logs")
	public R<Page<SysLog>> getLogPage(@ParameterObject Page<SysLog> page, @ParameterObject SysLogDTO sysLog) {
		return R.ok(sysLogService.getLogPage(page, sysLog));
	}

	@GetMapping("/list")
	@Operation(summary = "List logs", description = "List system logs by filters")
	public R<List<SysLog>> list(@ParameterObject SysLogDTO sysLog) {
		return R.ok(sysLogService.listLogs(sysLog));
	}

	@Inner
	@PostMapping
	@Operation(summary = "Save log", hidden = true)
	public R<Boolean> saveLog(@RequestBody SysLog sysLog) {
		return R.ok(sysLogService.saveLog(sysLog));
	}

}
