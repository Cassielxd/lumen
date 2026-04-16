package com.lumencloud.lumen.admin.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumencloud.lumen.admin.api.entity.SysOauthClientDetails;
import com.lumencloud.lumen.admin.api.vo.PublicLoginClientVO;
import com.lumencloud.lumen.admin.service.SysOauthClientDetailsService;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.log.annotation.SysLog;
import com.lumencloud.lumen.common.security.annotation.HasPermission;
import com.lumencloud.lumen.common.security.annotation.Inner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

/**
 * OAuth client management endpoints.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/client")
@Tag(name = "client", description = "OAuth client management")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SysClientController {

    private final SysOauthClientDetailsService clientDetailsService;

    @Inner
    @GetMapping("/getClientDetailsById/{clientId}")
    @Operation(summary = "Get client details by client id", hidden = true)
    public R<SysOauthClientDetails> getClientDetailsById(@PathVariable String clientId) {
        SysOauthClientDetails client = clientDetailsService.getOne(
                Wrappers.<SysOauthClientDetails>lambdaQuery().eq(SysOauthClientDetails::getClientId, clientId),
                false);
        return R.ok(client);
    }

    @GetMapping("/public/list")
    @Operation(summary = "List public login clients", hidden = true)
    public R<List<PublicLoginClientVO>> listPublicClients() {
        List<PublicLoginClientVO> clients = clientDetailsService.list(Wrappers.<SysOauthClientDetails>lambdaQuery()
                .orderByAsc(SysOauthClientDetails::getId))
                .stream()
                .map(this::toPublicLoginClient)
                .toList();
        return R.ok(clients);
    }

    @GetMapping("/details/{id}")
    @Operation(summary = "Get client details by id")
    public R<SysOauthClientDetails> getById(@PathVariable Long id) {
        return R.ok(clientDetailsService.getById(id));
    }

    @GetMapping("/details")
    @Operation(summary = "Get client details")
    public R<SysOauthClientDetails> getDetails(@ParameterObject SysOauthClientDetails query) {
        return R.ok(clientDetailsService.getOne(Wrappers.query(query), false));
    }

    @GetMapping("/page")
    @Operation(summary = "Page query clients")
    public R<Page<SysOauthClientDetails>> getClientPage(@ParameterObject Page<SysOauthClientDetails> page,
            @ParameterObject SysOauthClientDetails query) {
        return R.ok(clientDetailsService.getClientPage(page, query));
    }

    @SysLog("Add OAuth client")
    @PostMapping
    @HasPermission("sys_client_add")
    @Operation(summary = "Create client")
    public R<Boolean> saveClient(@Valid @RequestBody SysOauthClientDetails clientDetails) {
        return R.ok(clientDetailsService.saveClient(clientDetails));
    }

    @SysLog("Update OAuth client")
    @PutMapping
    @HasPermission("sys_client_edit")
    @Operation(summary = "Update client")
    public R<Boolean> updateClient(@Valid @RequestBody SysOauthClientDetails clientDetails) {
        return R.ok(clientDetailsService.updateClientById(clientDetails));
    }

    @SysLog("Delete OAuth clients")
    @DeleteMapping
    @HasPermission("sys_client_del")
    @Operation(summary = "Delete clients")
    public R<Boolean> removeByIds(@RequestBody Long[] ids) {
        return R.ok(clientDetailsService.removeBatchByIds(CollUtil.toList(ids)));
    }

    @SysLog("Sync OAuth client cache")
    @PutMapping("/sync")
    @HasPermission("sys_client_edit")
    @Operation(summary = "Sync client cache")
    public R syncClientCache() {
        return clientDetailsService.syncClientCache();
    }

    private PublicLoginClientVO toPublicLoginClient(SysOauthClientDetails client) {
        JSONObject additionalInformation = parseAdditionalInformation(client.getAdditionalInformation());
        PublicLoginClientVO vo = new PublicLoginClientVO();
        vo.setClientId(client.getClientId());
        vo.setClientSecret(client.getClientSecret());
        vo.setScope(client.getScope());
        vo.setAuthorizedGrantTypes(client.getAuthorizedGrantTypes());
        vo.setRequiresCaptcha(resolveBooleanFlag(additionalInformation, "captcha_flag", "requiresCaptcha", true));
        vo.setEncryptPassword(resolveBooleanFlag(additionalInformation, "enc_flag", "encryptPassword", true));
        vo.setDisplayName(resolveText(additionalInformation, "display_name", "displayName", "label", client.getClientId()));
        vo.setAudience(resolveText(additionalInformation, "audience", "clientAudience", client.getClientId()));
        vo.setDescription(resolveText(additionalInformation, "description", "clientDescription", "动态客户端"));
        return vo;
    }

    private JSONObject parseAdditionalInformation(String raw) {
        if (StrUtil.isBlank(raw) || !JSONUtil.isTypeJSON(raw)) {
            return new JSONObject();
        }
        return JSONUtil.parseObj(raw);
    }

    private Boolean resolveBooleanFlag(JSONObject json, String legacyKey, String key, boolean defaultValue) {
        if (json.containsKey(key)) {
            Object value = json.get(key);
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
            return "1".equals(String.valueOf(value)) || BooleanUtil.toBoolean(String.valueOf(value));
        }
        if (json.containsKey(legacyKey)) {
            Object value = json.get(legacyKey);
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
            return "1".equals(String.valueOf(value)) || BooleanUtil.toBoolean(String.valueOf(value));
        }
        return defaultValue;
    }

    private String resolveText(JSONObject json, String key1, String key2, String fallback) {
        return resolveText(json, key1, key2, null, fallback);
    }

    private String resolveText(JSONObject json, String key1, String key2, String key3, String fallback) {
        for (String key : new String[] { key1, key2, key3 }) {
            if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(json.getStr(key))) {
                return json.getStr(key);
            }
        }
        return fallback;
    }

}
