package com.lumencloud.lumen.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Account identifier governance view.
 */
@Data
@Schema(description = "Account identifier governance view")
public class AuthAccountIdentifierManageVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Identifier ID")
	private Long identifierId;

	@Schema(description = "Identifier type")
	private String identifierType;

	@Schema(description = "Identifier value")
	private String identifierValue;

	@Schema(description = "Primary flag")
	private String primaryFlag;

	@Schema(description = "Status")
	private String status;

	@Schema(description = "Verified at")
	private LocalDateTime verifiedAt;

}
