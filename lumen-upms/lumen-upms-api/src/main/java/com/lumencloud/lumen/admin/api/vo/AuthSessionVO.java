package com.lumencloud.lumen.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Session view for current account management.
 */
@Data
@Schema(description = "Authentication session view")
public class AuthSessionVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Session key")
	private String sid;

	@Schema(description = "Client ID")
	private String clientId;

	@Schema(description = "Principal name")
	private String principalName;

	@Schema(description = "Grant type")
	private String grantType;

	@Schema(description = "IP address")
	private String ipAddress;

	@Schema(description = "User agent")
	private String userAgent;

	@Schema(description = "Access token expires at")
	private LocalDateTime accessTokenExpiresAt;

	@Schema(description = "Refresh token expires at")
	private LocalDateTime refreshTokenExpiresAt;

	@Schema(description = "Last active time")
	private LocalDateTime lastActiveTime;

	@Schema(description = "Logout time")
	private LocalDateTime logoutTime;

	@Schema(description = "Status")
	private String status;

	@Schema(description = "Whether this is the current session")
	private Boolean current;

}
