package com.lumencloud.lumen.admin.service;

import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.entity.SysUser;

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

	void syncUserIdentity(SysUser user);

	void removeByUserIds(Collection<Long> userIds);

	List<AuthAccount> listByUserId(Long userId);

}
