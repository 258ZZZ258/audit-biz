package com.dfzq.auditai.biz.citation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dfzq.auditai.biz.dto.Citation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** A4 集成:Testcontainers PG 上按 chunk_id 回查 chunks⋈doc_versions 装配完整四级引用(§8.2)。 */
@SpringBootTest
@Testcontainers
class CitationAssemblerIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> pg =
            new PostgreSQLContainer<>("postgres:16").withInitScript("citation-it-schema.sql");

    @Autowired private CitationAssembler assembler;

    @Test
    void assemblesFullCitationByChunkId() {
        List<Citation> cites = assembler.assemble(List.of("CH0000000000000000000001"));

        assertThat(cites).hasSize(1);
        Citation c = cites.get(0);
        assertThat(c.chunkId()).isEqualTo("CH0000000000000000000001");
        assertThat(c.docTitle()).isEqualTo("资管产品适当性管理办法");
        assertThat(c.docNo()).isEqualTo("沪金监规[2024]1号");
        assertThat(c.clausePath()).isEqualTo("第三章/第十条");
        assertThat(c.pageStart()).isEqualTo(12);
        assertThat(c.pageEnd()).isEqualTo(13);
        assertThat(c.status()).isEqualTo("effective");
    }

    @Test
    void missingChunkIdDegradesToEmpty() {
        assertThat(assembler.assemble(List.of("nonexistent"))).isEmpty();
    }
}
