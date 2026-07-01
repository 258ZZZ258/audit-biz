package com.dfzq.auditai.biz.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** A3 验收:制度查询 SSE 端点 —— 授权流式 context/delta/done · 越权 B102 · 未认证 B101。 */
@SpringBootTest
@AutoConfigureMockMvc
class QueryControllerTest {

    private static final String BODY = "{\"question\":\"客户风险等级更新不及时的相关案例有哪些?\"}";

    @Autowired private MockMvc mockMvc;

    private RequestPostProcessor auth(String sub, List<String> corpus) {
        return jwt().jwt(
                        j ->
                                j.subject(sub)
                                        .claim("perm_tags", List.of())
                                        .claim("corpus_scope", corpus));
    }

    @Test
    void authorizedQueryStreamsSse() throws Exception {
        MvcResult r =
                mockMvc.perform(
                                post("/api/v1/regulation/queries")
                                        .with(auth("zhang", List.of("internal")))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(BODY))
                        .andExpect(request().asyncStarted())
                        .andReturn();
        mockMvc.perform(asyncDispatch(r))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("route_type")))
                .andExpect(content().string(containsString("[stub]")))
                .andExpect(content().string(containsString("finish_reason")));
    }

    @Test
    void unauthorizedSubjectForbidden() throws Exception {
        mockMvc.perform(
                        post("/api/v1/regulation/queries")
                                .with(auth("stranger", List.of("internal")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("B102"));
    }

    @Test
    void unauthenticatedRejected() throws Exception {
        mockMvc.perform(
                        post("/api/v1/regulation/queries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("B101"));
    }
}
