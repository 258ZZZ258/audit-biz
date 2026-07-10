package com.orientsec.idap.core.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orientsec.idap.core.base.IdapTestServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = IdapTestServer.class)
@AutoConfigureMockMvc
@Sql(statements = "DROP TABLE IF EXISTS idap_user_info")
@Sql(scripts = "classpath:ddl/V1.0.2__init_idap_user_info.sql")
class IdapUserInfoControllerIT {

    @Autowired private MockMvc mockMvc;

    @Test
    void createsAndListsUserThroughHttpContract() throws Exception {
        mockMvc.perform(
                        post("/idap/v1/idapUserInfo/create")
                                .with(jwt().jwt(jwt -> jwt.subject("admin")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"userName\":\"张三\",\"email\":\"zhang@example.com\","
                                                + "\"mobile\":\"13800000000\",\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("SUCCESS"));

        mockMvc.perform(
                        get("/idap/v1/idapUserInfo/list")
                                .param("userName", "张")
                                .with(jwt().jwt(jwt -> jwt.subject("admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].user_id").isNotEmpty())
                .andExpect(jsonPath("$.data[0].user_name").value("张三"));
    }
}
