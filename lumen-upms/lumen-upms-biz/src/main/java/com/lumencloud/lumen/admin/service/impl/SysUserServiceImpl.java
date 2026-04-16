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
import com.lumencloud.lumen.admin.api.entity.*;
import com.lumencloud.lumen.admin.api.util.ParamResolver;
import com.lumencloud.lumen.admin.api.vo.UserVO;
import com.lumencloud.lumen.admin.mapper.SysUserMapper;
import com.lumencloud.lumen.admin.mapper.SysUserPostMapper;
import com.lumencloud.lumen.admin.mapper.SysUserRoleMapper;
import com.lumencloud.lumen.admin.service.*;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.exception.ErrorCodes;
import com.lumencloud.lumen.common.core.util.MsgUtils;
import com.lumencloud.lumen.common.core.util.R;
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
import java.util.*;

/**
 * 系统用户服务实现类
 *
 * @author lengleng
 * @date 2025/05/30
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

	/**
	 * 保存用户信息
	 * @param userDto 用户数据传输对象
	 * @return 操作是否成功
	 * @throws Exception 事务回滚时抛出异常
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean saveUser(UserDTO userDto) {
		SysUser sysUser = new SysUser();
		BeanUtils.copyProperties(userDto, sysUser);
		sysUser.setDelFlag(CommonConstants.STATUS_NORMAL);
		sysUser.setCreateBy(userDto.getUsername());
		sysUser.setPassword(ENCODER.encode(userDto.getPassword()));
		baseMapper.insert(sysUser);
		// 保存用户岗位信息
		Optional.ofNullable(userDto.getPost()).ifPresent(posts -> posts.forEach(postId -> {
			SysUserPost userPost = new SysUserPost();
			userPost.setUserId(sysUser.getUserId());
			userPost.setPostId(postId);
			sysUserPostMapper.insert(userPost);
		}));

		// 如果角色为空，赋默认角色
		if (CollUtil.isEmpty(userDto.getRole())) {
			// 获取默认角色编码
			String defaultRole = ParamResolver.getStr("USER_DEFAULT_ROLE");
			// 默认角色
			SysRole sysRole = sysRoleService
				.getOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, defaultRole));
			userDto.setRole(Collections.singletonList(sysRole.getRoleId()));
		}

		// 插入用户角色关系表
		userDto.getRole().forEach(roleId -> {
			SysUserRole userRole = new SysUserRole();
			userRole.setUserId(sysUser.getUserId());
			userRole.setRoleId(roleId);
			sysUserRoleMapper.insert(userRole);
		});
		authAccountService.ensureUserAccounts(sysUser, resolveClientIds(userDto, Collections.singletonList("lumen")));
		return Boolean.TRUE;
	}

	/**
	 * 查询用户全部信息，包括角色和权限
	 * @param query 用户查询条件
	 * @return 包含用户角色和权限的用户信息对象
	 */
	@Override
	public R<UserInfo> getUserInfo(UserDTO query) {
		AuthAccount authAccount = null;
		AuthAccountCredential passwordCredential = null;
		UserDTO userQuery = query;
		if (StrUtil.isNotBlank(query.getClientId())) {
			authAccount = authAccountService.resolveAccount(query.getClientId(), query.getUsername(), query.getPhone())
				.orElse(null);
			if (authAccount == null) {
				return R.failed(MsgUtils.getMessage(ErrorCodes.SYS_USER_USERINFO_EMPTY, resolveIdentifier(query)));
			}
			if (!StrUtil.equals(CommonConstants.STATUS_NORMAL, authAccount.getStatus())) {
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
		// 设置权限列表（menu.permission）
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

	/**
	 * 分页查询用户信息（包含角色信息）
	 * @param page 分页对象
	 * @param userDTO 查询参数
	 * @return 包含用户和角色信息的分页结果
	 */
	@Override
	public IPage getUsersWithRolePage(Page page, UserDTO userDTO) {
		return baseMapper.getUsersPage(page, userDTO);
	}

	/**
	 * 通过ID查询用户信息
	 * @param id 用户ID
	 * @return 用户信息VO对象
	 */
	@Override
	public UserVO getUserById(Long id) {
		UserDTO query = new UserDTO();
		query.setUserId(id);
		return baseMapper.getUser(query);
	}

	/**
	 * 根据用户ID列表删除用户及相关缓存
	 * @param ids 用户ID数组
	 * @return 删除成功返回true
	 * @throws Exception 事务回滚时抛出异常
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean removeUserByIds(Long[] ids) {
		List<Long> idList = CollUtil.toList(ids);
		// 删除 spring cache
		Cache cache = cacheManager.getCache(CacheConstants.USER_DETAILS);
		baseMapper.selectByIds(idList).forEach(user -> cache.evictIfPresent(user.getUsername()));

		sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getUserId, idList));
		authAccountService.removeByUserIds(idList);
		this.removeBatchByIds(idList);
		return Boolean.TRUE;
	}

	/**
	 * 更新用户信息
	 * @param userDto 用户数据传输对象
	 * @return 操作结果，包含更新是否成功
	 */
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
		return R.ok(this.updateById(sysUser));
	}

	/**
	 * 更新用户信息
	 * @param userDto 用户数据传输对象，包含需要更新的用户信息
	 * @return 更新成功返回true
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CacheConstants.USER_DETAILS, key = "#userDto.username")
	public Boolean updateUser(UserDTO userDto) {
		// 更新用户表信息
		SysUser sysUser = new SysUser();
		BeanUtils.copyProperties(userDto, sysUser);
		sysUser.setUpdateTime(LocalDateTime.now());
		if (StrUtil.isNotBlank(userDto.getPassword())) {
			sysUser.setPassword(ENCODER.encode(userDto.getPassword()));
		}
		this.updateById(sysUser);

		// 更新用户角色表
		if (Objects.nonNull(userDto.getRole())) {
			// 删除用户角色关系
			sysUserRoleMapper
				.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userDto.getUserId()));
			userDto.getRole().forEach(roleId -> {
				SysUserRole userRole = new SysUserRole();
				userRole.setUserId(sysUser.getUserId());
				userRole.setRoleId(roleId);
				sysUserRoleMapper.insert(userRole);
			});
		}

		if (Objects.nonNull(userDto.getPost())) {
			// 删除用户岗位关系
			sysUserPostMapper
				.delete(Wrappers.<SysUserPost>lambdaQuery().eq(SysUserPost::getUserId, userDto.getUserId()));
			userDto.getPost().forEach(postId -> {
				SysUserPost userPost = new SysUserPost();
				userPost.setUserId(sysUser.getUserId());
				userPost.setPostId(postId);
				sysUserPostMapper.insert(userPost);
			});
		}
		SysUser latestUser = baseMapper.selectById(userDto.getUserId());
		authAccountService.syncUserIdentity(latestUser);
		if (userDto.getClientIds() != null) {
			authAccountService.ensureUserAccounts(latestUser, userDto.getClientIds());
		}
		evictUserDetailsCache(latestUser);
		return Boolean.TRUE;
	}

	/**
	 * 注册用户并赋予默认角色
	 * @param userDto 用户注册信息DTO
	 * @return 注册结果，包含成功或失败状态
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> registerUser(RegisterUserDTO userDto) {
		// 判断用户名是否存在
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

	/**
	 * 锁定用户
	 * @param username 用户名
	 * @return 操作结果，包含是否成功的信息
	 */
	@Override
	@CacheEvict(value = CacheConstants.USER_DETAILS, key = "#username")
	public R<Boolean> lockUser(String username) {
		SysUser sysUser = baseMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));

		if (Objects.nonNull(sysUser)) {
			sysUser.setLockFlag(CommonConstants.STATUS_LOCK);
			baseMapper.updateById(sysUser);
			authAccountService.syncUserIdentity(sysUser);
			evictUserDetailsCache(sysUser);
		}
		return R.ok();
	}

	/**
	 * 修改用户密码
	 * @param userDto 用户信息传输对象，包含用户名、原密码和新密码
	 * @return 操作结果，成功返回R.ok()，失败返回错误信息
	 * @CacheEvict 清除用户详情缓存
	 */
	@Override
	@CacheEvict(value = CacheConstants.USER_DETAILS, key = "#userDto.username")
	public R changePassword(UserDTO userDto) {
		SysUser sysUser = baseMapper.selectById(SecurityUtils.getUser().getId());
		if (Objects.isNull(sysUser)) {
			return R.failed("用户不存在");
		}

		if (StrUtil.isEmpty(userDto.getPassword())) {
			return R.failed("原密码不能为空");
		}

		if (!ENCODER.matches(userDto.getPassword(), sysUser.getPassword())) {
			log.info("原密码错误，修改个人信息失败:{}", userDto.getUsername());
			return R.failed(MsgUtils.getMessage(ErrorCodes.SYS_USER_UPDATE_PASSWORDERROR));
		}

		if (StrUtil.isEmpty(userDto.getNewpassword1())) {
			return R.failed("新密码不能为空");
		}
		String password = ENCODER.encode(userDto.getNewpassword1());

		this.update(Wrappers.<SysUser>lambdaUpdate()
			.set(SysUser::getPassword, password)
			.eq(SysUser::getUserId, sysUser.getUserId()));
		sysUser.setPassword(password);
		authAccountService.syncUserIdentity(sysUser);
		evictUserDetailsCache(sysUser);
		return R.ok();
	}

	/**
	 * 校验用户密码是否正确
	 * @param password 待校验的密码
	 * @return 校验结果，成功返回R.ok()，失败返回R.failed()
	 */
	@Override
	public R checkPassword(String password) {
		SysUser sysUser = baseMapper.selectById(SecurityUtils.getUser().getId());

		if (!ENCODER.matches(password, sysUser.getPassword())) {
			log.info("原密码错误");
			return R.failed("密码输入错误");
		}
		else {
			return R.ok();
		}
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

	private void evictUserDetailsCache(SysUser user) {
		Cache cache = cacheManager.getCache(CacheConstants.USER_DETAILS);
		if (cache == null || user == null) {
			return;
		}
		cache.evictIfPresent(user.getUsername());
		if (StrUtil.isNotBlank(user.getPhone())) {
			cache.evictIfPresent(user.getPhone());
		}
		authAccountService.listByUserId(user.getUserId()).forEach(account -> {
			if (StrUtil.isNotBlank(account.getLoginName())) {
				evictCacheKeyVariants(cache, account.getClientId(), account.getLoginName());
			}
			if (StrUtil.isNotBlank(account.getPhone())) {
				evictCacheKeyVariants(cache, account.getClientId(), account.getPhone());
			}
		});
	}

	private void evictCacheKeyVariants(Cache cache, String clientId, String principal) {
		cache.evictIfPresent(clientId + "::" + principal);
		cache.evictIfPresent(clientId + "::" + SecurityConstants.PASSWORD + "::" + principal);
		cache.evictIfPresent(clientId + "::" + SecurityConstants.OTP + "::" + principal);
		cache.evictIfPresent(clientId + "::" + SecurityConstants.MOBILE + "::" + principal);
		cache.evictIfPresent(clientId + "::" + SecurityConstants.PASSKEY + "::" + principal);
	}

}
