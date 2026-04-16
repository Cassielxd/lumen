package com.lumencloud.lumen.admin.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lumencloud.lumen.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Extensible account credential record.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Account credential")
public class AuthAccountCredential extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@TableId(value = "credential_id", type = IdType.ASSIGN_ID)
	@Schema(description = "Credential ID")
	private Long credentialId;

	@Schema(description = "Account ID")
	private Long accountId;

	@Schema(description = "Credential type")
	private String credentialType;

	@Schema(description = "Credential key")
	private String credentialKey;

	@Schema(description = "Secret value")
	private String secretValue;

	@Schema(description = "Status")
	private String status;

	@Schema(description = "Verified at")
	private LocalDateTime verifiedAt;

}
