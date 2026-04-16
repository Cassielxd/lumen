package com.lumencloud.lumen.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.entity.SysUser;
import com.lumencloud.lumen.admin.mapper.AuthAccountCredentialMapper;
import com.lumencloud.lumen.admin.mapper.AuthAccountMapper;
import com.lumencloud.lumen.admin.service.AuthAccountService;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Authentication account service implementation.
 */
@Service
@RequiredArgsConstructor
public class AuthAccountServiceImpl implements AuthAccountService {

	private static final String CREDENTIAL_KEY_DEFAULT = "";

	private static final String CREDENTIAL_PASSWORD = "PASSWORD";

	private static final String CREDENTIAL_OTP = "OTP";

	private final AuthAccountMapper authAccountMapper;

	private final AuthAccountCredentialMapper authAccountCredentialMapper;

	@Override
	public Optional<AuthAccount> resolveAccount(String clientId, String loginName, String phone) {
		if (!StringUtils.hasText(clientId)) {
			return Optional.empty();
		}
		if (StringUtils.hasText(loginName)) {
			return Optional.ofNullable(authAccountMapper.selectOne(Wrappers.<AuthAccount>lambdaQuery()
				.eq(AuthAccount::getClientId, clientId)
				.eq(AuthAccount::getLoginName, loginName), false));
		}
		if (StringUtils.hasText(phone)) {
			return Optional.ofNullable(authAccountMapper.selectOne(Wrappers.<AuthAccount>lambdaQuery()
				.eq(AuthAccount::getClientId, clientId)
				.eq(AuthAccount::getPhone, phone), false));
		}
		return Optional.empty();
	}

