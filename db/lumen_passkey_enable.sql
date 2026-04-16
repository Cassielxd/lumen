INSERT IGNORE INTO `sys_dict_item`
(`item_id`, `dict_id`, `item_text`, `item_value`, `label`, `description`, `sort_order`, `create_by`, `update_by`, `create_time`, `update_time`, `remarks`, `del_flag`)
VALUES
(10002, 14, 'passkey', 'PASSKEY', 'grant_types', 'passkey login grant', 7, 'admin', NULL, '2026-04-15 00:00:00', NULL, NULL, '0');

UPDATE `sys_oauth_client_details`
SET `authorized_grant_types` = CASE
    WHEN LOCATE('passkey', `authorized_grant_types`) > 0 THEN `authorized_grant_types`
    WHEN `authorized_grant_types` IS NULL OR `authorized_grant_types` = '' THEN 'passkey'
    ELSE CONCAT(`authorized_grant_types`, ',passkey')
END
WHERE `client_id` IN ('app', 'lumen');
