package com.lumencloud.lumen.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Public login client metadata used by the demo login page.
 */
@Data
@Schema(description = "Public login client metadata")
public class PublicLoginClientVO {

	@Schema(description = "Client ID")
	private String clientId;

	@Schema(description = "Client secret for demo login page")
	private String clientSecret;

	@Schema(description = "Scope")
	private String scope;

	@Schema(description = "Authorized grant types")
	private String[] authorizedGrantTypes;

	@Schema(description = "Whether captcha is required for password login")
	private Boolean requiresCaptcha;

	@Schema(description = "Whether password should be encrypted on the client side")
	private Boolean encryptPassword;

	@Schema(description = "Client display name")
	private String displayName;

	@Schema(description = "Client audience label")
	private String audience;

	@Schema(description = "Client description")
	private String description;

}