	@Override
	public Optional<AuthAccountCredential> getCredential(Long accountId, String credentialType) {
		if (accountId == null || !StringUtils.hasText(credentialType)) {
			return Optional.empty();
		}
		AuthAccountCredential credential = getCredential(accountId, credentialType, CREDENTIAL_KEY_DEFAULT);
		if (credential != null) {
			return Optional.of(credential);
		}
		return Optional.ofNullable(authAccountCredentialMapper.selectOne(Wrappers.<AuthAccountCredential>lambdaQuery()
			.eq(AuthAccountCredential::getAccountId, accountId)
			.eq(AuthAccountCredential::getCredentialType, credentialType)
			.orderByDesc(AuthAccountCredential::getVerifiedAt)
			.orderByDesc(AuthAccountCredential::getCreateTime), false));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void ensureUserAccounts(SysUser user, Collection<String> clientIds) {
		if (user == null || user.getUserId() == null || CollectionUtils.isEmpty(clientIds)) {
			return;
		}

		Set<String> normalizedClientIds = new LinkedHashSet<>();
		clientIds.stream().filter(StringUtils::hasText).forEach(normalizedClientIds::add);
		if (normalizedClientIds.isEmpty()) {
			return;
		}

		for (String clientId : normalizedClientIds) {
			AuthAccount account = authAccountMapper.selectOne(Wrappers.<AuthAccount>lambdaQuery()
				.eq(AuthAccount::getUserId, user.getUserId())
				.eq(AuthAccount::getClientId, clientId), false);
			if (account == null) {
				account = new AuthAccount();
				account.setUserId(user.getUserId());
				account.setClientId(clientId);
				account.setCreateBy(StringUtils.hasText(user.getUpdateBy()) ? user.getUpdateBy() : user.getCreateBy());
				account.setCreateTime(user.getCreateTime());
			}
			account.setLoginName(user.getUsername());
			account.setPhone(user.getPhone());
			account.setStatus(resolveAccountStatus(user));
			account.setUpdateBy(user.getUpdateBy());
			account.setUpdateTime(LocalDateTime.now());
			if (account.getAccountId() == null) {
				authAccountMapper.insert(account);
			}
			else {
				authAccountMapper.updateById(account);
			}
			syncCredentials(account, user);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void syncUserIdentity(SysUser user) {
		if (user == null || user.getUserId() == null) {
			return;
		}
		List<AuthAccount> accounts = listByUserId(user.getUserId());
		for (AuthAccount account : accounts) {
			account.setLoginName(user.getUsername());
			account.setPhone(user.getPhone());
			account.setStatus(resolveAccountStatus(user));
			account.setUpdateBy(user.getUpdateBy());
			account.setUpdateTime(LocalDateTime.now());
			authAccountMapper.updateById(account);
			syncCredentials(account, user);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeByUserIds(Collection<Long> userIds) {
		if (CollectionUtils.isEmpty(userIds)) {
			return;
		}
		List<AuthAccount> accounts = authAccountMapper.selectList(
				Wrappers.<AuthAccount>lambdaQuery().in(AuthAccount::getUserId, userIds));
		if (accounts.isEmpty()) {
			return;
		}
		List<Long> accountIds = accounts.stream().map(AuthAccount::getAccountId).toList();
		authAccountCredentialMapper.delete(
				Wrappers.<AuthAccountCredential>lambdaQuery().in(AuthAccountCredential::getAccountId, accountIds));
		authAccountMapper.deleteBatchIds(accountIds);
	}

	@Override
	public List<AuthAccount> listByUserId(Long userId) {
		if (userId == null) {
			return Collections.emptyList();
		}
		return authAccountMapper.selectList(Wrappers.<AuthAccount>lambdaQuery().eq(AuthAccount::getUserId, userId));
	}

	private void syncCredentials(AuthAccount account, SysUser user) {
		upsertPasswordCredential(account, user);
		upsertOtpCredential(account, user);
	}

	private void upsertPasswordCredential(AuthAccount account, SysUser user) {
		if (!StringUtils.hasText(user.getPassword())) {
			return;
		}
		AuthAccountCredential credential = getCredential(account.getAccountId(), CREDENTIAL_PASSWORD, CREDENTIAL_KEY_DEFAULT);
		if (credential == null) {
			credential = new AuthAccountCredential();
			credential.setAccountId(account.getAccountId());
			credential.setCredentialType(CREDENTIAL_PASSWORD);
			credential.setCredentialKey(CREDENTIAL_KEY_DEFAULT);
			credential.setCreateBy(StringUtils.hasText(user.getUpdateBy()) ? user.getUpdateBy() : user.getCreateBy());
			credential.setCreateTime(LocalDateTime.now());
		}
		credential.setSecretValue(user.getPassword());
		credential.setStatus(resolveAccountStatus(user));
		credential.setUpdateBy(user.getUpdateBy());
		credential.setUpdateTime(LocalDateTime.now());
		credential.setVerifiedAt(LocalDateTime.now());
		saveCredential(credential);
	}

	private void upsertOtpCredential(AuthAccount account, SysUser user) {
		if (!StringUtils.hasText(user.getPhone())) {
			return;
		}
		AuthAccountCredential credential = getCredential(account.getAccountId(), CREDENTIAL_OTP, CREDENTIAL_KEY_DEFAULT);
		if (credential == null) {
			credential = new AuthAccountCredential();
			credential.setAccountId(account.getAccountId());
			credential.setCredentialType(CREDENTIAL_OTP);
			credential.setCredentialKey(CREDENTIAL_KEY_DEFAULT);
			credential.setCreateBy(StringUtils.hasText(user.getUpdateBy()) ? user.getUpdateBy() : user.getCreateBy());
			credential.setCreateTime(LocalDateTime.now());
		}
		credential.setSecretValue(user.getPhone());
		credential.setStatus(resolveAccountStatus(user));
		credential.setUpdateBy(user.getUpdateBy());
		credential.setUpdateTime(LocalDateTime.now());
		saveCredential(credential);
	}

	private AuthAccountCredential getCredential(Long accountId, String type, String key) {
		return authAccountCredentialMapper.selectOne(Wrappers.<AuthAccountCredential>lambdaQuery()
			.eq(AuthAccountCredential::getAccountId, accountId)
			.eq(AuthAccountCredential::getCredentialType, type)
			.eq(AuthAccountCredential::getCredentialKey, key), false);
	}

	private void saveCredential(AuthAccountCredential credential) {
		if (credential.getCredentialId() == null) {
			authAccountCredentialMapper.insert(credential);
		}
		else {
			authAccountCredentialMapper.updateById(credential);
		}
	}

	private String resolveAccountStatus(SysUser user) {
		return StringUtils.hasText(user.getLockFlag()) ? user.getLockFlag() : CommonConstants.STATUS_NORMAL;
	}

}
