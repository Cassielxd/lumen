package com.lumencloud.lumen.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Platform credential governance view.
 */
@Data
@Schema(description = "Account credential governance view")
public class AuthAccountCredentialManageVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Account ID")
	private Long accountId;

	@Schema(description = "User ID")
	private Long userId;

	@Schema(description = "Client ID")
	private String clientId;

	@Schema(description = "Login name")
	private String loginName;

	@Schema(description = "Phone")
	private String phone;

	@Schema(description = "Account status")
	private String accountStatus;

	@Schema(description = "Password credential status")
	private String passwordStatus;

	@Schema(description = "OTP credential status")
	private String otpStatus;

	@Schema(description = "Normal passkey count")
	private Integer passkeyCount;

	@Schema(description = "Latest verified time")
	private LocalDateTime latestVerifiedAt;

	@Schema(description = "Account identifiers")
	private List<AuthAccountIdentifierManageVO> identifiers;

}
