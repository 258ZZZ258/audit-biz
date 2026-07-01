package com.dfzq.auditai.biz.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** A1 验收：受保护端点强制认证 + permitAll 白名单 + 当前用户解析（TODO-AUTH-001）。 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void healthIsPublicWithoutToken() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserResolvedFromJwt() throws Exception {
        mockMvc.perform(
                        get("/api/v1/me")
                                .with(jwt().jwt(j -> j.subject("zhang").claim("name", "张审计"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value("zhang"))
                .andExpect(jsonPath("$.name").value("张审计"));
    }
}
