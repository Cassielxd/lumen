package com.lumencloud.lumen.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Internal passkey account lookup request.
 */
@Data
@Schema(description = "Passkey account lookup request")
public class PasskeyAccountLookupDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Client ID")
	private String clientId;

	@Schema(description = "Username")
	private String username;

}
