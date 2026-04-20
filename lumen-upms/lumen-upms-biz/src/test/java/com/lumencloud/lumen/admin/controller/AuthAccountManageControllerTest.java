package com.lumencloud.lumen.admin.controller;

import com.lumencloud.lumen.admin.api.dto.AuthAccountCredentialStatusDTO;
import com.lumencloud.lumen.admin.api.dto.AuthAccountIdentifierUpsertDTO;
import com.lumencloud.lumen.admin.api.dto.AuthAccountPasswordResetDTO;
import com.lumencloud.lumen.admin.api.vo.AuthAccountCredentialManageVO;
import com.lumencloud.lumen.admin.api.vo.AuthAccountIdentifierManageVO;
import com.lumencloud.lumen.admin.service.AuthAccountService;
import com.lumencloud.lumen.common.core.util.R;
import com.lumencloud.lumen.common.security.annotation.HasPermission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthAccountManageControllerTest {

	@Mock
	private AuthAccountService authAccountService;

	@InjectMocks
	private AuthAccountManageController controller;

	@Test
	void listShouldReturnCredentialGovernanceRows() {
		AuthAccountCredentialManageVO row = new AuthAccountCredentialManageVO();
		row.setAccountId(1001L);
		row.setClientId("app");
		when(authAccountService.listManageAccounts("app", "admin", null)).thenReturn(List.of(row));

		R<List<AuthAccountCredentialManageVO>> response = controller.list("app", "admin", null);

		assertEquals(1, response.getData().size());
		assertEquals("app", response.getData().get(0).getClientId());
	}

	@Test
	void resetPasswordShouldDelegateToService() {
		AuthAccountPasswordResetDTO request = new AuthAccountPasswordResetDTO();
		request.setAccountId(1001L);
		request.setNewPassword("123456");
		when(authAccountService.resetPassword(org.mockito.ArgumentMatchers.eq(1001L),
				org.mockito.ArgumentMatchers.eq("123456"), anyString())).thenReturn(Boolean.TRUE);

		R<Boolean> response = controller.resetPassword(request);

		assertTrue(Boolean.TRUE.equals(response.getData()));
		verify(authAccountService).resetPassword(org.mockito.ArgumentMatchers.eq(1001L),
				org.mockito.ArgumentMatchers.eq("123456"), anyString());
	}

	@Test
	void updateOtpStatusShouldDelegateToService() {
		AuthAccountCredentialStatusDTO request = new AuthAccountCredentialStatusDTO();
		request.setAccountId(1001L);
		request.setStatus("9");
		when(authAccountService.updateOtpStatus(org.mockito.ArgumentMatchers.eq(1001L),
				org.mockito.ArgumentMatchers.eq("9"), anyString())).thenReturn(Boolean.TRUE);

		R<Boolean> response = controller.updateOtpStatus(request);

		assertTrue(Boolean.TRUE.equals(response.getData()));
		verify(authAccountService).updateOtpStatus(org.mockito.ArgumentMatchers.eq(1001L),
				org.mockito.ArgumentMatchers.eq("9"), anyString());
	}

	@Test
	void identifiersShouldReturnIdentifierRows() {
		AuthAccountIdentifierManageVO identifier = new AuthAccountIdentifierManageVO();
		identifier.setIdentifierId(2001L);
		identifier.setIdentifierType("EMAIL");
		when(authAccountService.listIdentifiers(1001L)).thenReturn(List.of(identifier));

		R<List<AuthAccountIdentifierManageVO>> response = controller.identifiers(1001L);

		assertEquals(1, response.getData().size());
		assertEquals("EMAIL", response.getData().get(0).getIdentifierType());
	}

	@Test
	void saveIdentifierShouldDelegateToService() {
		AuthAccountIdentifierUpsertDTO request = new AuthAccountIdentifierUpsertDTO();
		request.setAccountId(1001L);
		request.setIdentifierType("EMAIL");
		request.setIdentifierValue("admin@example.com");
		when(authAccountService.saveIdentifier(org.mockito.ArgumentMatchers.eq(1001L),
				org.mockito.ArgumentMatchers.eq("EMAIL"), org.mockito.ArgumentMatchers.eq("admin@example.com"),
				anyString())).thenReturn(Boolean.TRUE);

		R<Boolean> response = controller.saveIdentifier(request);

		assertTrue(Boolean.TRUE.equals(response.getData()));
		verify(authAccountService).saveIdentifier(org.mockito.ArgumentMatchers.eq(1001L),
				org.mockito.ArgumentMatchers.eq("EMAIL"), org.mockito.ArgumentMatchers.eq("admin@example.com"),
				anyString());
	}

	@Test
	void removeIdentifierShouldDelegateToService() {
		when(authAccountService.removeIdentifier(org.mockito.ArgumentMatchers.eq(2001L), anyString()))
			.thenReturn(Boolean.TRUE);

		R<Boolean> response = controller.removeIdentifier(2001L);

		assertTrue(Boolean.TRUE.equals(response.getData()));
		verify(authAccountService).removeIdentifier(org.mockito.ArgumentMatchers.eq(2001L), anyString());
	}

	@Test
	void manageEndpointsShouldRequireAdminPermission() throws NoSuchMethodException {
		assertHasPermission("list", new Class<?>[] { String.class, String.class, String.class });
		assertHasPermission("resetPassword", new Class<?>[] { AuthAccountPasswordResetDTO.class });
		assertHasPermission("updateOtpStatus", new Class<?>[] { AuthAccountCredentialStatusDTO.class });
		assertHasPermission("clearPasskeys", new Class<?>[] { Long.class });
		assertHasPermission("identifiers", new Class<?>[] { Long.class });
		assertHasPermission("saveIdentifier", new Class<?>[] { AuthAccountIdentifierUpsertDTO.class });
		assertHasPermission("removeIdentifier", new Class<?>[] { Long.class });
	}

	private void assertHasPermission(String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
		Method method = AuthAccountManageController.class.getDeclaredMethod(methodName, parameterTypes);
		HasPermission annotation = method.getAnnotation(HasPermission.class);
		assertNotNull(annotation);
		assertArrayEquals(new String[] { "auth_account_manage" }, annotation.value());
	}

}
