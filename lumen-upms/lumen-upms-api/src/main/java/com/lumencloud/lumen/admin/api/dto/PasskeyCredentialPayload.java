package com.lumencloud.lumen.admin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Stored passkey credential metadata.
 */
@Data
@Schema(description = "Passkey credential payload")
public class PasskeyCredentialPayload implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "COSE public key in base64url")
	private String publicKeyCose;

	@Schema(description = "Signature counter")
	private Long signCount;

	@Schema(description = "COSE algorithm identifier")
	private Integer algorithm;

	@Schema(description = "AAGUID")
	private String aaguid;

	@Schema(description = "Authenticator transports")
	private List<String> transports;

}
