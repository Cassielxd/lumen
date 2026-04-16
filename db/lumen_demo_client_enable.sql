UPDATE `sys_oauth_client_details`
SET `authorized_grant_types` = CASE
    WHEN `authorized_grant_types` IS NULL OR `authorized_grant_types` = '' THEN 'password,refresh_token,otp,passkey'
    ELSE `authorized_grant_types`
END
WHERE `client_id` = 'test';

UPDATE `sys_oauth_client_details`
SET `authorized_grant_types` = CASE
    WHEN LOCATE('otp', `authorized_grant_types`) > 0 THEN `authorized_grant_types`
    ELSE CONCAT(`authorized_grant_types`, ',otp')
END
WHERE `client_id` = 'test';

UPDATE `sys_oauth_client_details`
SET `authorized_grant_types` = CASE
    WHEN LOCATE('passkey', `authorized_grant_types`) > 0 THEN `authorized_grant_types`
    ELSE CONCAT(`authorized_grant_types`, ',passkey')
END
WHERE `client_id` = 'test';
