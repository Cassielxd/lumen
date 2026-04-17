package com.lumencloud.lumen.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumencloud.lumen.admin.api.dto.SysLogDTO;
import com.lumencloud.lumen.admin.api.entity.SysLog;
import com.lumencloud.lumen.admin.service.SysLogService;
import com.lumencloud.lumen.common.core.util.R;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysLogControllerTest {

	@Mock
	private SysLogService sysLogService;

	@InjectMocks
	private SysLogController controller;

	@Test
	void getLogPageShouldDelegateToService() {
		Page<SysLog> page = new Page<>(1, 10);
		page.setTotal(1);
		SysLog record = new SysLog();
		record.setTitle("登录成功");
		page.setRecords(List.of(record));
		when(sysLogService.getLogPage(any(Page.class), any(SysLogDTO.class))).thenReturn(page);

		R<Page<SysLog>> response = controller.getLogPage(new Page<>(1, 10), new SysLogDTO());

		assertEquals(1, response.getData().getTotal());
		assertEquals("登录成功", response.getData().getRecords().get(0).getTitle());
	}

	@Test
	void listShouldDelegateToService() {
		SysLog record = new SysLog();
		record.setTitle("平台撤销会话");
		when(sysLogService.listLogs(any(SysLogDTO.class))).thenReturn(List.of(record));

		R<List<SysLog>> response = controller.list(new SysLogDTO());

		assertEquals(1, response.getData().size());
		assertEquals("平台撤销会话", response.getData().get(0).getTitle());
	}

	@Test
	void saveLogShouldDelegateToService() {
		when(sysLogService.saveLog(any(SysLog.class))).thenReturn(Boolean.TRUE);

		R<Boolean> response = controller.saveLog(new SysLog());

		assertTrue(Boolean.TRUE.equals(response.getData()));
		verify(sysLogService).saveLog(any(SysLog.class));
	}

}
