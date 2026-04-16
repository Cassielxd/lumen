package com.lumencloud.lumen.auth.endpoint;

import com.lumencloud.lumen.admin.api.dto.PasskeyAccountLookupDTO;
import com.lumencloud.lumen.admin.api.feign.RemotePasskeyService;
import com.lumencloud.lumen.admin.api.vo.PasskeyAccountInfoVO;
import com.lumencloud.lumen.admin.api.vo.PasskeyCredentialVO;
import com.lumencloud.lumen.auth.support.passkey.PasskeyAssertionOptionsRequest;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.core.util.RetOps;
import com.lumencloud.lumen.common.core.util.WebUtils;
import com.lumencloud.lumen.common.security.annotation.Inner;
import com.lumencloud.lumen.common.security.passkey.PasskeyChallengeContext;
import com.lumencloud.lumen.common.security.passkey.PasskeyChallengeService;
import com.lumencloud.lumen.common.security.passkey.PasskeyWebAuthnUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public passkey endpoints used before token issuance.
 */
@RestController
@RequiredArgsConstructor
@Inner(false)
@RequestMapping("/passkey")
@Tag(name = "passkey-auth", description = "Passkey authentication")
public class PasskeyEndpoint {

	private final RemotePasskeyService remotePasskeyService;

	private final PasskeyChallengeService passkeyChallengeService;

	@PostMapping("/assertion/options")
	@Operation(summary = "Create passkey assertion options", description = "Create WebAuthn assertion options")
	public R<Map<String, Object>> assertionOptions(HttpServletRequest request,
			@RequestBody PasskeyAssertionOptionsRequest optionsRequest) {
		String clientId = WebUtils.findClientId(request).orElse(null);
		if (!StringUtils.hasText(clientId)) {
			return R.failed("client_id is required");
		}
		PasskeyAccountLookupDTO lookupDTO = new PasskeyAccountLookupDTO();
		lookupDTO.setClientId(clientId);
		lookupDTO.setUsername(optionsRequest.getUsername());
		PasskeyAccountInfoVO accountInfo = RetOps.of(remotePasskeyService.getAccount(lookupDTO)).getData().orElse(null);
		if (accountInfo == null || accountInfo.getCredentials() == null || accountInfo.getCredentials().isEmpty()) {
			return R.failed("passkey is unavailable for this account");
		}

		List<Map<String, Object>> allowCredentials = accountInfo.getCredentials()
			.stream()
			.filter(credential -> StringUtils.hasText(credential.getCredentialKey()))
			.map(credential -> PasskeyWebAuthnUtils.createDescriptor(credential.getCredentialKey(),
					credential.getPayload() == null ? List.of() : credential.getPayload().getTransports()))
			.toList();
		if (allowCredentials.isEmpty()) {
			return R.failed("passkey is unavailable for this account");
		}

		PasskeyChallengeContext challengeContext = new PasskeyChallengeContext();
		challengeContext.setType(PasskeyChallengeContext.TYPE_ASSERTION);
		challengeContext.setChallenge(PasskeyWebAuthnUtils.randomChallenge());
		challengeContext.setClientId(accountInfo.getClientId());
		challengeContext.setAccountId(accountInfo.getAccountId());
		challengeContext.setUserId(accountInfo.getUserId());
		challengeContext.setUsername(accountInfo.getUsername());
		challengeContext.setRpId(PasskeyWebAuthnUtils.resolveRpId(request));
		challengeContext.setOrigin(PasskeyWebAuthnUtils.resolveOrigin(request));
		passkeyChallengeService.save(challengeContext);

		Map<String, Object> options = new LinkedHashMap<>();
		options.put("challenge", challengeContext.getChallenge());
		options.put("rpId", challengeContext.getRpId());
		options.put("timeout", PasskeyChallengeService.DEFAULT_TTL_SECONDS * 1000);
		options.put("userVerification", "preferred");
		options.put("allowCredentials", allowCredentials);
		return R.ok(options);
	}

}
