package com.lumencloud.lumen.auth.support.passkey;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Public assertion options request.
 */
@Data
@Schema(description = "Passkey assertion options request")
public class PasskeyAssertionOptionsRequest implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Username")
	private String username;

}
