package com.lumencloud.lumen.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Platform password reset request.
 */
@Data
@Schema(description = "Account password reset request")
public class AuthAccountPasswordResetDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Account ID")
	private Long accountId;

	@Schema(description = "New password")
	private String newPassword;

}
