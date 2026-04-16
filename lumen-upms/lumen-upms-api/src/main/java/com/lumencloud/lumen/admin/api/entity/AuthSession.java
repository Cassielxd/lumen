package com.lumencloud.lumen.admin.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lumencloud.lumen.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Authentication session record.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Authentication session")
public class AuthSession extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@TableId(value = "session_id", type = IdType.ASSIGN_ID)
	@Schema(description = "Session ID")
	private Long sessionId;

	@Schema(description = "Session key")
	private String sid;

	@Schema(description = "Account ID")
	private Long accountId;

	@Schema(description = "User ID")
	private Long userId;

	@Schema(description = "Client ID")
	private String clientId;

	@Schema(description = "Principal name")
	private String principalName;

	@Schema(description = "Grant type")
	private String grantType;

	@Schema(description = "Access token hash")
	private String accessTokenHash;

	@Schema(description = "Refresh token hash")
	private String refreshTokenHash;

	@Schema(description = "Access token expires at")
	private LocalDateTime accessTokenExpiresAt;

	@Schema(description = "Refresh token expires at")
	private LocalDateTime refreshTokenExpiresAt;

	@Schema(description = "IP address")
	private String ipAddress;

	@Schema(description = "User agent")
	private String userAgent;

	@Schema(description = "Last active time")
	private LocalDateTime lastActiveTime;

	@Schema(description = "Logout time")
	private LocalDateTime logoutTime;

	@Schema(description = "Status")
	private String status;

}
