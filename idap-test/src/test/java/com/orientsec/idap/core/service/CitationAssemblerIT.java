package com.orientsec.idap.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.orientsec.idap.common.model.Citation;
import com.orientsec.idap.core.base.IdapTestServer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** A4 集成:Testcontainers PG 上按 chunk_id 回查 chunks⋈doc_versions 装配完整四级引用(§8.2)。 */
@SpringBootTest(classes = IdapTestServer.class)
@Testcontainers
class CitationAssemblerIT {

    @Container
    static PostgreSQLContainer<?> pg =
            new PostgreSQLContainer<>("postgres:16").withInitScript("citation-it-schema.sql");

    @Autowired private CitationAssembler assembler;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("datasource.idap.jdbc-url", pg::getJdbcUrl);
        registry.add("datasource.idap.username", pg::getUsername);
        registry.add("datasource.idap.password", pg::getPassword);
    }

    @Test
    void assemblesFullCitationByChunkId() {
        List<Citation> cites =
                assembler.assemble(
                        Collections.singletonList("CH0000000000000000000001"),
                        Collections.singletonMap("CH0000000000000000000001", 0.77));

        assertThat(cites).hasSize(1);
        Citation c = cites.get(0);
        assertThat(c.chunkId()).isEqualTo("CH0000000000000000000001");
        assertThat(c.docTitle()).isEqualTo("资管产品适当性管理办法");
        assertThat(c.docNo()).isEqualTo("沪金监规[2024]1号");
        assertThat(c.clausePath()).isEqualTo("第三章/第十条");
        assertThat(c.pageStart()).isEqualTo(12);
        assertThat(c.pageEnd()).isEqualTo(13);
        assertThat(c.status()).isEqualTo("effective");
        assertThat(c.snippet()).contains("客户风险等级");
        assertThat(c.matchScore()).isEqualTo(0.77);
    }

    @Test
    void fallsBackToSourceFilenameWhenTitleIsBlank() {
        List<Citation> cites =
                assembler.assemble(Collections.singletonList("CH0000000000000000000002"));

        assertThat(cites).hasSize(1);
        Citation c = cites.get(0);
        assertThat(c.docTitle()).isEqualTo("北京证券交易所上市公司证券发行注册管理办法");
        assertThat(c.docNo()).isEqualTo("证监会令第211号");
    }

    @Test
    void sortsCitationsByMatchScoreDescending() {
        Map<String, Double> scores = new HashMap<>();
        scores.put("CH0000000000000000000001", 0.12);
        scores.put("CH0000000000000000000002", 0.88);

        List<Citation> cites =
                assembler.assemble(
                        java.util.Arrays.asList(
                                "CH0000000000000000000001", "CH0000000000000000000002"),
                        scores);

        assertThat(cites)
                .extracting(Citation::chunkId)
                .containsExactly("CH0000000000000000000002", "CH0000000000000000000001");
    }

    @Test
    void missingChunkIdDegradesToEmpty() {
        assertThat(assembler.assemble(Collections.singletonList("nonexistent"))).isEmpty();
    }
}
