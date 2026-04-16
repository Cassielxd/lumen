package com.lumencloud.lumen.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumencloud.lumen.admin.api.entity.SysPublicParam;
import com.lumencloud.lumen.admin.service.SysPublicParamService;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.log.annotation.SysLog;
import com.lumencloud.lumen.common.security.annotation.HasPermission;
import com.lumencloud.lumen.common.security.annotation.Inner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public parameter management endpoints.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/param")
@Tag(name = "param", description = "Public parameter management")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SysPublicParamController {

    private final SysPublicParamService sysPublicParamService;

    @Inner
    @GetMapping("/publicValue/{key}")
    @Operation(summary = "Get parameter value by key", hidden = true)
    public R<String> getByKey(@PathVariable("key") String publicKey) {
        return R.ok(sysPublicParamService.getParamValue(publicKey));
    }

    @GetMapping("/details/{id}")
    @Operation(summary = "Get parameter details by id")
    public R<SysPublicParam> getById(@PathVariable("id") Long id) {
        return R.ok(sysPublicParamService.getById(id));
    }

    @GetMapping("/details")
    @Operation(summary = "Get parameter details")
    public R<SysPublicParam> getDetails(@ParameterObject SysPublicParam query) {
        return R.ok(sysPublicParamService.getOne(Wrappers.query(query), false));
    }

    @GetMapping("/page")
    @Operation(summary = "Page query parameters")
    public R<Page<SysPublicParam>> getPage(@ParameterObject Page<SysPublicParam> page,
            @ParameterObject SysPublicParam query) {
        return R.ok(sysPublicParamService.page(page, Wrappers.<SysPublicParam>lambdaQuery()
            .like(StrUtil.isNotBlank(query.getPublicName()), SysPublicParam::getPublicName, query.getPublicName())
            .like(StrUtil.isNotBlank(query.getPublicKey()), SysPublicParam::getPublicKey, query.getPublicKey())
            .eq(StrUtil.isNotBlank(query.getStatus()), SysPublicParam::getStatus, query.getStatus())
            .eq(StrUtil.isNotBlank(query.getPublicType()), SysPublicParam::getPublicType, query.getPublicType())
            .orderByDesc(SysPublicParam::getCreateTime)));
    }

    @SysLog("Add public parameter")
    @PostMapping
    @HasPermission("sys_syspublicparam_add")
    @Operation(summary = "Create parameter")
    public R<Boolean> save(@RequestBody SysPublicParam sysPublicParam) {
        return R.ok(sysPublicParamService.save(sysPublicParam));
    }

    @SysLog("Update public parameter")
    @PutMapping
    @HasPermission("sys_syspublicparam_edit")
    @Operation(summary = "Update parameter")
    public R update(@RequestBody SysPublicParam sysPublicParam) {
        return sysPublicParamService.updateParam(sysPublicParam);
    }

    @SysLog("Delete public parameters")
    @DeleteMapping
    @HasPermission("sys_syspublicparam_del")
    @Operation(summary = "Delete parameters")
    public R remove(@RequestBody Long[] publicIds) {
        return sysPublicParamService.removeParamByIds(publicIds);
    }

    @SysLog("Sync public parameter cache")
    @PutMapping("/sync")
    @HasPermission("sys_syspublicparam_edit")
    @Operation(summary = "Sync parameter cache")
    public R syncParam() {
        return sysPublicParamService.syncParamCache();
    }

}
