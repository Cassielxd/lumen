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

package com.lumencloud.lumen.admin.api.dto;

import com.lumencloud.lumen.admin.api.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * User transfer object.
 *
 * @author lengleng
 * @date 2017/11/5
 */
@Data
@Schema(description = "System user transfer object")
@EqualsAndHashCode(callSuper = true)
public class UserDTO extends SysUser {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Role IDs")
	private List<Long> role;

	@Schema(description = "Department ID")
	private Long deptId;

	@Schema(description = "Post IDs")
	private List<Long> post;

	@Schema(description = "New password")
	private String newpassword1;

	@Schema(description = "Login client ID")
	private String clientId;

	@Schema(description = "Bound client IDs")
	private List<String> clientIds;

	@Schema(description = "Requested grant type")
	private String grantType;

}
