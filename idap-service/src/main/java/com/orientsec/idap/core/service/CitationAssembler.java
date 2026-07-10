package com.orientsec.idap.core.service;

import com.orientsec.idap.common.model.Citation;
import com.orientsec.idap.core.mapper.CitationMapper;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private static final Map<String, Double> NO_SCORES = Collections.emptyMap();

    public CitationAssembler(CitationMapper mapper) {
        this.mapper = mapper;
    }

    public List<Citation> assemble(List<String> chunkIds) {
        return assemble(chunkIds, NO_SCORES);
    }

    public List<Citation> assemble(List<String> chunkIds, Map<String, Double> scoresByChunkId) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return mapper.findByChunkIds(chunkIds).stream()
                    .map(row -> toCitation(row, scoresByChunkId))
                    .sorted(CitationAssembler::compareByMatchScoreDesc)
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            log.warn("引用回查失败,降级为空引用(chunkIds={}): {}", chunkIds.size(), e.toString());
            return Collections.emptyList();
        }
    }

    private static int compareByMatchScoreDesc(Citation left, Citation right) {
        return Comparator.nullsLast(Comparator.<Double>naturalOrder().reversed())
                .compare(left.matchScore(), right.matchScore());
    }

    private static Citation toCitation(
            Map<String, Object> row, Map<String, Double> scoresByChunkId) {
        String chunkId = str(row.get("chunk_id"));
        String docNo = str(row.get("doc_no"));
        return new Citation(
                chunkId,
                chunkId, // clause_id ≡ chunk_id(真 schema 无独立 clause 列)
                docTitle(row, docNo),
                docNo,
                str(row.get("clause_path")),
                intOrNull(row.get("page_start")),
                intOrNull(row.get("page_end")),
                str(row.get("version")),
                str(row.get("status")),
                str(row.get("snippet")),
                scoresByChunkId.get(chunkId),
                null);
    }

    private static String docTitle(Map<String, Object> row, String docNo) {
        String title = blankToNull(str(row.get("doc_title")));
        if (title != null) {
            return title;
        }
        return titleFromSourceFilename(str(row.get("source_filename")), docNo);
    }

    private static String titleFromSourceFilename(String sourceFilename, String docNo) {
        String name = blankToNull(sourceFilename);
        if (name == null) {
            return null;
        }
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceFirst("^D\\d+_\\d+_+", "");
        while (name.matches("(?i).+\\.(pdf|docx?|xlsx?)$")) {
            name = name.replaceFirst("(?i)\\.(pdf|docx?|xlsx?)$", "");
        }
        name = name.replace('_', ' ').trim().replaceAll("\\s+", " ");
        String normalizedDocNo = blankToNull(docNo);
        if (normalizedDocNo != null && name.startsWith(normalizedDocNo + " ")) {
            name = name.substring(normalizedDocNo.length()).trim();
        }
        return blankToNull(name);
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Clob) {
            try {
                Clob clob = (Clob) o;
                return clob.getSubString(1L, (int) clob.length());
            } catch (SQLException e) {
                throw new IllegalStateException("读取引用文本字段失败", e);
            }
        }
        return o.toString();
    }

    private static Integer intOrNull(Object o) {
        return o == null ? null : ((Number) o).intValue();
    }
}
