UPDATE `sys_oauth_client_details`
SET `additional_information` = JSON_SET(
    COALESCE(NULLIF(`additional_information`, ''), '{}'),
    '$.display_name', '会员入口',
    '$.audience', '会员',
    '$.description', '会员登录入口，支持密码、OTP 和 Passkey。',
    '$.captcha_flag', '1',
    '$.enc_flag', '1'
)
WHERE `client_id` = 'app';

UPDATE `sys_oauth_client_details`
SET `additional_information` = JSON_SET(
    COALESCE(NULLIF(`additional_information`, ''), '{}'),
    '$.display_name', '社区运营',
    '$.audience', '社区运营',
    '$.description', '社区运营入口，默认使用密码登录。',
    '$.captcha_flag', '1',
    '$.enc_flag', '1'
)
WHERE `client_id` = 'daemon';

UPDATE `sys_oauth_client_details`
SET `additional_information` = JSON_SET(
    COALESCE(NULLIF(`additional_information`, ''), '{}'),
    '$.display_name', '平台运营',
    '$.audience', '平台运营',
    '$.description', '平台运营入口，负责 Client、登录方式和账号治理。',
    '$.captcha_flag', '1',
    '$.enc_flag', '1'
)
WHERE `client_id` = 'lumen';

UPDATE `sys_oauth_client_details`
SET `additional_information` = JSON_SET(
    COALESCE(NULLIF(`additional_information`, ''), '{}'),
    '$.display_name', '调试沙盒',
    '$.audience', '调试',
    '$.description', '联调专用入口，用于快速验证 Token、会话和 Passkey。',
    '$.captcha_flag', '0',
    '$.enc_flag', '0'
)
WHERE `client_id` = 'test';
