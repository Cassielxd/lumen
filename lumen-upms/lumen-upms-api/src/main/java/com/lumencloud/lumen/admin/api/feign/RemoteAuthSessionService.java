package com.lumencloud.lumen.admin.api.feign;

import com.lumencloud.lumen.admin.api.dto.AuthSessionLogoutDTO;
import com.lumencloud.lumen.admin.api.dto.AuthSessionSaveDTO;
import com.lumencloud.lumen.admin.api.entity.AuthSession;
import com.lumencloud.lumen.common.core.constant.ServiceNameConstants;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Internal auth session service.
 */
@FeignClient(contextId = "remoteAuthSessionService", value = ServiceNameConstants.UPMS_SERVICE)
public interface RemoteAuthSessionService {

	@NoToken
	@PostMapping("/auth-session/login")
	R<AuthSession> saveSession(@RequestBody AuthSessionSaveDTO request);

	@NoToken
	@PostMapping("/auth-session/logout")
	R<Boolean> logout(@RequestBody AuthSessionLogoutDTO request);

	@NoToken
	@PostMapping("/auth-session/access-token")
	R<AuthSession> getByAccessToken(@RequestBody AuthSessionLogoutDTO request);

	@NoToken
	@PostMapping("/auth-session/refresh-token")
	R<AuthSession> getByRefreshToken(@RequestBody AuthSessionLogoutDTO request);

}
