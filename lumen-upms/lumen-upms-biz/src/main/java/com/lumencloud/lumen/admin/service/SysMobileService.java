/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the lumencloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.lumencloud.lumen.admin.service;

import com.lumencloud.lumen.admin.api.vo.SmsCodeSendVO;
import com.lumencloud.lumen.common.core.util.R;

/**
 * 手机验证码服务接口。
 */
public interface SysMobileService {

	/**
	 * 发送手机验证码。
	 * @param mobile 手机号
	 * @return 验证码结果
	 */
	R<SmsCodeSendVO> sendSmsCode(String mobile);

}
