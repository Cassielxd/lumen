package com.lumencloud.lumen.admin.controller;

import com.lumencloud.lumen.admin.api.dto.AuthSessionLogoutDTO;
import com.lumencloud.lumen.admin.api.dto.AuthSessionSaveDTO;
import com.lumencloud.lumen.admin.api.entity.AuthSession;
import com.lumencloud.lumen.admin.api.vo.AuthSessionVO;
import com.lumencloud.lumen.admin.service.AuthSessionService;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.security.annotation.Inner;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal auth session endpoints.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth-session")
@Tag(name = "auth-session", description = "Authentication session")
public class AuthSessionController {

	private final AuthSessionService authSessionService;

	@Inner
	@PostMapping("/login")
	@Operation(summary = "Save session", hidden = true)
	public R<AuthSession> saveSession(@RequestBody AuthSessionSaveDTO request) {
		return R.ok(authSessionService.saveSession(request));
	}

	@Inner
	@PostMapping("/logout")
	@Operation(summary = "Logout session", hidden = true)
	public R<Boolean> logout(@RequestBody AuthSessionLogoutDTO request) {
		return R.ok(authSessionService.logoutByAccessToken(request.getAccessToken()));
	}

	@Inner
	@PostMapping("/access-token")
	@Operation(summary = "Find session by access token", hidden = true)
	public R<AuthSession> getByAccessToken(@RequestBody AuthSessionLogoutDTO request) {
		return R.ok(authSessionService.getByAccessToken(request.getAccessToken()));
	}

	@Inner
	@PostMapping("/refresh-token")
	@Operation(summary = "Find session by refresh token", hidden = true)
	public R<AuthSession> getByRefreshToken(@RequestBody AuthSessionLogoutDTO request) {
		return R.ok(authSessionService.getByRefreshToken(request.getRefreshToken()));
	}

	@GetMapping("/current/list")
	@Operation(summary = "List current account sessions", description = "List current account sessions")
	public R<List<AuthSessionVO>> currentSessions() {
		return R.ok(authSessionService.listByAccountId(SecurityUtils.getUser().getAccountId(),
				SecurityUtils.getSessionId()));
	}

	@DeleteMapping("/current/{sid}")
	@Operation(summary = "Revoke session", description = "Revoke one session of current account")
	public R<Boolean> revokeCurrentSession(@PathVariable String sid) {
		return R.ok(authSessionService.logoutBySid(SecurityUtils.getUser().getAccountId(), sid));
	}

	@DeleteMapping("/current/others")
	@Operation(summary = "Revoke other sessions", description = "Revoke all other sessions of current account")
	public R<Boolean> revokeOtherSessions() {
		return R.ok(authSessionService.logoutOtherSessions(SecurityUtils.getUser().getAccountId(),
				SecurityUtils.getSessionId()));
	}

}
