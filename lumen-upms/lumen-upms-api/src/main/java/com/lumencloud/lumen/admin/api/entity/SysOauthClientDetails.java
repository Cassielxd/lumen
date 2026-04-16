package com.lumencloud.lumen.admin.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.lumencloud.lumen.common.mybatis.handler.JsonStringArrayTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * OAuth client details.
 */
@Data
@Schema(description = "OAuth client details")
@EqualsAndHashCode(callSuper = true)
@TableName(autoResultMap = true)
public class SysOauthClientDetails extends Model<SysOauthClientDetails> {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@Schema(description = "Primary key")
	private Long id;

	@NotBlank(message = "client_id 不能为空")
	@Schema(description = "Client ID")
	private String clientId;

	@NotBlank(message = "client_secret 不能为空")
	@Schema(description = "Client secret")
	private String clientSecret;

	@Schema(description = "Resource IDs")
	private String resourceIds;

	@NotBlank(message = "scope 不能为空")
	@Schema(description = "Scope")
	private String scope;

	@TableField(typeHandler = JsonStringArrayTypeHandler.class)
	@Schema(description = "Authorized grant types")
	private String[] authorizedGrantTypes;

	@Schema(description = "Redirect URIs")
	private String webServerRedirectUri;

	@Schema(description = "Authorities")
	private String authorities;

	@Schema(description = "Access token validity in seconds")
	private Integer accessTokenValidity;

	@Schema(description = "Refresh token validity in seconds")
	private Integer refreshTokenValidity;

	@Schema(description = "Additional information JSON")
	private String additionalInformation;

	@Schema(description = "Auto approve flag")
	private String autoapprove;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "Delete flag")
	private String delFlag;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "Created by")
	private String createBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "Updated by")
	private String updateBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "Created time")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "Updated time")
	private LocalDateTime updateTime;

}
