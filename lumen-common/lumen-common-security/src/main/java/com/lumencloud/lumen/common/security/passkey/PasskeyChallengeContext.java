package com.lumencloud.lumen.common.security.passkey;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Redis-backed passkey challenge context.
 */
@Data
@Schema(description = "Passkey challenge context")
public class PasskeyChallengeContext implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public static final String TYPE_ASSERTION = "assertion";

	public static final String TYPE_REGISTRATION = "registration";

	private String type;

	private String challenge;

	private String clientId;

	private Long accountId;

	private Long userId;

	private String username;

	private String displayName;

	private String rpId;

	private String origin;

	private LocalDateTime issuedAt;

}
