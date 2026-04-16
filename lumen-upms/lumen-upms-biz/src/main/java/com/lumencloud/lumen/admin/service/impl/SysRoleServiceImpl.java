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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumencloud.lumen.admin.api.entity.SysRole;
import com.lumencloud.lumen.admin.api.entity.SysRoleMenu;
import com.lumencloud.lumen.admin.api.vo.RoleVO;
import com.lumencloud.lumen.admin.mapper.SysRoleMapper;
import com.lumencloud.lumen.admin.service.SysRoleMenuService;
import com.lumencloud.lumen.admin.service.SysRoleService;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.exception.ErrorCodes;
import com.lumencloud.lumen.common.core.util.MsgUtils;
import com.lumencloud.lumen.common.core.util.R;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;

/**
 * 系统角色服务实现类
 *
 * @author lengleng
 * @since 2017-10-29
 */
@Service
@AllArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

	private SysRoleMenuService roleMenuService;

	/**
	 * 通过用户ID查询角色信息
	 * @param userId 用户ID
	 * @return 角色信息列表
	 */
	@Override
	public List listRolesByUserId(Long userId) {
		return baseMapper.listRolesByUserId(userId);
	}

	/**
	 * 根据角色ID查询角色列表
	 * @param roleIdList 角色ID列表
	 * @param key 缓存key
	 * @return 角色列表
	 */
	@Override
	@Cacheable(value = CacheConstants.ROLE_DETAILS, key = "#key", unless = "#result.isEmpty()")
	public List<SysRole> listRolesByRoleIds(List<Long> roleIdList, String key) {
		return baseMapper.selectByIds(roleIdList);
	}

	/**
	 * 通过角色ID删除角色并清空角色菜单缓存
	 * @param ids 角色ID数组
	 * @return 删除是否成功
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean removeRoleByIds(Long[] ids) {
		roleMenuService
			.remove(Wrappers.<SysRoleMenu>update().lambda().in(SysRoleMenu::getRoleId, CollUtil.toList(ids)));
		return this.removeBatchByIds(CollUtil.toList(ids));
	}

	/**
	 * 更新角色菜单列表
	 * @param roleVo 包含角色ID和菜单ID列表的角色对象
	 * @return 更新是否成功
	 */
	@Override
	public Boolean updateRoleMenus(RoleVO roleVo) {
		return roleMenuService.saveRoleMenus(roleVo.getRoleId(), roleVo.getMenuIds());
	}

}
