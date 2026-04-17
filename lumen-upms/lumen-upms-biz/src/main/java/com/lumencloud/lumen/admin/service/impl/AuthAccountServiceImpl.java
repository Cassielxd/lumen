package com.lumencloud.lumen.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.entity.AuthAccountIdentifier;
import com.lumencloud.lumen.admin.api.entity.SysUser;
import com.lumencloud.lumen.admin.api.vo.AuthAccountIdentifierManageVO;
import com.lumencloud.lumen.admin.mapper.AuthAccountCredentialMapper;
import com.lumencloud.lumen.admin.mapper.AuthAccountIdentifierMapper;
import com.lumencloud.lumen.admin.mapper.AuthAccountMapper;
import com.lumencloud.lumen.admin.mapper.SysUserMapper;
import com.lumencloud.lumen.admin.service.AuthAccountService;
import com.lumencloud.lumen.admin.api.vo.AuthAccountCredentialManageVO;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

	private static final String IDENTIFIER_USERNAME = "USERNAME";

	private static final String IDENTIFIER_PHONE = "PHONE";

	private static final String IDENTIFIER_EMAIL = "EMAIL";

	private static final String PRIMARY_FLAG_YES = "1";

	private static final String PRIMARY_FLAG_NO = "0";

	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

	private final AuthAccountMapper authAccountMapper;

	private final AuthAccountCredentialMapper authAccountCredentialMapper;

	private final AuthAccountIdentifierMapper authAccountIdentifierMapper;

	private final SysUserMapper sysUserMapper;

	@Override
	public Optional<AuthAccount> resolveAccount(String clientId, String loginName, String phone) {
		if (!StringUtils.hasText(clientId)) {
			return Optional.empty();
		}
		if (StringUtils.hasText(loginName)) {
			Optional<AuthAccount> account = resolveByIdentifier(clientId, IDENTIFIER_USERNAME, loginName);
			if (account.isPresent()) {
				return account;
			}
			return Optional.ofNullable(authAccountMapper.selectOne(Wrappers.<AuthAccount>lambdaQuery()
				.eq(AuthAccount::getClientId, clientId)
				.eq(AuthAccount::getLoginName, loginName), false));
		}
		if (StringUtils.hasText(phone)) {
			Optional<AuthAccount> account = resolveByIdentifier(clientId, IDENTIFIER_PHONE, phone);
			if (account.isPresent()) {
				return account;
			}
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
			syncIdentifiers(account, user);
			upsertPasswordCredential(account, user.getPassword(), account.getStatus(), user.getUpdateBy(), user.getCreateBy());
			upsertOtpCredential(account, user.getPhone(), account.getStatus(), user.getUpdateBy(), user.getCreateBy());
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void syncUserProfile(SysUser user) {
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
			syncIdentifiers(account, user);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void syncPasswordCredential(Long userId, String encodedPassword, String updatedBy) {
		if (userId == null || !StringUtils.hasText(encodedPassword)) {
			return;
		}
		List<AuthAccount> accounts = listByUserId(userId);
		for (AuthAccount account : accounts) {
			upsertPasswordCredential(account, encodedPassword, account.getStatus(), updatedBy, updatedBy);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void syncPasswordCredentialForClients(Long userId, Collection<String> clientIds, String encodedPassword,
			String updatedBy) {
		if (userId == null || !StringUtils.hasText(encodedPassword) || CollectionUtils.isEmpty(clientIds)) {
			return;
		}
		Set<String> normalizedClientIds = new LinkedHashSet<>();
		clientIds.stream().filter(StringUtils::hasText).forEach(normalizedClientIds::add);
		if (normalizedClientIds.isEmpty()) {
			return;
		}
		List<AuthAccount> accounts = listByUserId(userId);
		for (AuthAccount account : accounts) {
			if (!normalizedClientIds.contains(account.getClientId())) {
				continue;
			}
			upsertPasswordCredential(account, encodedPassword, account.getStatus(), updatedBy, updatedBy);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updatePasswordCredential(Long accountId, String encodedPassword, String updatedBy) {
		if (accountId == null || !StringUtils.hasText(encodedPassword)) {
			return;
		}
		AuthAccount account = authAccountMapper.selectById(accountId);
		if (account == null) {
			return;
		}
		upsertPasswordCredential(account, encodedPassword, account.getStatus(), updatedBy, updatedBy);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void syncOtpCredential(Long userId, String phone, String status, String updatedBy) {
		if (userId == null) {
			return;
		}
		List<AuthAccount> accounts = listByUserId(userId);
		for (AuthAccount account : accounts) {
			upsertOtpCredential(account, phone, status, updatedBy, updatedBy);
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
		authAccountIdentifierMapper
			.delete(Wrappers.<AuthAccountIdentifier>lambdaQuery().in(AuthAccountIdentifier::getAccountId, accountIds));
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

	@Override
	public List<AuthAccountCredentialManageVO> listManageAccounts(String clientId, String loginName, String phone) {
		List<AuthAccount> accounts = authAccountMapper.selectList(Wrappers.<AuthAccount>lambdaQuery()
			.eq(StringUtils.hasText(clientId), AuthAccount::getClientId, clientId)
			.like(StringUtils.hasText(loginName), AuthAccount::getLoginName, loginName)
			.eq(StringUtils.hasText(phone), AuthAccount::getPhone, phone)
			.orderByAsc(AuthAccount::getClientId)
			.orderByAsc(AuthAccount::getLoginName)
			.orderByDesc(AuthAccount::getCreateTime));
		if (accounts.isEmpty()) {
			return List.of();
		}
		List<Long> accountIds = accounts.stream().map(AuthAccount::getAccountId).toList();
		List<AuthAccountCredential> credentials = authAccountCredentialMapper.selectList(Wrappers
			.<AuthAccountCredential>lambdaQuery()
			.in(AuthAccountCredential::getAccountId, accountIds)
			.orderByDesc(AuthAccountCredential::getVerifiedAt)
			.orderByDesc(AuthAccountCredential::getCreateTime));
		List<AuthAccountIdentifier> identifiers = authAccountIdentifierMapper.selectList(Wrappers
			.<AuthAccountIdentifier>lambdaQuery()
			.in(AuthAccountIdentifier::getAccountId, accountIds)
			.orderByAsc(AuthAccountIdentifier::getIdentifierType)
			.orderByDesc(AuthAccountIdentifier::getPrimaryFlag)
			.orderByAsc(AuthAccountIdentifier::getCreateTime));
		return accounts.stream().map(account -> toManageView(account, credentials, identifiers)).toList();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean resetPassword(Long accountId, String rawPassword, String updatedBy) {
		if (accountId == null || !StringUtils.hasText(rawPassword)) {
			return Boolean.FALSE;
		}
		AuthAccount account = authAccountMapper.selectById(accountId);
		if (account == null) {
			return Boolean.FALSE;
		}
		String encodedPassword = PASSWORD_ENCODER.encode(rawPassword);
		updatePasswordCredential(accountId, encodedPassword, updatedBy);
		syncLegacyUserPassword(account.getUserId(), encodedPassword, updatedBy);
		return Boolean.TRUE;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean updateOtpStatus(Long accountId, String status, String updatedBy) {
		if (accountId == null || !StringUtils.hasText(status)) {
			return Boolean.FALSE;
		}
		AuthAccount account = authAccountMapper.selectById(accountId);
		if (account == null) {
			return Boolean.FALSE;
		}
		if (CommonConstants.STATUS_NORMAL.equals(status) && !StringUtils.hasText(account.getPhone())) {
			return Boolean.FALSE;
		}
		upsertOtpCredential(account, account.getPhone(), status, updatedBy, updatedBy);
		return Boolean.TRUE;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean clearPasskeys(Long accountId, String updatedBy) {
		if (accountId == null) {
			return Boolean.FALSE;
		}
		List<AuthAccountCredential> credentials = authAccountCredentialMapper.selectList(Wrappers
			.<AuthAccountCredential>lambdaQuery()
			.eq(AuthAccountCredential::getAccountId, accountId)
			.eq(AuthAccountCredential::getCredentialType, "PASSKEY")
			.eq(AuthAccountCredential::getStatus, CommonConstants.STATUS_NORMAL));
		credentials.forEach(credential -> {
			credential.setStatus(CommonConstants.STATUS_LOCK);
			credential.setUpdateBy(updatedBy);
			credential.setUpdateTime(LocalDateTime.now());
			authAccountCredentialMapper.updateById(credential);
		});
		return Boolean.TRUE;
	}

	@Override
	public List<AuthAccountIdentifierManageVO> listIdentifiers(Long accountId) {
		if (accountId == null) {
			return List.of();
		}
		List<AuthAccountIdentifier> identifiers = authAccountIdentifierMapper.selectList(Wrappers
			.<AuthAccountIdentifier>lambdaQuery()
			.eq(AuthAccountIdentifier::getAccountId, accountId)
			.orderByAsc(AuthAccountIdentifier::getIdentifierType)
			.orderByDesc(AuthAccountIdentifier::getPrimaryFlag)
			.orderByAsc(AuthAccountIdentifier::getCreateTime));
		return identifiers.stream().map(this::toIdentifierView).toList();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean saveIdentifier(Long accountId, String identifierType, String identifierValue, String updatedBy) {
		if (accountId == null || !StringUtils.hasText(identifierType) || !StringUtils.hasText(identifierValue)) {
			return Boolean.FALSE;
		}
		AuthAccount account = authAccountMapper.selectById(accountId);
		if (account == null) {
			return Boolean.FALSE;
		}

		String normalizedType = normalizeIdentifierType(identifierType);
		String normalizedValue = normalizeIdentifierValue(normalizedType, identifierValue);
		if (!StringUtils.hasText(normalizedValue)) {
			return Boolean.FALSE;
		}

		AuthAccountIdentifier duplicate = authAccountIdentifierMapper.selectOne(Wrappers
			.<AuthAccountIdentifier>lambdaQuery()
			.eq(AuthAccountIdentifier::getClientId, account.getClientId())
			.eq(AuthAccountIdentifier::getIdentifierType, normalizedType)
			.eq(AuthAccountIdentifier::getIdentifierValue, normalizedValue), false);
		if (duplicate != null && !accountId.equals(duplicate.getAccountId())) {
			return Boolean.FALSE;
		}

		AuthAccountIdentifier identifier = duplicate;
		if (identifier == null) {
			identifier = authAccountIdentifierMapper.selectOne(Wrappers.<AuthAccountIdentifier>lambdaQuery()
				.eq(AuthAccountIdentifier::getAccountId, accountId)
				.eq(AuthAccountIdentifier::getIdentifierType, normalizedType)
				.eq(AuthAccountIdentifier::getIdentifierValue, normalizedValue), false);
		}
		if (identifier == null) {
			identifier = new AuthAccountIdentifier();
			identifier.setAccountId(accountId);
			identifier.setCreateBy(updatedBy);
			identifier.setCreateTime(LocalDateTime.now());
			identifier.setPrimaryFlag(PRIMARY_FLAG_NO);
		}

		identifier.setClientId(account.getClientId());
		identifier.setIdentifierType(normalizedType);
		identifier.setIdentifierValue(normalizedValue);
		identifier.setStatus(account.getStatus());
		identifier.setVerifiedAt(resolveIdentifierVerifiedAt(normalizedType, normalizedValue, account));
		identifier.setUpdateBy(updatedBy);
		identifier.setUpdateTime(LocalDateTime.now());

		if (identifier.getIdentifierId() == null) {
			authAccountIdentifierMapper.insert(identifier);
		}
		else {
			authAccountIdentifierMapper.updateById(identifier);
		}
		return Boolean.TRUE;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean removeIdentifier(Long identifierId, String updatedBy) {
		if (identifierId == null) {
			return Boolean.FALSE;
		}
		AuthAccountIdentifier identifier = authAccountIdentifierMapper.selectById(identifierId);
		if (identifier == null) {
			return Boolean.FALSE;
		}
		if (PRIMARY_FLAG_YES.equals(identifier.getPrimaryFlag())) {
			return Boolean.FALSE;
		}
		return authAccountIdentifierMapper.deleteById(identifierId) > 0;
	}

	private void syncIdentifiers(AuthAccount account, SysUser user) {
		replaceIdentifier(account, IDENTIFIER_USERNAME, user.getUsername(), true);
		replaceIdentifier(account, IDENTIFIER_PHONE, user.getPhone(), false);
		replaceIdentifier(account, IDENTIFIER_EMAIL, user.getEmail(), false);
		refreshIdentifierStatus(account);
	}

	private void replaceIdentifier(AuthAccount account, String type, String value, boolean verified) {
		List<AuthAccountIdentifier> identifiers = authAccountIdentifierMapper.selectList(Wrappers
			.<AuthAccountIdentifier>lambdaQuery()
			.eq(AuthAccountIdentifier::getAccountId, account.getAccountId())
			.eq(AuthAccountIdentifier::getIdentifierType, type));

		String normalizedValue = normalizeIdentifierValue(type, value);
		if (!StringUtils.hasText(normalizedValue)) {
			identifiers.stream()
				.filter(identifier -> PRIMARY_FLAG_YES.equals(identifier.getPrimaryFlag()))
				.forEach(identifier -> authAccountIdentifierMapper.deleteById(identifier.getIdentifierId()));
			return;
		}

		AuthAccountIdentifier target = identifiers.stream()
			.filter(identifier -> normalizedValue.equals(identifier.getIdentifierValue()))
			.findFirst()
			.orElseGet(() -> identifiers.stream()
				.filter(identifier -> PRIMARY_FLAG_YES.equals(identifier.getPrimaryFlag()))
				.findFirst()
				.orElse(null));

		for (AuthAccountIdentifier identifier : identifiers) {
			if (target == null || !Objects.equals(identifier.getIdentifierId(), target.getIdentifierId())) {
				if (normalizedValue.equals(identifier.getIdentifierValue())) {
					authAccountIdentifierMapper.deleteById(identifier.getIdentifierId());
					continue;
				}
				if (PRIMARY_FLAG_YES.equals(identifier.getPrimaryFlag())) {
					identifier.setPrimaryFlag(PRIMARY_FLAG_NO);
				}
				identifier.setClientId(account.getClientId());
				identifier.setStatus(account.getStatus());
				identifier.setUpdateBy(account.getUpdateBy());
				identifier.setUpdateTime(LocalDateTime.now());
				authAccountIdentifierMapper.updateById(identifier);
			}
		}

		if (target == null) {
			target = new AuthAccountIdentifier();
			target.setAccountId(account.getAccountId());
			target.setCreateBy(account.getUpdateBy());
			target.setCreateTime(LocalDateTime.now());
			target.setIdentifierType(type);
		}

		target.setClientId(account.getClientId());
		target.setIdentifierValue(normalizedValue);
		target.setPrimaryFlag(PRIMARY_FLAG_YES);
		target.setStatus(account.getStatus());
		target.setVerifiedAt(verified ? LocalDateTime.now() : resolveIdentifierVerifiedAt(type, normalizedValue, account));
		target.setUpdateBy(account.getUpdateBy());
		target.setUpdateTime(LocalDateTime.now());

		if (target.getIdentifierId() == null) {
			authAccountIdentifierMapper.insert(target);
		}
		else {
			authAccountIdentifierMapper.updateById(target);
		}
	}

	private void refreshIdentifierStatus(AuthAccount account) {
		List<AuthAccountIdentifier> identifiers = authAccountIdentifierMapper.selectList(Wrappers
			.<AuthAccountIdentifier>lambdaQuery()
			.eq(AuthAccountIdentifier::getAccountId, account.getAccountId()));
		for (AuthAccountIdentifier identifier : identifiers) {
			if (Objects.equals(identifier.getStatus(), account.getStatus())
					&& Objects.equals(identifier.getClientId(), account.getClientId())) {
				continue;
			}
			identifier.setClientId(account.getClientId());
			identifier.setStatus(account.getStatus());
			identifier.setUpdateBy(account.getUpdateBy());
			identifier.setUpdateTime(LocalDateTime.now());
			authAccountIdentifierMapper.updateById(identifier);
		}
	}

	private String normalizeIdentifierType(String identifierType) {
		return identifierType.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeIdentifierValue(String identifierType, String identifierValue) {
		if (!StringUtils.hasText(identifierValue)) {
			return null;
		}
		String normalizedValue = identifierValue.trim();
		if (IDENTIFIER_EMAIL.equals(normalizeIdentifierType(identifierType))) {
			return normalizedValue.toLowerCase(Locale.ROOT);
		}
		return normalizedValue;
	}

	private LocalDateTime resolveIdentifierVerifiedAt(String identifierType, String identifierValue, AuthAccount account) {
		if (IDENTIFIER_USERNAME.equals(identifierType) && identifierValue.equals(account.getLoginName())) {
			return LocalDateTime.now();
		}
		if (IDENTIFIER_PHONE.equals(identifierType) && identifierValue.equals(account.getPhone())) {
			return null;
		}
		return null;
	}

	private void upsertPasswordCredential(AuthAccount account, String encodedPassword, String status, String updatedBy,
			String createdBy) {
		if (!StringUtils.hasText(encodedPassword)) {
			return;
		}
		AuthAccountCredential credential = getCredential(account.getAccountId(), CREDENTIAL_PASSWORD, CREDENTIAL_KEY_DEFAULT);
		if (credential == null) {
			credential = new AuthAccountCredential();
			credential.setAccountId(account.getAccountId());
			credential.setCredentialType(CREDENTIAL_PASSWORD);
			credential.setCredentialKey(CREDENTIAL_KEY_DEFAULT);
			credential.setCreateBy(StringUtils.hasText(updatedBy) ? updatedBy : createdBy);
			credential.setCreateTime(LocalDateTime.now());
		}
		credential.setSecretValue(encodedPassword);
		credential.setStatus(StringUtils.hasText(status) ? status : CommonConstants.STATUS_NORMAL);
		credential.setUpdateBy(updatedBy);
		credential.setUpdateTime(LocalDateTime.now());
		credential.setVerifiedAt(LocalDateTime.now());
		saveCredential(credential);
	}

	private void upsertOtpCredential(AuthAccount account, String phone, String status, String updatedBy, String createdBy) {
		AuthAccountCredential credential = getCredential(account.getAccountId(), CREDENTIAL_OTP, CREDENTIAL_KEY_DEFAULT);
		if (!StringUtils.hasText(phone)) {
			if (credential != null) {
				authAccountCredentialMapper.deleteById(credential.getCredentialId());
			}
			return;
		}
		if (credential == null) {
			credential = new AuthAccountCredential();
			credential.setAccountId(account.getAccountId());
			credential.setCredentialType(CREDENTIAL_OTP);
			credential.setCredentialKey(CREDENTIAL_KEY_DEFAULT);
			credential.setCreateBy(StringUtils.hasText(updatedBy) ? updatedBy : createdBy);
			credential.setCreateTime(LocalDateTime.now());
		}
		credential.setSecretValue(null);
		credential.setStatus(StringUtils.hasText(status) ? status : CommonConstants.STATUS_NORMAL);
		credential.setUpdateBy(updatedBy);
		credential.setUpdateTime(LocalDateTime.now());
		saveCredential(credential);
	}

	private AuthAccountCredential getCredential(Long accountId, String type, String key) {
		return authAccountCredentialMapper.selectOne(Wrappers.<AuthAccountCredential>lambdaQuery()
			.eq(AuthAccountCredential::getAccountId, accountId)
			.eq(AuthAccountCredential::getCredentialType, type)
			.eq(AuthAccountCredential::getCredentialKey, key), false);
	}

	private Optional<AuthAccount> resolveByIdentifier(String clientId, String identifierType, String identifierValue) {
		AuthAccountIdentifier identifier = authAccountIdentifierMapper.selectOne(Wrappers
			.<AuthAccountIdentifier>lambdaQuery()
			.eq(AuthAccountIdentifier::getClientId, clientId)
			.eq(AuthAccountIdentifier::getIdentifierType, identifierType)
			.eq(AuthAccountIdentifier::getIdentifierValue, identifierValue)
			.eq(AuthAccountIdentifier::getStatus, CommonConstants.STATUS_NORMAL), false);
		if (identifier == null || identifier.getAccountId() == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(authAccountMapper.selectById(identifier.getAccountId()));
	}

	private void saveCredential(AuthAccountCredential credential) {
		if (credential.getCredentialId() == null) {
			authAccountCredentialMapper.insert(credential);
		}
		else {
			authAccountCredentialMapper.updateById(credential);
		}
	}

	private void syncLegacyUserPassword(Long userId, String encodedPassword, String updatedBy) {
		if (userId == null || !StringUtils.hasText(encodedPassword)) {
			return;
		}
		SysUser user = new SysUser();
		user.setUserId(userId);
		user.setPassword(encodedPassword);
		user.setUpdateBy(updatedBy);
		user.setUpdateTime(LocalDateTime.now());
		sysUserMapper.updateById(user);
	}

	private String resolveAccountStatus(SysUser user) {
		return StringUtils.hasText(user.getLockFlag()) ? user.getLockFlag() : CommonConstants.STATUS_NORMAL;
	}

	private AuthAccountCredentialManageVO toManageView(AuthAccount account, List<AuthAccountCredential> credentials,
			List<AuthAccountIdentifier> identifiers) {
		List<AuthAccountCredential> accountCredentials = credentials.stream()
			.filter(credential -> account.getAccountId().equals(credential.getAccountId()))
			.toList();
		List<AuthAccountIdentifierManageVO> accountIdentifiers = identifiers.stream()
			.filter(identifier -> account.getAccountId().equals(identifier.getAccountId()))
			.map(this::toIdentifierView)
			.toList();
		AuthAccountCredentialManageVO view = new AuthAccountCredentialManageVO();
		view.setAccountId(account.getAccountId());
		view.setUserId(account.getUserId());
		view.setClientId(account.getClientId());
		view.setLoginName(account.getLoginName());
		view.setPhone(account.getPhone());
		view.setAccountStatus(account.getStatus());
		view.setPasswordStatus(resolveCredentialStatus(accountCredentials, CREDENTIAL_PASSWORD));
		view.setOtpStatus(resolveCredentialStatus(accountCredentials, CREDENTIAL_OTP));
		view.setPasskeyCount((int) accountCredentials.stream()
			.filter(credential -> "PASSKEY".equals(credential.getCredentialType()))
			.filter(credential -> CommonConstants.STATUS_NORMAL.equals(credential.getStatus()))
			.count());
		view.setLatestVerifiedAt(accountCredentials.stream()
			.map(AuthAccountCredential::getVerifiedAt)
			.filter(java.util.Objects::nonNull)
			.max(LocalDateTime::compareTo)
			.orElse(null));
		view.setIdentifiers(accountIdentifiers);
		return view;
	}

	private AuthAccountIdentifierManageVO toIdentifierView(AuthAccountIdentifier identifier) {
		AuthAccountIdentifierManageVO view = new AuthAccountIdentifierManageVO();
		view.setIdentifierId(identifier.getIdentifierId());
		view.setIdentifierType(identifier.getIdentifierType());
		view.setIdentifierValue(identifier.getIdentifierValue());
		view.setPrimaryFlag(identifier.getPrimaryFlag());
		view.setStatus(identifier.getStatus());
		view.setVerifiedAt(identifier.getVerifiedAt());
		return view;
	}

	private String resolveCredentialStatus(List<AuthAccountCredential> credentials, String credentialType) {
		return credentials.stream()
			.filter(credential -> credentialType.equals(credential.getCredentialType()))
			.map(AuthAccountCredential::getStatus)
			.findFirst()
			.orElse(null);
	}

}
