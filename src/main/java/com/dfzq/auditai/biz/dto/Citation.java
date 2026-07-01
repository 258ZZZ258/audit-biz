package com.dfzq.auditai.biz.dto;

/**
 * 完整四级引用(§7.3 / §8.2 Java 收口):biz 按 chunk_id 回查 PG chunks⋈doc_versions 装配,交前端。
 *
 * <p>注:audit-ai chunks 表主键即 {@code chunk_id}(无独立 clause 列),故回查主键为 chunk_id; 契约 §8.1 写的 clause_id
 * 在此等同 chunk_id(clause_id vs chunk_id 待与边界契约对齐,见 devlog)。
 */
public record Citation(
        String chunkId,
        String clauseId,
        String docTitle,
        String docNo,
        String clausePath,
        Integer pageStart,
        Integer pageEnd,
        String version,
        String status) {}
