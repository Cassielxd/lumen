/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the lumencloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.lumencloud.lumen.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumencloud.lumen.admin.api.dto.RegisterUserDTO;
import com.lumencloud.lumen.admin.api.dto.UserDTO;
import com.lumencloud.lumen.admin.api.dto.UserInfo;
import com.lumencloud.lumen.admin.api.entity.AuthAccount;
import com.lumencloud.lumen.admin.api.entity.AuthAccountCredential;
import com.lumencloud.lumen.admin.api.entity.SysMenu;
import com.lumencloud.lumen.admin.api.entity.SysPost;
import com.lumencloud.lumen.admin.api.entity.SysRole;
import com.lumencloud.lumen.admin.api.entity.SysUser;
import com.lumencloud.lumen.admin.api.entity.SysUserPost;
import com.lumencloud.lumen.admin.api.entity.SysUserRole;
import com.lumencloud.lumen.admin.api.util.ParamResolver;
import com.lumencloud.lumen.admin.api.vo.UserVO;
import com.lumencloud.lumen.admin.mapper.SysUserMapper;
import com.lumencloud.lumen.admin.mapper.SysUserPostMapper;
import com.lumencloud.lumen.admin.mapper.SysUserRoleMapper;
import com.lumencloud.lumen.admin.service.AuthAccountService;
import com.lumencloud.lumen.admin.service.SysDeptService;
import com.lumencloud.lumen.admin.service.SysMenuService;
import com.lumencloud.lumen.admin.service.SysPostService;
import com.lumencloud.lumen.admin.service.SysRoleService;
import com.lumencloud.lumen.admin.service.SysUserService;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.exception.CheckedException;
import com.lumencloud.lumen.common.core.exception.ErrorCodes;
import com.lumencloud.lumen.common.core.util.MsgUtils;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.security.service.LumenUser;
import com.lumencloud.lumen.common.security.util.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * User profile service implementation.
 */
