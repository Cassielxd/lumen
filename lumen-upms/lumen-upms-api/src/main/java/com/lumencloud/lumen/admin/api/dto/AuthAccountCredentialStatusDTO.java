package com.lumencloud.lumen.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Account credential status request.
 */
@Data
@Schema(description = "Account credential status request")
public class AuthAccountCredentialStatusDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Account ID")
	private Long accountId;

	@Schema(description = "Credential status")
	private String status;

}
