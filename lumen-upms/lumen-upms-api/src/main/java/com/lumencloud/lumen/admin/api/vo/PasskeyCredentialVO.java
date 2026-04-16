package com.lumencloud.lumen.admin.api.vo;

import com.lumencloud.lumen.admin.api.dto.PasskeyCredentialPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Passkey credential view.
 */
@Data
@Schema(description = "Passkey credential")
public class PasskeyCredentialVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Credential key")
	private String credentialKey;

	@Schema(description = "Credential status")
	private String status;

	@Schema(description = "Verified at")
	private LocalDateTime verifiedAt;

	@Schema(description = "Created at")
	private LocalDateTime createTime;

	@Schema(description = "Credential payload")
	private PasskeyCredentialPayload payload;

}
