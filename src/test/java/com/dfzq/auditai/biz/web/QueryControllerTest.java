package com.dfzq.auditai.biz.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dfzq.auditai.biz.client.BoundaryClient;
import com.dfzq.auditai.biz.dto.Filters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** A3 验收:制度查询 SSE —— context 契约形态 · request_id 下传 · 入参校验 B2xx · 越权 B102 · 未认证 B101。 */
@SpringBootTest
@AutoConfigureMockMvc
class QueryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private CapturingBoundaryClient boundary;

    private RequestPostProcessor auth(String sub, List<String> corpus) {
        return jwt().jwt(
                        j ->
                                j.subject(sub)
                                        .claim("perm_tags", List.of())
                                        .claim("corpus_scope", corpus));
    }

    @Test
    void authorizedQueryStreamsContractCompliantContext() throws Exception {
        MvcResult r =
                mockMvc.perform(
                                post("/api/v1/regulation/queries")
                                        .with(auth("zhang", List.of("internal")))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"question\":\"客户风险等级更新不及时的案例?\"}"))
                        .andExpect(request().asyncStarted())
                        .andReturn();
        String sse =
                mockMvc.perform(asyncDispatch(r))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        JsonNode ctx = eventData(sse, "context");
        // ContextEvent 必填(frontend.regquery.v1.yaml)
        assertThat(ctx.hasNonNull("query_id")).isTrue();
        assertThat(ctx.hasNonNull("session_id")).isTrue();
        assertThat(ctx.get("current_question").asText()).contains("客户风险等级");
        assertThat(ctx.hasNonNull("route_type")).isTrue();
        assertThat(ctx.path("review").path("required").isBoolean()).isTrue();
        // request_id 下传 = 前端 query_id(链路不断,BOUNDARY-REQUEST-ID-001)
        assertThat(boundary.lastRequestId).isEqualTo(ctx.get("query_id").asText());
        // A4:result 事件存在(capturing 不发 citation → 空引用降级,counts.clauses=0)
        assertThat(sse).contains("event:result");
        assertThat(eventData(sse, "result").path("counts").path("clauses").asInt()).isZero();
        assertThat(sse).contains("event:done");
    }

    @Test
    void missingQuestionRejectedAsB201() throws Exception {
        mockMvc.perform(
                        post("/api/v1/regulation/queries")
                                .with(auth("zhang", List.of("internal")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("B201"));
    }

    @Test
    void invalidJsonRejectedAsB201() throws Exception {
        mockMvc.perform(
                        post("/api/v1/regulation/queries")
                                .with(auth("zhang", List.of("internal")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{bad"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("B201"));
    }

    @Test
    void unauthorizedSubjectForbidden() throws Exception {
        mockMvc.perform(
                        post("/api/v1/regulation/queries")
                                .with(auth("stranger", List.of("internal")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"question\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("B102"));
    }

    @Test
    void unauthenticatedRejected() throws Exception {
        mockMvc.perform(
                        post("/api/v1/regulation/queries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"question\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("B101"));
    }

    /** 从 SSE 文本取指定 event 的 data JSON。 */
    private JsonNode eventData(String sse, String event) throws Exception {
        String[] lines = sse.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String ev = lines[i].strip();
            if (ev.startsWith("event:") && ev.substring(6).strip().equals(event)) {
                for (int j = i + 1; j < lines.length; j++) {
                    String l = lines[j].strip();
                    if (l.startsWith("data:")) {
                        return mapper.readTree(l.substring(l.indexOf(':') + 1).strip());
                    }
                }
            }
        }
        throw new AssertionError("SSE 无 " + event + " 事件:\n" + sse);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        CapturingBoundaryClient capturingBoundaryClient() {
            return new CapturingBoundaryClient();
        }
    }

    /** 捕获 request_id 的测试替身,回放同 stub 的事件。 */
    static class CapturingBoundaryClient implements BoundaryClient {
        volatile String lastRequestId;

        @Override
        public void query(String requestId, String question, Filters filters, Listener l) {
            this.lastRequestId = requestId;
            l.onMeta("judgmental", true);
            l.onDelta(0, "text", "[stub] " + question);
            l.onDone("stop");
        }
    }
}
