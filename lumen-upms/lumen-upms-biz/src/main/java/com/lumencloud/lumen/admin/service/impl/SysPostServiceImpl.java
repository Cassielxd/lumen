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
package com.lumencloud.lumen.admin.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumencloud.lumen.admin.api.entity.SysPost;
import com.lumencloud.lumen.admin.mapper.SysPostMapper;
import com.lumencloud.lumen.admin.service.SysPostService;
import com.lumencloud.lumen.common.core.exception.ErrorCodes;
import com.lumencloud.lumen.common.core.util.MsgUtils;
import com.lumencloud.lumen.common.core.util.R;

import cn.hutool.core.collection.CollUtil;

/**
 * 岗位信息表服务实现类
 *
 * @author lengleng
 * @date 2025/05/30
 */
@Service
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements SysPostService {

}
