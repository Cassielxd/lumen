SET FOREIGN_KEY_CHECKS = 0;

INSERT IGNORE INTO `auth_account_identifier`
(`identifier_id`, `account_id`, `client_id`, `identifier_type`, `identifier_value`, `primary_flag`, `verified_at`, `status`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT 320000 + a.`account_id`, a.`account_id`, a.`client_id`, 'USERNAME', u.`username`, '1', CURRENT_TIMESTAMP,
       a.`status`, COALESCE(a.`update_by`, a.`create_by`, u.`update_by`, u.`create_by`),
       COALESCE(a.`create_time`, CURRENT_TIMESTAMP), a.`update_by`, a.`update_time`
FROM `auth_account` a
JOIN `sys_user` u ON u.`user_id` = a.`user_id`
WHERE u.`username` IS NOT NULL AND u.`username` <> '';

INSERT IGNORE INTO `auth_account_identifier`
(`identifier_id`, `account_id`, `client_id`, `identifier_type`, `identifier_value`, `primary_flag`, `verified_at`, `status`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT 330000 + a.`account_id`, a.`account_id`, a.`client_id`, 'PHONE', u.`phone`, '1', NULL,
       a.`status`, COALESCE(a.`update_by`, a.`create_by`, u.`update_by`, u.`create_by`),
       COALESCE(a.`create_time`, CURRENT_TIMESTAMP), a.`update_by`, a.`update_time`
FROM `auth_account` a
JOIN `sys_user` u ON u.`user_id` = a.`user_id`
WHERE u.`phone` IS NOT NULL AND u.`phone` <> '';

INSERT IGNORE INTO `auth_account_identifier`
(`identifier_id`, `account_id`, `client_id`, `identifier_type`, `identifier_value`, `primary_flag`, `verified_at`, `status`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT 340000 + a.`account_id`, a.`account_id`, a.`client_id`, 'EMAIL', LOWER(u.`email`), '0', NULL,
       a.`status`, COALESCE(a.`update_by`, a.`create_by`, u.`update_by`, u.`create_by`),
       COALESCE(a.`create_time`, CURRENT_TIMESTAMP), a.`update_by`, a.`update_time`
FROM `auth_account` a
JOIN `sys_user` u ON u.`user_id` = a.`user_id`
WHERE u.`email` IS NOT NULL AND u.`email` <> '';

ALTER TABLE `auth_account`
  DROP INDEX `uk_auth_account_client_login_name`,
  DROP INDEX `uk_auth_account_client_phone`,
  DROP COLUMN `login_name`,
  DROP COLUMN `phone`;

ALTER TABLE `sys_user`
  DROP COLUMN `password`,
  DROP COLUMN `salt`;

SET FOREIGN_KEY_CHECKS = 1;