@Slf4j
@Service
@AllArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

	private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

	private final SysMenuService sysMenuService;

	private final SysRoleService sysRoleService;

	private final SysPostService sysPostService;

	private final SysDeptService sysDeptService;

	private final SysUserRoleMapper sysUserRoleMapper;

	private final SysUserPostMapper sysUserPostMapper;

	private final CacheManager cacheManager;

	private final AuthAccountService authAccountService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean saveUser(UserDTO userDto) {
		SysUser sysUser = new SysUser();
		BeanUtils.copyProperties(userDto, sysUser);
		sysUser.setDelFlag(CommonConstants.STATUS_NORMAL);
		sysUser.setCreateBy(userDto.getUsername());
		baseMapper.insert(sysUser);

		Optional.ofNullable(userDto.getPost()).ifPresent(posts -> posts.forEach(postId -> {
			SysUserPost userPost = new SysUserPost();
			userPost.setUserId(sysUser.getUserId());
			userPost.setPostId(postId);
			sysUserPostMapper.insert(userPost);
		}));

		if (CollUtil.isEmpty(userDto.getRole())) {
			String defaultRole = ParamResolver.getStr("USER_DEFAULT_ROLE");
			SysRole sysRole = sysRoleService
				.getOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, defaultRole));
			userDto.setRole(Collections.singletonList(sysRole.getRoleId()));
		}

		userDto.getRole().forEach(roleId -> {
			SysUserRole userRole = new SysUserRole();
			userRole.setUserId(sysUser.getUserId());
			userRole.setRoleId(roleId);
			sysUserRoleMapper.insert(userRole);
		});

		List<String> clientIds = resolveClientIds(userDto, Collections.singletonList("lumen"));
		authAccountService.ensureUserAccounts(sysUser, clientIds);
		if (StrUtil.isNotBlank(userDto.getPassword())) {
			authAccountService.syncPasswordCredentialForClients(sysUser.getUserId(), clientIds,
					ENCODER.encode(userDto.getPassword()), sysUser.getCreateBy());
		}
		return Boolean.TRUE;
	}

	@Override
	public R<UserInfo> getUserInfo(UserDTO query) {
		AuthAccount authAccount = null;
		AuthAccountCredential passwordCredential = null;
		UserDTO userQuery = query;
		if (StrUtil.isNotBlank(query.getClientId())) {
			authAccount = authAccountService.resolveAccount(query.getClientId(), query.getUsername(), query.getPhone())
				.orElse(null);
			if (authAccount == null || !StrUtil.equals(CommonConstants.STATUS_NORMAL, authAccount.getStatus())) {
				return R.failed(MsgUtils.getMessage(ErrorCodes.SYS_USER_USERINFO_EMPTY, resolveIdentifier(query)));
			}
			String requiredCredentialType = resolveRequiredCredentialType(query.getGrantType());
			if (StrUtil.isNotBlank(requiredCredentialType)) {
				AuthAccountCredential requiredCredential = authAccountService
					.getCredential(authAccount.getAccountId(), requiredCredentialType)
					.orElse(null);
				if (requiredCredential == null
						|| !StrUtil.equals(CommonConstants.STATUS_NORMAL, requiredCredential.getStatus())) {
					return R.failed(MsgUtils.getMessage(ErrorCodes.SYS_USER_USERINFO_EMPTY, resolveIdentifier(query)));
				}
			}
			passwordCredential = authAccountService.getCredential(authAccount.getAccountId(), "PASSWORD").orElse(null);
			userQuery = new UserDTO();
			userQuery.setUserId(authAccount.getUserId());
		}

		UserVO dbUser = baseMapper.getUser(userQuery);
		if (dbUser == null) {
			return R.failed(MsgUtils.getMessage(ErrorCodes.SYS_USER_USERINFO_EMPTY, query.getUsername()));
		}

		UserInfo userInfo = new UserInfo();
		BeanUtils.copyProperties(dbUser, userInfo);
		List<String> permissions = dbUser.getRoleList()
			.stream()
			.map(SysRole::getRoleId)
			.flatMap(roleId -> sysMenuService.findMenuByRoleId(roleId).stream())
			.filter(menu -> StrUtil.isNotEmpty(menu.getPermission()))
			.map(SysMenu::getPermission)
			.toList();
		userInfo.setPermissions(permissions);
		if (authAccount != null) {
			userInfo.setAccountId(authAccount.getAccountId());
			userInfo.setAccountClientId(authAccount.getClientId());
			userInfo.setLockFlag(authAccount.getStatus());
			userInfo.setPassword(passwordCredential == null ? null : passwordCredential.getSecretValue());
		}
		return R.ok(userInfo);
	}

	@Override
	public IPage getUsersWithRolePage(Page page, UserDTO userDTO) {
		return baseMapper.getUsersPage(page, userDTO);
	}

	@Override
	public UserVO getUserById(Long id) {
		UserDTO query = new UserDTO();
		query.setUserId(id);
		return baseMapper.getUser(query);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean removeUserByIds(Long[] ids) {
		List<Long> idList = CollUtil.toList(ids);
		baseMapper.selectByIds(idList).forEach(this::evictUserDetailsCache);
		sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getUserId, idList));
		authAccountService.removeByUserIds(idList);
		this.removeBatchByIds(idList);
		return Boolean.TRUE;
	}

	@Override
	@CacheEvict(value = CacheConstants.USER_DETAILS, key = "#userDto.username")
	public R<Boolean> updateUserInfo(UserDTO userDto) {
		SysUser sysUser = new SysUser();
		sysUser.setPhone(userDto.getPhone());
		sysUser.setUserId(SecurityUtils.getUser().getId());
		sysUser.setAvatar(userDto.getAvatar());
		sysUser.setNickname(userDto.getNickname());
		sysUser.setName(userDto.getName());
		sysUser.setEmail(userDto.getEmail());
		boolean updated = this.updateById(sysUser);
		if (updated) {
			SysUser latestUser = baseMapper.selectById(sysUser.getUserId());
			authAccountService.syncUserProfile(latestUser);
			authAccountService.syncOtpCredential(latestUser.getUserId(), latestUser.getPhone(),
					resolveAccountStatus(latestUser), latestUser.getUpdateBy());
			evictUserDetailsCache(latestUser);
		}
		return R.ok(updated);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CacheConstants.USER_DETAILS, key = "#userDto.username")
	public Boolean updateUser(UserDTO userDto) {
		if (StrUtil.isNotBlank(userDto.getPassword())) {
			throw new CheckedException("用户资料更新不再支持修改密码，请使用账号密码重置接口");
		}

		SysUser sysUser = new SysUser();
		BeanUtils.copyProperties(userDto, sysUser);
		sysUser.setUpdateTime(LocalDateTime.now());
		this.updateById(sysUser);

		if (Objects.nonNull(userDto.getRole())) {
			sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userDto.getUserId()));
			userDto.getRole().forEach(roleId -> {
				SysUserRole userRole = new SysUserRole();
				userRole.setUserId(sysUser.getUserId());
				userRole.setRoleId(roleId);
				sysUserRoleMapper.insert(userRole);
			});
		}

		if (Objects.nonNull(userDto.getPost())) {
			sysUserPostMapper.delete(Wrappers.<SysUserPost>lambdaQuery().eq(SysUserPost::getUserId, userDto.getUserId()));
			userDto.getPost().forEach(postId -> {
				SysUserPost userPost = new SysUserPost();
				userPost.setUserId(sysUser.getUserId());
				userPost.setPostId(postId);
				sysUserPostMapper.insert(userPost);
			});
		}

		SysUser latestUser = baseMapper.selectById(userDto.getUserId());
		authAccountService.syncUserProfile(latestUser);
		authAccountService.syncOtpCredential(latestUser.getUserId(), latestUser.getPhone(),
				resolveAccountStatus(latestUser), latestUser.getUpdateBy());
		if (userDto.getClientIds() != null) {
			authAccountService.ensureUserAccounts(latestUser, userDto.getClientIds());
		}
		evictUserDetailsCache(latestUser);
		return Boolean.TRUE;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> registerUser(RegisterUserDTO userDto) {
		SysUser sysUser = this.getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, userDto.getUsername()));
		if (sysUser != null) {
			String message = MsgUtils.getMessage(ErrorCodes.SYS_USER_USERNAME_EXISTING, userDto.getUsername());
			return R.failed(message);
		}

		UserDTO user = new UserDTO();
		BeanUtils.copyProperties(userDto, user);
		user.setClientIds(resolveClientIds(user, Collections.singletonList("app")));
		return R.ok(saveUser(user));
	}

	@Override
	@CacheEvict(value = CacheConstants.USER_DETAILS, key = "#username")
	public R<Boolean> lockUser(String username) {
		SysUser sysUser = baseMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
		if (Objects.nonNull(sysUser)) {
			sysUser.setLockFlag(CommonConstants.STATUS_LOCK);
			baseMapper.updateById(sysUser);
			authAccountService.syncUserProfile(sysUser);
			authAccountService.syncOtpCredential(sysUser.getUserId(), sysUser.getPhone(), resolveAccountStatus(sysUser),
					sysUser.getUpdateBy());
			evictUserDetailsCache(sysUser);
		}
		return R.ok();
	}

	@Override
	@CacheEvict(value = CacheConstants.USER_DETAILS, key = "#userDto.username")
	public R changePassword(UserDTO userDto) {
		LumenUser currentUser = SecurityUtils.getUser();
		if (currentUser == null || currentUser.getAccountId() == null) {
			return R.failed("当前登录账号缺少认证上下文，请重新登录");
		}
		SysUser sysUser = baseMapper.selectById(currentUser.getId());
		if (Objects.isNull(sysUser)) {
			return R.failed("用户不存在");
		}
		if (StrUtil.isEmpty(userDto.getPassword())) {
			return R.failed("原密码不能为空");
		}

		String currentPasswordHash = resolveCurrentPasswordHash(sysUser, currentUser);
		if (StrUtil.isBlank(currentPasswordHash) || !ENCODER.matches(userDto.getPassword(), currentPasswordHash)) {
			log.info("原密码错误，修改个人信息失败:{}", userDto.getUsername());
			return R.failed(MsgUtils.getMessage(ErrorCodes.SYS_USER_UPDATE_PASSWORDERROR));
		}

		if (StrUtil.isEmpty(userDto.getNewpassword1())) {
			return R.failed("新密码不能为空");
		}
		String encodedPassword = ENCODER.encode(userDto.getNewpassword1());
		String operator = StrUtil.isNotBlank(currentUser.getUsername()) ? currentUser.getUsername() : sysUser.getUsername();
		authAccountService.updatePasswordCredential(currentUser.getAccountId(), encodedPassword, operator);
		evictUserDetailsCache(sysUser);
		return R.ok();
	}

	@Override
	public R checkPassword(String password) {
		LumenUser currentUser = SecurityUtils.getUser();
		if (currentUser == null || currentUser.getAccountId() == null) {
			return R.failed("当前登录账号缺少认证上下文，请重新登录");
		}
		SysUser sysUser = baseMapper.selectById(currentUser.getId());
		String currentPasswordHash = resolveCurrentPasswordHash(sysUser, currentUser);
		if (StrUtil.isBlank(currentPasswordHash) || !ENCODER.matches(password, currentPasswordHash)) {
			log.info("原密码错误");
			return R.failed("密码输入错误");
		}
		return R.ok();
	}

	private List<String> resolveClientIds(UserDTO userDto, List<String> defaultClientIds) {
		return CollUtil.isEmpty(userDto.getClientIds()) ? defaultClientIds : userDto.getClientIds();
	}

	private String resolveIdentifier(UserDTO query) {
		if (StrUtil.isNotBlank(query.getUsername())) {
			return query.getUsername();
		}
		if (StrUtil.isNotBlank(query.getPhone())) {
			return query.getPhone();
		}
		return String.valueOf(query.getUserId());
	}

	private String resolveRequiredCredentialType(String grantType) {
		if (StrUtil.equalsIgnoreCase(SecurityConstants.PASSWORD, grantType)) {
			return "PASSWORD";
		}
		if (StrUtil.equalsAnyIgnoreCase(grantType, SecurityConstants.OTP, SecurityConstants.MOBILE)) {
			return "OTP";
		}
		if (StrUtil.equalsIgnoreCase(SecurityConstants.PASSKEY, grantType)) {
			return "PASSKEY";
		}
		return null;
	}

	private String resolveAccountStatus(SysUser user) {
		return StrUtil.isNotBlank(user.getLockFlag()) ? user.getLockFlag() : CommonConstants.STATUS_NORMAL;
	}

	private String resolveCurrentPasswordHash(SysUser sysUser, LumenUser currentUser) {
		if (currentUser != null && currentUser.getAccountId() != null) {
			AuthAccountCredential credential = authAccountService.getCredential(currentUser.getAccountId(), "PASSWORD")
				.orElse(null);
			if (credential != null && StrUtil.isNotBlank(credential.getSecretValue())) {
				return credential.getSecretValue();
			}
		}
		return null;
	}

	private void evictUserDetailsCache(SysUser user) {
		Cache cache = cacheManager.getCache(CacheConstants.USER_DETAILS);
		if (cache == null || user == null) {
			return;
		}
		cache.evictIfPresent(user.getUsername());
		if (StrUtil.isNotBlank(user.getPhone())) {
			cache.evictIfPresent(user.getPhone());
		}
		if (StrUtil.isNotBlank(user.getEmail())) {
			cache.evictIfPresent(user.getEmail());
			cache.evictIfPresent(user.getEmail().toLowerCase(Locale.ROOT));
		}
		authAccountService.listByUserId(user.getUserId()).forEach(account -> authAccountService.listIdentifiers(account.getAccountId())
			.forEach(identifier -> evictCacheKeyVariants(cache, account.getClientId(), identifier.getIdentifierValue())));
	}

	private void evictCacheKeyVariants(Cache cache, String clientId, String principal) {
		if (StrUtil.isBlank(principal)) {
			return;
		}
		cache.evictIfPresent(principal);
		cache.evictIfPresent(clientId + "::" + principal);
		cache.evictIfPresent(clientId + "::" + SecurityConstants.PASSWORD + "::" + principal);
		cache.evictIfPresent(clientId + "::" + SecurityConstants.OTP + "::" + principal);
		cache.evictIfPresent(clientId + "::" + SecurityConstants.MOBILE + "::" + principal);
		cache.evictIfPresent(clientId + "::" + SecurityConstants.PASSKEY + "::" + principal);
		if (principal.contains("@")) {
			String normalizedPrincipal = principal.toLowerCase(Locale.ROOT);
			cache.evictIfPresent(normalizedPrincipal);
			cache.evictIfPresent(clientId + "::" + normalizedPrincipal);
			cache.evictIfPresent(clientId + "::" + SecurityConstants.PASSWORD + "::" + normalizedPrincipal);
			cache.evictIfPresent(clientId + "::" + SecurityConstants.OTP + "::" + normalizedPrincipal);
			cache.evictIfPresent(clientId + "::" + SecurityConstants.MOBILE + "::" + normalizedPrincipal);
			cache.evictIfPresent(clientId + "::" + SecurityConstants.PASSKEY + "::" + normalizedPrincipal);
		}
	}

}
