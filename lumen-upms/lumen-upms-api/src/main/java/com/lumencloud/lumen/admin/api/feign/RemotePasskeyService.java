package com.lumencloud.lumen.admin.api.feign;

import com.lumencloud.lumen.admin.api.dto.PasskeyAccountLookupDTO;
import com.lumencloud.lumen.admin.api.dto.PasskeyCredentialCounterUpdateDTO;
import com.lumencloud.lumen.admin.api.vo.PasskeyAccountInfoVO;
import com.lumencloud.lumen.common.core.constant.ServiceNameConstants;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.feign.annotation.NoToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Internal passkey service.
 */
@FeignClient(contextId = "remotePasskeyService", value = ServiceNameConstants.UPMS_SERVICE)
public interface RemotePasskeyService {

	@NoToken
	@PostMapping("/passkey/account")
	R<PasskeyAccountInfoVO> getAccount(@RequestBody PasskeyAccountLookupDTO request);

	@NoToken
	@PostMapping("/passkey/sign-count")
	R<Boolean> updateSignCount(@RequestBody PasskeyCredentialCounterUpdateDTO request);

}
