package com.dfzq.auditai.biz.citation;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 引用回查 mapper:按 chunk_id 批量读 chunks⋈doc_versions(audit-ai Alembic 管 schema,biz 只读)。
 *
 * <p>返回行 Map(列标签为 key);由 {@link CitationAssembler} 组装为 {@code Citation}。
 */
@Mapper
public interface CitationMapper {

    @Select({
        "<script>",
        "SELECT c.chunk_id AS chunk_id, c.clause_path AS clause_path,",
        "       c.page_start AS page_start, c.page_end AS page_end,",
        "       dv.title AS doc_title, dv.doc_number AS doc_no,",
        "       dv.doc_version_id AS version, dv.version_status AS status",
        "  FROM chunks c JOIN doc_versions dv ON c.doc_version_id = dv.doc_version_id",
        " WHERE c.chunk_id IN",
        " <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    List<Map<String, Object>> findByChunkIds(@Param("ids") List<String> ids);
}
