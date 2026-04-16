package com.lumencloud.lumen.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Internal passkey signature counter update request.
 */
@Data
@Schema(description = "Passkey signature counter update request")
public class PasskeyCredentialCounterUpdateDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Account ID")
	private Long accountId;

	@Schema(description = "Credential key")
	private String credentialKey;

	@Schema(description = "Signature counter")
	private Long signCount;

}
