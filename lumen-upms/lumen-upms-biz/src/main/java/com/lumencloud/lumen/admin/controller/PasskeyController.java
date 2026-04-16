package com.lumencloud.lumen.admin.controller;

import com.lumencloud.lumen.admin.api.dto.PasskeyAccountLookupDTO;
import com.lumencloud.lumen.admin.api.dto.PasskeyCredentialCounterUpdateDTO;
import com.lumencloud.lumen.admin.api.dto.PasskeyRegistrationFinishDTO;
import com.lumencloud.lumen.admin.api.vo.PasskeyAccountInfoVO;
import com.lumencloud.lumen.admin.api.vo.PasskeyCredentialVO;
import com.lumencloud.lumen.admin.service.PasskeyService;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.security.annotation.Inner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Passkey endpoints.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/passkey")
@Tag(name = "passkey", description = "Passkey management")
public class PasskeyController {

	private final PasskeyService passkeyService;

	@Inner
	@PostMapping("/account")
	@Operation(summary = "Get passkey account", hidden = true)
	public R<PasskeyAccountInfoVO> account(@RequestBody PasskeyAccountLookupDTO request) {
		return R.ok(passkeyService.getAccount(request.getClientId(), request.getUsername()));
	}

	@Inner
	@PostMapping("/sign-count")
	@Operation(summary = "Update sign count", hidden = true)
	public R<Boolean> signCount(@RequestBody PasskeyCredentialCounterUpdateDTO request) {
		return R.ok(passkeyService.updateSignCount(request));
	}

	@GetMapping("/current/list")
	@Operation(summary = "List current passkeys", description = "List passkeys of current account")
	public R<List<PasskeyCredentialVO>> currentList() {
		return R.ok(passkeyService.listCurrentPasskeys());
	}

	@PostMapping("/current/register/options")
	@Operation(summary = "Create registration options", description = "Create WebAuthn registration options")
	public R<Map<String, Object>> registerOptions(HttpServletRequest request) {
		return R.ok(passkeyService.createCurrentRegistrationOptions(request));
	}

	@PostMapping("/current/register")
	@Operation(summary = "Finish passkey registration", description = "Complete WebAuthn registration")
	public R<PasskeyCredentialVO> register(HttpServletRequest request,
			@RequestBody PasskeyRegistrationFinishDTO finishDTO) {
		return R.ok(passkeyService.registerCurrentPasskey(request, finishDTO));
	}

	@DeleteMapping("/current/{credentialKey}")
	@Operation(summary = "Delete passkey", description = "Delete current account passkey")
	public R<Boolean> delete(@PathVariable String credentialKey) {
		return R.ok(passkeyService.removeCurrentPasskey(credentialKey));
	}

}
