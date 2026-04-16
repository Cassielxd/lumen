package com.lumencloud.lumen.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * SMS code response for demo-friendly OTP login.
 */
@Data
@Accessors(chain = true)
@Schema(description = "SMS code send result")
public class SmsCodeSendVO {

	@Schema(description = "Mobile number")
	private String mobile;

	@Schema(description = "Verification code cached for OTP login")
	private String code;

	@Schema(description = "Whether the existing code was reused")
	private Boolean reused;

	@Schema(description = "Whether an external SMS provider was invoked successfully")
	private Boolean delivered;

	@Schema(description = "Code validity duration in seconds")
	private Long expiresInSeconds;

}
