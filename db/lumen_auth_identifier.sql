SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `auth_account_identifier` (
  `identifier_id` bigint NOT NULL COMMENT 'Identifier ID',
  `account_id` bigint NOT NULL COMMENT 'Account ID',
  `client_id` varchar(32) NOT NULL COMMENT 'Client ID',
  `identifier_type` varchar(32) NOT NULL COMMENT 'Identifier type',
  `identifier_value` varchar(128) NOT NULL COMMENT 'Identifier value',
  `primary_flag` char(1) DEFAULT '1' COMMENT 'Primary flag',
  `verified_at` datetime DEFAULT NULL COMMENT 'Verified time',
  `status` char(1) DEFAULT '0' COMMENT 'Status',
  `create_by` varchar(64) DEFAULT NULL COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) DEFAULT NULL COMMENT 'Updated by',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`identifier_id`) USING BTREE,
  UNIQUE KEY `uk_auth_account_identifier_client` (`client_id`,`identifier_type`,`identifier_value`) USING BTREE,
  KEY `idx_auth_account_identifier_account_id` (`account_id`) USING BTREE
) ENGINE=InnoDB COMMENT='Authentication account identifier';

INSERT IGNORE INTO `auth_account_identifier`
(`identifier_id`, `account_id`, `client_id`, `identifier_type`, `identifier_value`, `primary_flag`, `verified_at`, `status`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT 120000 + `account_id`, `account_id`, `client_id`, 'USERNAME', `login_name`, '1', CURRENT_TIMESTAMP, `status`, COALESCE(`update_by`, `create_by`), COALESCE(`create_time`, CURRENT_TIMESTAMP), `update_by`, `update_time`
FROM `auth_account`
WHERE `login_name` IS NOT NULL AND `login_name` <> '';

INSERT IGNORE INTO `auth_account_identifier`
(`identifier_id`, `account_id`, `client_id`, `identifier_type`, `identifier_value`, `primary_flag`, `verified_at`, `status`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT 130000 + `account_id`, `account_id`, `client_id`, 'PHONE', `phone`, '1', NULL, `status`, COALESCE(`update_by`, `create_by`), COALESCE(`create_time`, CURRENT_TIMESTAMP), `update_by`, `update_time`
FROM `auth_account`
WHERE `phone` IS NOT NULL AND `phone` <> '';

SET FOREIGN_KEY_CHECKS = 1;
