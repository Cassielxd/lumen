SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `auth_account` (
  `account_id` bigint NOT NULL COMMENT 'Account ID',
  `user_id` bigint NOT NULL COMMENT 'User ID',
  `client_id` varchar(32) NOT NULL COMMENT 'Client ID',
  `login_name` varchar(64) DEFAULT NULL COMMENT 'Login name',
  `phone` varchar(20) DEFAULT NULL COMMENT 'Phone',
  `status` char(1) DEFAULT '0' COMMENT 'Status',
  `create_by` varchar(64) DEFAULT NULL COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) DEFAULT NULL COMMENT 'Updated by',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`account_id`) USING BTREE,
  UNIQUE KEY `uk_auth_account_client_user` (`client_id`,`user_id`) USING BTREE,
  UNIQUE KEY `uk_auth_account_client_login_name` (`client_id`,`login_name`) USING BTREE,
  UNIQUE KEY `uk_auth_account_client_phone` (`client_id`,`phone`) USING BTREE
) ENGINE=InnoDB COMMENT='Client-bound authentication account';

CREATE TABLE IF NOT EXISTS `auth_account_credential` (
  `credential_id` bigint NOT NULL COMMENT 'Credential ID',
  `account_id` bigint NOT NULL COMMENT 'Account ID',
  `credential_type` varchar(32) NOT NULL COMMENT 'Credential type',
  `credential_key` varchar(128) NOT NULL DEFAULT '' COMMENT 'Credential key',
  `secret_value` varchar(255) DEFAULT NULL COMMENT 'Secret value',
  `status` char(1) DEFAULT '0' COMMENT 'Status',
  `verified_at` datetime DEFAULT NULL COMMENT 'Verified time',
  `create_by` varchar(64) DEFAULT NULL COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) DEFAULT NULL COMMENT 'Updated by',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`credential_id`) USING BTREE,
  UNIQUE KEY `uk_auth_account_credential` (`account_id`,`credential_type`,`credential_key`) USING BTREE
) ENGINE=InnoDB COMMENT='Authentication account credential';

CREATE TABLE IF NOT EXISTS `auth_session` (
  `session_id` bigint NOT NULL COMMENT 'Session ID',
  `sid` varchar(64) NOT NULL COMMENT 'Session key',
  `account_id` bigint NOT NULL COMMENT 'Account ID',
  `user_id` bigint NOT NULL COMMENT 'User ID',
  `client_id` varchar(32) NOT NULL COMMENT 'Client ID',
  `principal_name` varchar(64) NOT NULL COMMENT 'Principal name',
  `grant_type` varchar(32) DEFAULT NULL COMMENT 'Grant type',
  `access_token_hash` char(64) NOT NULL COMMENT 'Access token hash',
  `refresh_token_hash` char(64) DEFAULT NULL COMMENT 'Refresh token hash',
  `access_token_expires_at` datetime DEFAULT NULL COMMENT 'Access token expires at',
  `refresh_token_expires_at` datetime DEFAULT NULL COMMENT 'Refresh token expires at',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP address',
  `user_agent` varchar(512) DEFAULT NULL COMMENT 'User agent',
  `last_active_time` datetime DEFAULT NULL COMMENT 'Last active time',
  `logout_time` datetime DEFAULT NULL COMMENT 'Logout time',
  `status` char(1) DEFAULT '0' COMMENT 'Status',
  `create_by` varchar(64) DEFAULT NULL COMMENT 'Created by',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
  `update_by` varchar(64) DEFAULT NULL COMMENT 'Updated by',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
  PRIMARY KEY (`session_id`) USING BTREE,
  UNIQUE KEY `uk_auth_session_sid` (`sid`) USING BTREE,
  UNIQUE KEY `uk_auth_session_access_token_hash` (`access_token_hash`) USING BTREE,
  UNIQUE KEY `uk_auth_session_refresh_token_hash` (`refresh_token_hash`) USING BTREE,
  KEY `idx_auth_session_account_id` (`account_id`) USING BTREE,
  KEY `idx_auth_session_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB COMMENT='Authentication session';

INSERT IGNORE INTO `auth_account` VALUES (100001, 1, 'app', 'admin', '17034642999', '0', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account` VALUES (100002, 1, 'daemon', 'admin', '17034642999', '0', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account` VALUES (100003, 1, 'gen', 'admin', '17034642999', '0', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account` VALUES (100004, 1, 'mp', 'admin', '17034642999', '0', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account` VALUES (100005, 1, 'lumen', 'admin', '17034642999', '0', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account` VALUES (100006, 1, 'test', 'admin', '17034642999', '0', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');

INSERT IGNORE INTO `auth_account_credential` VALUES (110001, 100001, 'PASSWORD', '', '$2a$10$c/Ae0pRjJtMZg3BnvVpO.eIK6WYWVbKTzqgdy3afR7w.vd.xi3Mgy', '0', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account_credential` VALUES (110002, 100001, 'OTP', '', '17034642999', '0', NULL, 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account_credential` VALUES (110003, 100002, 'PASSWORD', '', '$2a$10$c/Ae0pRjJtMZg3BnvVpO.eIK6WYWVbKTzqgdy3afR7w.vd.xi3Mgy', '0', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account_credential` VALUES (110004, 100002, 'OTP', '', '17034642999', '0', NULL, 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account_credential` VALUES (110005, 100003, 'PASSWORD', '', '$2a$10$c/Ae0pRjJtMZg3BnvVpO.eIK6WYWVbKTzqgdy3afR7w.vd.xi3Mgy', '0', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account_credential` VALUES (110006, 100003, 'OTP', '', '17034642999', '0', NULL, 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account_credential` VALUES (110007, 100004, 'PASSWORD', '', '$2a$10$c/Ae0pRjJtMZg3BnvVpO.eIK6WYWVbKTzqgdy3afR7w.vd.xi3Mgy', '0', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account_credential` VALUES (110008, 100004, 'OTP', '', '17034642999', '0', NULL, 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account_credential` VALUES (110009, 100005, 'PASSWORD', '', '$2a$10$c/Ae0pRjJtMZg3BnvVpO.eIK6WYWVbKTzqgdy3afR7w.vd.xi3Mgy', '0', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account_credential` VALUES (110010, 100005, 'OTP', '', '17034642999', '0', NULL, 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account_credential` VALUES (110011, 100006, 'PASSWORD', '', '$2a$10$c/Ae0pRjJtMZg3BnvVpO.eIK6WYWVbKTzqgdy3afR7w.vd.xi3Mgy', '0', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');
INSERT IGNORE INTO `auth_account_credential` VALUES (110012, 100006, 'OTP', '', '17034642999', '0', NULL, 'admin', '2026-04-15 00:00:00', 'admin', '2026-04-15 00:00:00');

SET FOREIGN_KEY_CHECKS = 1;
