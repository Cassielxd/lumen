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

import com.lumencloud.lumen.admin.api.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security user info payload.
 */
@Data
@Schema(description = "Spring Security user info")
@EqualsAndHashCode(callSuper = true)
public class UserInfo extends UserVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Encoded password credential")
	private String password;

	@Schema(description = "Permission codes")
	private List<String> permissions = new ArrayList<>();

	@Schema(description = "Account ID")
	private Long accountId;

	@Schema(description = "Account client ID")
	private String accountClientId;

}
