package com.dfzq.auditai.biz.citation;

import com.dfzq.auditai.biz.dto.Citation;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 引用四级回查装配(§8.2 Java 收口):按 chunk_id 回查 PG,把 audit-ai 的轻量引用装成完整 {@link Citation}。
 *
 * <p>热路径韧性:无 id 短路;回查失败(PG 不可达)/命中不到 → **降级为空引用,不崩**(答案仍可回)。
 */
@Component
public class CitationAssembler {

    private static final Logger log = LoggerFactory.getLogger(CitationAssembler.class);

    private final CitationMapper mapper;

    public CitationAssembler(CitationMapper mapper) {
        this.mapper = mapper;
    }

    public List<Citation> assemble(List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return List.of();
        }
        try {
            return mapper.findByChunkIds(chunkIds).stream()
                    .map(CitationAssembler::toCitation)
                    .toList();
        } catch (RuntimeException e) {
            log.warn("引用回查失败,降级为空引用(chunkIds={}): {}", chunkIds.size(), e.toString());
            return List.of();
        }
    }

    private static Citation toCitation(Map<String, Object> row) {
        return new Citation(
                str(row.get("chunk_id")),
                str(row.get("chunk_id")), // clause_id ≡ chunk_id(真 schema 无独立 clause 列)
                str(row.get("doc_title")),
                str(row.get("doc_no")),
                str(row.get("clause_path")),
                intOrNull(row.get("page_start")),
                intOrNull(row.get("page_end")),
                str(row.get("version")),
                str(row.get("status")));
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static Integer intOrNull(Object o) {
        return o == null ? null : ((Number) o).intValue();
    }
}
