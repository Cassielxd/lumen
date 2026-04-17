package com.lumencloud.lumen.admin.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lumencloud.lumen.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Account login identifier.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Account login identifier")
public class AuthAccountIdentifier extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@TableId(value = "identifier_id", type = IdType.ASSIGN_ID)
	@Schema(description = "Identifier ID")
	private Long identifierId;

	@Schema(description = "Account ID")
	private Long accountId;

	@Schema(description = "Client ID")
	private String clientId;

	@Schema(description = "Identifier type")
	private String identifierType;

	@Schema(description = "Identifier value")
	private String identifierValue;

	@Schema(description = "Primary flag")
	private String primaryFlag;

	@Schema(description = "Verified at")
	private LocalDateTime verifiedAt;

	@Schema(description = "Status")
	private String status;

}
