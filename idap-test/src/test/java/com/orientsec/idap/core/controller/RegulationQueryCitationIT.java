package com.orientsec.idap.core.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientsec.idap.core.base.IdapTestServer;
import com.orientsec.idap.core.client.BoundaryClient;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * A4 契约收口(BOUNDARY-CITATION-KEY-001):边界**只发 clause_id**(chunk_id 缺失)时,result 仍装配出完整四级引用 ——证明「按
 * clause_id 回查」成立(clause_id 值 = chunk_id,§7.3),合法引用不丢。
 */
@SpringBootTest(classes = IdapTestServer.class)
@AutoConfigureMockMvc
@Import(RegulationQueryCitationIT.Cfg.class)
@Testcontainers
class RegulationQueryCitationIT {

    @Container
    static PostgreSQLContainer<?> pg =
            new PostgreSQLContainer<>("postgres:16").withInitScript("citation-it-schema.sql");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("datasource.idap.jdbc-url", pg::getJdbcUrl);
        registry.add("datasource.idap.username", pg::getUsername);
        registry.add("datasource.idap.password", pg::getPassword);
    }

    @Test
    void resolvesFullCitationFromClauseIdOnly() throws Exception {
        MvcResult r =
                mockMvc.perform(
                                post("/api/v1/regulation/queries")
                                        .with(
                                                jwt().jwt(
                                                                j ->
                                                                        j.subject("zhang")
                                                                                .claim(
                                                                                        "perm_tags",
                                                                                        Collections
                                                                                                .emptyList())
                                                                                .claim(
                                                                                        "corpus_scope",
                                                                                        Collections
                                                                                                .singletonList(
                                                                                                        "internal"))))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"question\":\"客户适当性依据?\"}"))
                        .andExpect(request().asyncStarted())
                        .andReturn();
        String sse =
                mockMvc.perform(asyncDispatch(r))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        JsonNode result = eventData(sse, "result");
        assertThat(result.path("counts").path("regulations").asInt()).isEqualTo(1);
        assertThat(result.path("counts").path("clauses").asInt()).isEqualTo(1);
        assertThat(result.path("regulations").get(0).path("hit_clause_count").asInt()).isEqualTo(1);
        JsonNode c0 = result.path("clauses").get(0);
        assertThat(c0.path("doc_title").asText()).isEqualTo("资管产品适当性管理办法");
        assertThat(c0.path("clause_path").asText()).isEqualTo("第三章/第十条");
        assertThat(c0.path("status").asText()).isEqualTo("effective");
        assertThat(c0.path("snippet").asText()).contains("客户风险等级");
        assertThat(c0.path("match_score").asDouble()).isEqualTo(0.91);
    }

    private JsonNode eventData(String sse, String event) throws Exception {
        String[] lines = sse.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String ev = lines[i].trim();
            if (ev.startsWith("event:") && ev.substring(6).trim().equals(event)) {
                for (int j = i + 1; j < lines.length; j++) {
                    String d = lines[j].trim();
                    if (d.startsWith("data:")) {
                        return mapper.readTree(d.substring(d.indexOf(':') + 1).trim());
                    }
                }
            }
        }
        throw new AssertionError("SSE 无 " + event + " 事件:\n" + sse);
    }

    /** 边界桩:只发 clause_id(= seed 的 chunk_id 值),chunk_id 留空 —— 模拟契约合法的 clause_id-only 引用。 */
    @TestConfiguration
    static class Cfg {
        @Bean
        @Primary
        BoundaryClient clauseIdOnlyStub() {
            return (requestId, question, filters, options, l) -> {
                l.onMeta("evidence", true, false, false);
                l.onCitation("CH0000000000000000000001", null, 0.91, true);
                l.onDone("stop", 0.91, java.util.Collections.emptyList());
            };
        }
    }
}
