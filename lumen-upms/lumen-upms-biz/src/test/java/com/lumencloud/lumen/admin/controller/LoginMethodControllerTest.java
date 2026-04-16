package com.lumencloud.lumen.admin.controller;

import com.lumencloud.lumen.admin.api.entity.SysDict;
import com.lumencloud.lumen.admin.api.entity.SysDictItem;
import com.lumencloud.lumen.admin.service.SysDictItemService;
import com.lumencloud.lumen.admin.service.SysDictService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.lumencloud.lumen.common.core.util.R;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginMethodControllerTest {

	@Mock
	private SysDictService sysDictService;

	@Mock
	private SysDictItemService sysDictItemService;

	@InjectMocks
	private LoginMethodController controller;

	@Test
	void saveShouldInjectGrantTypeDictionary() {
		SysDict dict = new SysDict();
		dict.setId(14L);
		when(sysDictService.getOne(any(), any(Boolean.class))).thenReturn(dict);
		when(sysDictItemService.save(any(SysDictItem.class))).thenReturn(true);

		SysDictItem item = new SysDictItem();
		item.setItemValue("magic_link");
		item.setLabel("Magic Link");

		R<Boolean> response = controller.save(item);

		ArgumentCaptor<SysDictItem> captor = ArgumentCaptor.forClass(SysDictItem.class);
		verify(sysDictItemService).save(captor.capture());
		assertEquals(14L, captor.getValue().getDictId());
		assertEquals("grant_types", captor.getValue().getDictType());
		assertTrue(Boolean.TRUE.equals(response.getData()));
	}

	@Test
	void updateShouldReuseGrantTypeDictionaryBinding() {
		SysDictItem existing = new SysDictItem();
		existing.setId(100L);
		existing.setDictId(14L);
		existing.setDictType("grant_types");
		when(sysDictItemService.getById(100L)).thenReturn(existing);
		when(sysDictItemService.updateDictItem(any(SysDictItem.class))).thenReturn(R.ok());

		SysDictItem request = new SysDictItem();
		request.setId(100L);
		request.setItemValue("passkey");
		request.setLabel("Passkey");

		controller.update(request);

		ArgumentCaptor<SysDictItem> captor = ArgumentCaptor.forClass(SysDictItem.class);
		verify(sysDictItemService).updateDictItem(captor.capture());
		assertEquals(14L, captor.getValue().getDictId());
		assertEquals("grant_types", captor.getValue().getDictType());
	}

	@Test
	void listShouldReturnGrantTypeItems() {
		SysDictItem item = new SysDictItem();
		item.setItemValue("otp");
		when(sysDictItemService.list(org.mockito.ArgumentMatchers.<Wrapper<SysDictItem>>any())).thenReturn(List.of(item));

		R<List<SysDictItem>> response = controller.list();

		assertEquals(1, response.getData().size());
		assertSame(item, response.getData().get(0));
	}

}
