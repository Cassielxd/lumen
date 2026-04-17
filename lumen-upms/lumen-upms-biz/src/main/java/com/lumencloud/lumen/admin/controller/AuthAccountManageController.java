package com.lumencloud.lumen.admin.controller;

import com.lumencloud.lumen.admin.api.dto.AuthAccountCredentialStatusDTO;
import com.lumencloud.lumen.admin.api.dto.AuthAccountIdentifierUpsertDTO;
import com.lumencloud.lumen.admin.api.dto.AuthAccountPasswordResetDTO;
import com.lumencloud.lumen.admin.api.vo.AuthAccountCredentialManageVO;
import com.lumencloud.lumen.admin.api.vo.AuthAccountIdentifierManageVO;
import com.lumencloud.lumen.admin.service.AuthAccountService;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.log.annotation.SysLog;
import com.lumencloud.lumen.common.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

/**
 * Platform account credential governance endpoints.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth-account/manage")
@Tag(name = "auth-account-manage", description = "Platform account credential governance")
public class AuthAccountManageController {

	private final AuthAccountService authAccountService;

	@GetMapping("/list")
	@Operation(summary = "List account credentials", description = "List account credential summaries for platform governance")
	public R<List<AuthAccountCredentialManageVO>> list(@RequestParam(required = false) String clientId,
			@RequestParam(required = false) String loginName, @RequestParam(required = false) String phone) {
		return R.ok(authAccountService.listManageAccounts(clientId, loginName, phone));
	}

	@PutMapping("/password")
	@SysLog("重置账号密码")
	@Operation(summary = "Reset password", description = "Reset password for one account owner")
	public R<Boolean> resetPassword(@RequestBody AuthAccountPasswordResetDTO request) {
		return R.ok(authAccountService.resetPassword(request.getAccountId(), request.getNewPassword(), currentOperator()));
	}

	@PutMapping("/otp-status")
	@SysLog("更新账号 OTP 状态")
	@Operation(summary = "Update OTP status", description = "Enable or disable OTP credential for one account")
	public R<Boolean> updateOtpStatus(@RequestBody AuthAccountCredentialStatusDTO request) {
		return R.ok(authAccountService.updateOtpStatus(request.getAccountId(), request.getStatus(), currentOperator()));
	}

	@DeleteMapping("/passkeys/{accountId}")
	@SysLog("清空账号 Passkey")
	@Operation(summary = "Clear passkeys", description = "Disable all passkeys for one account")
	public R<Boolean> clearPasskeys(@PathVariable Long accountId) {
		return R.ok(authAccountService.clearPasskeys(accountId, currentOperator()));
	}

	@GetMapping("/identifiers")
	@Operation(summary = "List identifiers", description = "List account identifiers for one account")
	public R<List<AuthAccountIdentifierManageVO>> identifiers(@RequestParam Long accountId) {
		return R.ok(authAccountService.listIdentifiers(accountId));
	}

	@PostMapping("/identifier")
	@SysLog("保存账号标识")
	@Operation(summary = "Save identifier", description = "Create or reactivate one account identifier")
	public R<Boolean> saveIdentifier(@RequestBody AuthAccountIdentifierUpsertDTO request) {
		return R.ok(authAccountService.saveIdentifier(request.getAccountId(), request.getIdentifierType(),
				request.getIdentifierValue(), currentOperator()));
	}

	@DeleteMapping("/identifier/{identifierId}")
	@SysLog("删除账号标识")
	@Operation(summary = "Remove identifier", description = "Delete one non-primary account identifier")
	public R<Boolean> removeIdentifier(@PathVariable Long identifierId) {
		return R.ok(authAccountService.removeIdentifier(identifierId, currentOperator()));
	}

	private String currentOperator() {
		return SecurityUtils.getUser() == null ? "system" : SecurityUtils.getUser().getUsername();
	}

}
