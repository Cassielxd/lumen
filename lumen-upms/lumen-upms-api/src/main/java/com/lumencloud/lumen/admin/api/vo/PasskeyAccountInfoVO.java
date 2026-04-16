package com.lumencloud.lumen.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Internal passkey account information.
 */
@Data
@Schema(description = "Passkey account info")
public class PasskeyAccountInfoVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Account ID")
	private Long accountId;

	@Schema(description = "User ID")
	private Long userId;

	@Schema(description = "Client ID")
	private String clientId;

	@Schema(description = "Username")
	private String username;

	@Schema(description = "Passkey credentials")
	private List<PasskeyCredentialVO> credentials;

}
