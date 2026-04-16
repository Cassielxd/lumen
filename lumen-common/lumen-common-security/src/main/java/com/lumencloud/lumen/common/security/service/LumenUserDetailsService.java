package com.lumencloud.lumen.common.security.service;

import cn.hutool.core.util.StrUtil;
import com.lumencloud.lumen.admin.api.dto.UserInfo;
import com.lumencloud.lumen.admin.api.entity.SysRole;
import com.lumencloud.lumen.common.core.constant.CommonConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.core.util.RetOps;
import org.springframework.core.Ordered;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * User details service abstraction with ordering.
 */
public interface LumenUserDetailsService extends UserDetailsService, Ordered {

	default boolean support(String clientId, String grantType) {
		return true;
	}

	default int getOrder() {
		return 0;
	}

	default UserDetails getUserDetails(R<UserInfo> result) {
		UserInfo info = RetOps.of(result)
			.getData()
			.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		Set<String> dbAuthsSet = new HashSet<>();

		resolveRoles(info).forEach(role -> dbAuthsSet.add(SecurityConstants.ROLE + role.getRoleId()));
		dbAuthsSet.addAll(resolvePermissions(info));
		Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(dbAuthsSet.toArray(new String[0]));

		Long deptId = info.getDept() == null ? null : info.getDept().getDeptId();
		return new LumenUser(info.getUserId(), deptId, info.getAccountId(), info.getAccountClientId(),
				info.getUsername(), normalizePassword(info.getPassword()), info.getPhone(), true, true, true,
				StrUtil.equals(info.getLockFlag(), CommonConstants.STATUS_NORMAL), authorities);
	}

	default UserDetails loadUserByUser(LumenUser lumenUser) {
		return this.loadUserByUsername(lumenUser.getUsername());
	}

	private static List<String> resolvePermissions(UserInfo info) {
		return info.getPermissions() == null ? List.of() : info.getPermissions();
	}

	private static List<SysRole> resolveRoles(UserInfo info) {
		return info.getRoleList() == null ? List.of() : info.getRoleList();
	}

	private static String normalizePassword(String password) {
		if (StrUtil.isBlank(password)) {
			return SecurityConstants.NOOP + UUID.randomUUID();
		}
		if (password.startsWith("{")) {
			return password;
		}
		return SecurityConstants.BCRYPT + password;
	}

}
