package com.lumencloud.lumen.admin.service;

import com.lumencloud.lumen.admin.api.dto.PasskeyCredentialCounterUpdateDTO;
import com.lumencloud.lumen.admin.api.dto.PasskeyRegistrationFinishDTO;
import com.lumencloud.lumen.admin.api.vo.PasskeyAccountInfoVO;
import com.lumencloud.lumen.admin.api.vo.PasskeyCredentialVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface PasskeyService {

	Map<String, Object> createCurrentRegistrationOptions(HttpServletRequest request);

	PasskeyCredentialVO registerCurrentPasskey(HttpServletRequest request, PasskeyRegistrationFinishDTO finishDTO);

	List<PasskeyCredentialVO> listCurrentPasskeys();

	Boolean removeCurrentPasskey(String credentialKey);

	PasskeyAccountInfoVO getAccount(String clientId, String username);

	Boolean updateSignCount(PasskeyCredentialCounterUpdateDTO request);

}
