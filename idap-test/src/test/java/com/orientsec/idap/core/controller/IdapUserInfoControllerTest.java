package com.orientsec.idap.core.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.orientsec.idap.common.model.Result;
import com.orientsec.idap.core.model.IdapUserInfo;
import com.orientsec.idap.core.service.IdapUserInfoService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IdapUserInfoControllerTest {

    private IdapUserInfoService service;
    private IdapUserInfoController controller;

    @BeforeEach
    void setUp() {
        service = mock(IdapUserInfoService.class);
        controller = new IdapUserInfoController(service);
    }

    @Test
    void listsUsersWithUnifiedResult() {
        IdapUserInfo user = user("U1");
        when(service.list(any(Wrapper.class))).thenReturn(Collections.singletonList(user));

        Result<List<IdapUserInfo>> result = controller.list("张", null, null, 1, 1);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsExactly(user);
    }

    @Test
    void returnsExistingUserDetail() {
        IdapUserInfo user = user("U1");
        when(service.getById("U1")).thenReturn(user);

        Result<IdapUserInfo> result = controller.detail("U1");

        assertThat(result.getData()).isSameAs(user);
    }

    @Test
    void createsUserWithScreenshotDefaults() {
        Map<String, Object> data = new HashMap<>();
        data.put("userName", "张三");
        data.put("email", "zhang@example.com");
        data.put("mobile", "13800000000");

        Result<Void> result = controller.create(data);

        ArgumentCaptor<IdapUserInfo> captor = ArgumentCaptor.forClass(IdapUserInfo.class);
        verify(service).save(captor.capture());
        IdapUserInfo saved = captor.getValue();
        assertThat(saved.getUserId()).isNotBlank();
        assertThat(saved.getSex()).isEqualTo((byte) 0);
        assertThat(saved.getStatus()).isEqualTo((byte) 1);
        assertThat(saved.getDeleted()).isEqualTo((byte) 0);
        assertThat(saved.getCreateUser()).isEqualTo("admin");
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void softDeletesEveryRequestedUser() {
        IdapUserInfo first = user("U1");
        IdapUserInfo second = user("U2");
        when(service.getById("U1")).thenReturn(first);
        when(service.getById("U2")).thenReturn(second);
        Map<String, Object> data =
                Collections.<String, Object>singletonMap("ids", Arrays.asList("U1", "U2"));

        controller.delete(data);

        ArgumentCaptor<IdapUserInfo> captor = ArgumentCaptor.forClass(IdapUserInfo.class);
        verify(service, org.mockito.Mockito.times(2)).updateById(captor.capture());
        assertThat(captor.getAllValues()).allMatch(user -> user.getDeleted() == (byte) 1);
    }

    @Test
    void changesUserStatus() {
        IdapUserInfo user = user("U1");
        when(service.getById("U1")).thenReturn(user);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", "U1");
        data.put("status", 0);

        controller.changeStatus(data);

        assertThat(user.getStatus()).isEqualTo((byte) 0);
        verify(service).updateById(user);
    }

    @Test
    void keepsPasswordResetAsScreenshotPlaceholder() {
        Result<Void> result =
                controller.resetPassword(Collections.<String, Object>singletonMap("userId", "U1"));

        assertThat(result.getCode()).isEqualTo(200);
    }

    private static IdapUserInfo user(String id) {
        IdapUserInfo user = new IdapUserInfo();
        user.setUserId(id);
        user.setUserName("张三");
        user.setDeleted((byte) 0);
        user.setStatus((byte) 1);
        return user;
    }
}
