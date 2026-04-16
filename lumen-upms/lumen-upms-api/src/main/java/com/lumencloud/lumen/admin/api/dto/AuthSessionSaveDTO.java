package com.lumencloud.lumen.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Session upsert command.
 */
@Data
@Schema(description = "Session save request")
public class AuthSessionSaveDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

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

	@Schema(description = "Access token")
	private String accessToken;

	@Schema(description = "Refresh token")
	private String refreshToken;

	@Schema(description = "Access token expires at")
	private LocalDateTime accessTokenExpiresAt;

	@Schema(description = "Refresh token expires at")
	private LocalDateTime refreshTokenExpiresAt;

	@Schema(description = "IP address")
	private String ipAddress;

	@Schema(description = "User-Agent")
	private String userAgent;

}
