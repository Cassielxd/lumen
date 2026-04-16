package com.lumencloud.lumen.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Passkey registration completion request.
 */
@Data
@Schema(description = "Passkey registration finish request")
public class PasskeyRegistrationFinishDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "clientDataJSON base64url")
	private String clientDataJSON;

	@Schema(description = "attestationObject base64url")
	private String attestationObject;

	@Schema(description = "Authenticator transports")
	private List<String> transports;

}
