package com.lumencloud.lumen.admin.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class SysOauthClientDetailsServiceImplTest {

	@Test
	void normalizeGrantTypesShouldDropDirtyValues() {
		assertArrayEquals(new String[] { "password", "otp", "passkey" },
				SysOauthClientDetailsServiceImpl
					.normalizeGrantTypes(new String[] { "password", "", "null", "otp", "undefined", "passkey", "otp" }));
	}

	@Test
	void normalizeGrantTypesShouldHandleEmptyInput() {
		assertArrayEquals(new String[0], SysOauthClientDetailsServiceImpl.normalizeGrantTypes(null));
		assertArrayEquals(new String[0], SysOauthClientDetailsServiceImpl.normalizeGrantTypes(new String[0]));
	}

}
