package com.orientsec.idap.core.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orientsec.idap.core.base.IdapTestServer;
import com.orientsec.idap.core.config.SecurityConfig;
import com.orientsec.idap.core.web.error.RestAuthEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/** /health permitAll → 200 Result 包装（安全链在位仍公开，A0+A1）。 */
@WebMvcTest(HealthController.class)
@ContextConfiguration(classes = IdapTestServer.class)
@Import({
    SecurityConfig.class,
    RestAuthEntryPoint.class,
    HealthControllerTest.TestSecurityBeans.class
})
class HealthControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    /**
     * SecurityConfig 的 oauth2ResourceServer().jwt() 需一个 JwtDecoder bean，用 lambda 实 bean 提供（非
     * Mockito）。
     *
     * <p>不用 @MockBean：其依赖 Mockito inline self-attach，在受限沙箱 / JDK 21+ / 信创环境会失败 （Codex
     * TEST-VERIFY-001，保持测试套零 Mockito 以稳过 CI/信创）。/health permitAll 不触发解码，抛异常即可。
     */
    @TestConfiguration
    static class TestSecurityBeans {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("permitAll 路径不触发令牌解码");
            };
        }
    }
}
