package com.lumencloud.lumen.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Session logout command.
 */
@Data
@Schema(description = "Session logout request")
public class AuthSessionLogoutDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Access token")
	private String accessToken;

	@Schema(description = "Refresh token")
	private String refreshToken;

}
