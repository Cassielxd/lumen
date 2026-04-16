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

package com.lumencloud.lumen.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumencloud.lumen.admin.api.dto.RegisterUserDTO;
import com.lumencloud.lumen.admin.api.dto.UserDTO;
import com.lumencloud.lumen.admin.api.dto.UserInfo;
import com.lumencloud.lumen.admin.api.entity.SysUser;
import com.lumencloud.lumen.admin.api.vo.UserVO;
import com.lumencloud.lumen.common.core.util.R;

public interface SysUserService extends IService<SysUser> {

	R<UserInfo> getUserInfo(UserDTO query);

	IPage getUsersWithRolePage(Page page, UserDTO userDTO);

	Boolean removeUserByIds(Long[] ids);

	R<Boolean> updateUserInfo(UserDTO userDto);

	Boolean updateUser(UserDTO userDto);

	UserVO getUserById(Long id);

	Boolean saveUser(UserDTO userDto);

	R<Boolean> registerUser(RegisterUserDTO userDto);

	R<Boolean> lockUser(String username);

	R changePassword(UserDTO userDto);

	R checkPassword(String password);

}
