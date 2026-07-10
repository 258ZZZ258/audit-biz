package com.orientsec.genesis.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.orientsec.genesis.auth.common.model.Menu;
import com.orientsec.genesis.auth.common.model.User;
import com.orientsec.genesis.auth.filter.bjca.utils.BJCAUtils;
import com.orientsec.genesis.auth.service.GenesisUserService;
import com.orientsec.idap.common.model.Result;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserAuthControllerTest {

    private final GenesisUserService service = mock(GenesisUserService.class);
    private final UserAuthController controller =
            new UserAuthController(service, mock(BJCAUtils.class), "https://portal.example.test/");

    @Test
    void returnsMenusFromGenesisService() {
        List<Menu> menus = Collections.singletonList(mock(Menu.class));
        when(service.getUserMenus()).thenReturn(menus);

        Result<List<Menu>> result = controller.menuList();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isSameAs(menus);
    }

    @Test
    void returnsCurrentGenesisUser() {
        User user = mock(User.class);
        when(service.getCurrentUser()).thenReturn(user);

        Result<User> result = controller.whoAmI();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isSameAs(user);
    }
}
