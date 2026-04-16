package com.lumencloud.lumen.admin.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.lumencloud.lumen.admin.api.entity.SysOauthClientDetails;
import com.lumencloud.lumen.admin.api.vo.PublicLoginClientVO;
import com.lumencloud.lumen.admin.service.SysOauthClientDetailsService;
import com.lumencloud.lumen.common.core.util.R;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysClientControllerTest {

	@Mock
	private SysOauthClientDetailsService clientDetailsService;

	@InjectMocks
	private SysClientController controller;

	@Test
	void listPublicClientsShouldExposeLoginMetadata() {
		SysOauthClientDetails client = new SysOauthClientDetails();
		client.setClientId("app");
		client.setClientSecret("app-secret");
		client.setScope("server");
		client.setAuthorizedGrantTypes(new String[] { "password", "otp" });
		client.setAdditionalInformation("""
				{"captcha_flag":"1","enc_flag":"0","display_name":"Member App","audience":"Member","description":"Member login portal"}
				""");
		when(clientDetailsService.list(any(Wrapper.class))).thenReturn(List.of(client));

		R<List<PublicLoginClientVO>> response = controller.listPublicClients();

		assertEquals(1, response.getData().size());
		PublicLoginClientVO metadata = response.getData().get(0);
		assertEquals("app", metadata.getClientId());
		assertEquals("app-secret", metadata.getClientSecret());
		assertTrue(metadata.getRequiresCaptcha());
		assertFalse(metadata.getEncryptPassword());
		assertEquals("Member App", metadata.getDisplayName());
		assertEquals("Member", metadata.getAudience());
		assertEquals("Member login portal", metadata.getDescription());
	}

}
