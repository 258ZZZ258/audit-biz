package com.orientsec.idap.core.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orientsec.idap.core.base.IdapTestServer;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** 前端制度查询契约明细端点与 v1 占位端点。 */
@SpringBootTest(classes = IdapTestServer.class)
@AutoConfigureMockMvc
@Sql(
        statements = {
            "DROP TABLE IF EXISTS cases",
            "DROP TABLE IF EXISTS chunks",
            "DROP TABLE IF EXISTS doc_versions",
            "CREATE TABLE doc_versions (doc_version_id varchar(26) PRIMARY KEY,"
                    + " title varchar(512), doc_number varchar(128), issuer varchar(128),"
                    + " source_filename varchar(512),"
                    + " issue_date date, effective_date date, version_status varchar(16))",
            "CREATE TABLE chunks (chunk_id varchar(24) PRIMARY KEY,"
                    + " doc_version_id varchar(26), clause_path varchar(512),"
                    + " page_start int, page_end int, text clob)",
            "CREATE TABLE cases (doc_version_id varchar(26) PRIMARY KEY,"
                    + " penalty_org varchar(256), penalty_date date, violation_category varchar(64),"
                    + " cited_regulations varchar(512))",
            "INSERT INTO doc_versions (doc_version_id, title, doc_number, issuer,"
                    + " source_filename, issue_date, effective_date, version_status)"
                    + " VALUES ('DV01', '资管产品适当性管理办法', '沪金监规[2024]1号',"
                    + " '上海金融监管局', 'D0001_001__沪金监规[2024]1号_资管产品适当性管理办法.pdf',"
                    + " DATE '2024-01-02', DATE '2024-02-01', 'effective')",
            "INSERT INTO chunks (chunk_id, doc_version_id, clause_path, page_start, page_end, text)"
                    + " VALUES ('CH0000000000000000000001', 'DV01', '第三章/第十条', 12, 13,"
                    + " '金融机构应当及时更新客户风险等级。')",
            "INSERT INTO doc_versions (doc_version_id, title, doc_number, issuer,"
                    + " source_filename, issue_date, effective_date, version_status)"
                    + " VALUES ('CASE01', '某证券公司适当性管理处罚案', '沪罚字[2024]9号',"
                    + " '上海金融监管局', 'D0002_001__沪罚字[2024]9号_某证券公司适当性管理处罚案.pdf',"
                    + " DATE '2024-03-01', DATE '2024-03-01', 'effective')",
            "INSERT INTO cases (doc_version_id, penalty_org, penalty_date,"
                    + " violation_category, cited_regulations)"
                    + " VALUES ('CASE01', '上海金融监管局', DATE '2024-03-08',"
                    + " '适当性管理', '资管产品适当性管理办法')"
        })
class RegulationResourceControllerTest {

    @Autowired private MockMvc mockMvc;

    private RequestPostProcessor auth() {
        return jwt().jwt(
                        j ->
                                j.subject("zhang")
                                        .claim("perm_tags", Collections.emptyList())
                                        .claim(
                                                "corpus_scope",
                                                Collections.singletonList("internal")));
    }

    @Test
    void getsClauseDetail() throws Exception {
        mockMvc.perform(get("/api/v1/regulation/clauses/CH0000000000000000000001").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.clause_id").value("CH0000000000000000000001"))
                .andExpect(jsonPath("$.data.doc_title").value("资管产品适当性管理办法"))
                .andExpect(jsonPath("$.data.full_text").value("金融机构应当及时更新客户风险等级。"))
                .andExpect(
                        jsonPath("$.data.deep_link")
                                .value("/api/v1/regulation/clauses/CH0000000000000000000001"));
    }

    @Test
    void missingClauseReturns404Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/regulation/clauses/missing").with(auth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("B404"));
    }

    @Test
    void getsCaseDetail() throws Exception {
        mockMvc.perform(get("/api/v1/regulation/cases/CASE01").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.case_id").value("CASE01"))
                .andExpect(jsonPath("$.data.case_name").value("某证券公司适当性管理处罚案"))
                .andExpect(jsonPath("$.data.regulator").value("上海金融监管局"))
                .andExpect(jsonPath("$.data.penalty_date").value("2024-03-08"))
                .andExpect(jsonPath("$.data.violation_topic").value("适当性管理"))
                .andExpect(jsonPath("$.data.related_regulation").value("资管产品适当性管理办法"));
    }

    @Test
    void placeholderEndpointsReturn501Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/regulation/sessions").with(auth()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error.code").value("B501"));
        mockMvc.perform(get("/api/v1/regulation/sessions/S1").with(auth()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error.code").value("B501"));
        mockMvc.perform(get("/api/v1/regulation/queries/Q1/audit-trail").with(auth()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error.code").value("B501"));
        mockMvc.perform(get("/api/v1/regulation/queries/Q1/permission-trail").with(auth()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error.code").value("B501"));
        mockMvc.perform(post("/api/v1/regulation/queries/Q1/export").with(auth()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error.code").value("B501"));
        mockMvc.perform(
                        post("/api/v1/files")
                                .with(auth())
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error.code").value("B501"));
    }
}
