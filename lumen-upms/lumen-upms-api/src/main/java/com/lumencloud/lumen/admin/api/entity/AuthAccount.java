package com.lumencloud.lumen.admin.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lumencloud.lumen.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Client-bound authentication account.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Authentication account")
public class AuthAccount extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@TableId(value = "account_id", type = IdType.ASSIGN_ID)
	@Schema(description = "Account ID")
	private Long accountId;

	@Schema(description = "User ID")
	private Long userId;

	@Schema(description = "Client ID")
	private String clientId;

	@Schema(description = "Login name")
	private String loginName;

	@Schema(description = "Phone")
	private String phone;

	@Schema(description = "Status")
	private String status;

}
