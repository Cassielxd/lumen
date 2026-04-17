package com.lumencloud.lumen.admin.controller;

import com.lumencloud.lumen.admin.api.vo.AuthSessionVO;
import com.lumencloud.lumen.admin.service.AuthSessionService;
import com.lumencloud.lumen.common.core.util.R;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionControllerTest {

	@Mock
	private AuthSessionService authSessionService;

	@InjectMocks
	private AuthSessionController controller;

	@Test
	void manageSessionsShouldReturnFilteredSessionList() {
		AuthSessionVO session = new AuthSessionVO();
		session.setSid("sid-1");
		session.setClientId("app");
		session.setPrincipalName("admin");
		when(authSessionService.listAll("app", "admin", "0")).thenReturn(List.of(session));

		R<List<AuthSessionVO>> response = controller.manageSessions("app", "admin", "0");

		assertEquals(1, response.getData().size());
		assertEquals("sid-1", response.getData().get(0).getSid());
	}

	@Test
	void revokeManagedSessionShouldDelegateToService() {
		when(authSessionService.adminLogoutBySid("sid-2")).thenReturn(Boolean.TRUE);

		R<Boolean> response = controller.revokeManagedSession("sid-2");

		verify(authSessionService).adminLogoutBySid("sid-2");
		assertTrue(Boolean.TRUE.equals(response.getData()));
	}

}
