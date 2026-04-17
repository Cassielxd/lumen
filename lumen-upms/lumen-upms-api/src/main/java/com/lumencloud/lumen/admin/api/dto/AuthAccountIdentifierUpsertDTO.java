package com.lumencloud.lumen.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Platform identifier upsert request.
 */
@Data
@Schema(description = "Account identifier upsert request")
public class AuthAccountIdentifierUpsertDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Account ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long accountId;

	@Schema(description = "Identifier type", requiredMode = Schema.RequiredMode.REQUIRED, example = "EMAIL")
	private String identifierType;

	@Schema(description = "Identifier value", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin@example.com")
	private String identifierValue;

}
