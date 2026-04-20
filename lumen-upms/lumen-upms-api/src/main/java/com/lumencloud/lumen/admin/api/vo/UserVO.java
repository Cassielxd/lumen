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

package com.lumencloud.lumen.admin.api.vo;

import com.lumencloud.lumen.admin.api.entity.SysDept;
import com.lumencloud.lumen.admin.api.entity.SysPost;
import com.lumencloud.lumen.admin.api.entity.SysRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User view object.
 */
@Data
@Schema(description = "前端用户展示对象")
public class UserVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主键")
	private Long userId;

	@Schema(description = "用户名")
	private String username;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	@Schema(description = "删除标记")
	private String delFlag;

	@Schema(description = "锁定标记")
	private String lockFlag;

	@Schema(description = "手机号")
	private String phone;

	@Schema(description = "头像")
	private String avatar;

	@Schema(description = "所属部门")
	private SysDept dept;

	@Schema(description = "拥有的角色列表")
	private List<SysRole> roleList;

	@Schema(description = "岗位列表")
	private List<SysPost> postList;

	@Schema(description = "昵称")
	private String nickname;

	@Schema(description = "姓名")
	private String name;

	@Schema(description = "邮箱")
	private String email;

}
