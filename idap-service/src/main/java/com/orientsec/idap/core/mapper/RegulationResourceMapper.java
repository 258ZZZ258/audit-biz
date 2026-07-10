package com.orientsec.idap.core.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 前端制度查询明细端点的只读 PG 回查。 */
@Mapper
public interface RegulationResourceMapper {

    @Select(
            "SELECT c.chunk_id AS clause_id, c.text AS full_text,"
                    + " c.clause_path AS clause_path, c.page_start AS page_start,"
                    + " c.page_end AS page_end, dv.title AS doc_title,"
                    + " dv.doc_number AS doc_no, dv.source_filename AS source_filename,"
                    + " dv.doc_version_id AS version,"
                    + " dv.version_status AS status"
                    + " FROM chunks c JOIN doc_versions dv ON c.doc_version_id = dv.doc_version_id"
                    + " WHERE c.chunk_id = #{clauseId}")
    Map<String, Object> findClauseDetail(@Param("clauseId") String clauseId);

    @Select(
            "SELECT ca.doc_version_id AS case_id, dv.title AS case_name,"
                    + " dv.doc_number AS doc_no, dv.source_filename AS source_filename,"
                    + " ca.penalty_org AS regulator, ca.penalty_date AS penalty_date,"
                    + " ca.violation_category AS violation_topic,"
                    + " ca.cited_regulations AS related_regulation"
                    + " FROM cases ca JOIN doc_versions dv ON ca.doc_version_id = dv.doc_version_id"
                    + " WHERE ca.doc_version_id = #{caseId}")
    Map<String, Object> findCaseDetail(@Param("caseId") String caseId);
}
