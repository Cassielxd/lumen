package com.lumencloud.lumen.admin.service.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumencloud.lumen.admin.api.entity.SysOauthClientDetails;
import com.lumencloud.lumen.admin.mapper.SysOauthClientDetailsMapper;
import com.lumencloud.lumen.admin.service.SysOauthClientDetailsService;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.util.R;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * OAuth client details service implementation.
 */
@Service
public class SysOauthClientDetailsServiceImpl extends ServiceImpl<SysOauthClientDetailsMapper, SysOauthClientDetails>
		implements SysOauthClientDetailsService {

	@Override
	@CacheEvict(value = CacheConstants.CLIENT_DETAILS_KEY, key = "#clientDetails.clientId")
	@Transactional(rollbackFor = Exception.class)
	public Boolean updateClientById(SysOauthClientDetails clientDetails) {
		clientDetails.setAuthorizedGrantTypes(normalizeGrantTypes(clientDetails.getAuthorizedGrantTypes()));
		insertOrUpdate(clientDetails);
		return Boolean.TRUE;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean saveClient(SysOauthClientDetails clientDetails) {
		clientDetails.setAuthorizedGrantTypes(normalizeGrantTypes(clientDetails.getAuthorizedGrantTypes()));
		insertOrUpdate(clientDetails);
		return Boolean.TRUE;
	}

	private SysOauthClientDetails insertOrUpdate(SysOauthClientDetails clientDetails) {
		saveOrUpdate(clientDetails);
		return clientDetails;
	}

	@Override
	public Page getClientPage(Page page, SysOauthClientDetails query) {
		return baseMapper.selectPage(page, Wrappers.query(query));
	}

	@Override
	@CacheEvict(value = CacheConstants.CLIENT_DETAILS_KEY, allEntries = true)
	public R syncClientCache() {
		return R.ok();
	}

	static String[] normalizeGrantTypes(String[] grantTypes) {
		if (ArrayUtil.isEmpty(grantTypes)) {
			return new String[0];
		}
		return Arrays.stream(grantTypes)
			.filter(StrUtil::isNotBlank)
			.map(String::trim)
			.filter(item -> !StrUtil.equalsAnyIgnoreCase(item, "null", "undefined"))
			.distinct()
			.toArray(String[]::new);
	}

}
