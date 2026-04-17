package com.lumencloud.lumen.admin.service;

import com.lumencloud.lumen.admin.api.dto.AuthSessionSaveDTO;
import com.lumencloud.lumen.admin.api.entity.AuthSession;
import com.lumencloud.lumen.admin.api.vo.AuthSessionVO;

import java.util.List;

public interface AuthSessionService {

	AuthSession saveSession(AuthSessionSaveDTO request);

	Boolean logoutByAccessToken(String accessToken);

	AuthSession getByAccessToken(String accessToken);

	AuthSession getByRefreshToken(String refreshToken);

	List<AuthSessionVO> listByAccountId(Long accountId, String currentSid);

	Boolean logoutBySid(Long accountId, String sid);

	Boolean logoutOtherSessions(Long accountId, String currentSid);

	List<AuthSessionVO> listAll(String clientId, String principalName, String status);

	Boolean adminLogoutBySid(String sid);

}
