package com.lumencloud.lumen.admin.service;

import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.entity.SysUser;
import com.lumencloud.lumen.admin.api.vo.AuthAccountCredentialManageVO;
import com.lumencloud.lumen.admin.api.vo.AuthAccountIdentifierManageVO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Client-bound authentication account service.
 */
public interface AuthAccountService {

	Optional<AuthAccount> resolveAccount(String clientId, String loginName, String phone);

	Optional<AuthAccountCredential> getCredential(Long accountId, String credentialType);

	void ensureUserAccounts(SysUser user, Collection<String> clientIds);

	void syncUserProfile(SysUser user);

	void syncPasswordCredential(Long userId, String encodedPassword, String updatedBy);

	void syncPasswordCredentialForClients(Long userId, Collection<String> clientIds, String encodedPassword,
			String updatedBy);

	void updatePasswordCredential(Long accountId, String encodedPassword, String updatedBy);

	void syncOtpCredential(Long userId, String phone, String status, String updatedBy);

	void removeByUserIds(Collection<Long> userIds);

	List<AuthAccount> listByUserId(Long userId);

	List<AuthAccountCredentialManageVO> listManageAccounts(String clientId, String loginName, String phone);

	Boolean resetPassword(Long accountId, String rawPassword, String updatedBy);

	Boolean updateOtpStatus(Long accountId, String status, String updatedBy);

	Boolean clearPasskeys(Long accountId, String updatedBy);

	List<AuthAccountIdentifierManageVO> listIdentifiers(Long accountId);

	Boolean saveIdentifier(Long accountId, String identifierType, String identifierValue, String updatedBy);

	Boolean removeIdentifier(Long identifierId, String updatedBy);

}
