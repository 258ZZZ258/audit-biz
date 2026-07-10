package com.orientsec.idap.core.service;

import com.orientsec.idap.common.exception.NotFoundException;
import com.orientsec.idap.common.model.CaseDetail;
import com.orientsec.idap.common.model.ClauseDetail;
import com.orientsec.idap.core.mapper.RegulationResourceMapper;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RegulationResourceService {

    private static final String CLAUSE_DEEP_LINK_PREFIX = "/api/v1/regulation/clauses/";

    private final RegulationResourceMapper mapper;

    public RegulationResourceService(RegulationResourceMapper mapper) {
        this.mapper = mapper;
    }

    public ClauseDetail getClauseDetail(String clauseId) {
        Map<String, Object> row = mapper.findClauseDetail(clauseId);
        if (row == null || row.isEmpty()) {
            throw new NotFoundException("条款不存在:" + clauseId);
        }
        return new ClauseDetail(
                str(row, "clause_id"),
                title(row, "doc_title"),
                str(row, "doc_no"),
                str(row, "clause_path"),
                str(row, "full_text"),
                intOrNull(row, "page_start"),
                intOrNull(row, "page_end"),
                str(row, "version"),
                str(row, "status"),
                CLAUSE_DEEP_LINK_PREFIX + clauseId);
    }

    public CaseDetail getCaseDetail(String caseId) {
        Map<String, Object> row = mapper.findCaseDetail(caseId);
        if (row == null || row.isEmpty()) {
            throw new NotFoundException("案例不存在:" + caseId);
        }
        return new CaseDetail(
                str(row, "case_id"),
                title(row, "case_name"),
                str(row, "regulator"),
                str(row, "penalty_date"),
                str(row, "violation_topic"),
                str(row, "related_regulation"),
                null,
                null,
                null,
                null);
    }

    private static String title(Map<String, Object> row, String titleKey) {
        String title = blankToNull(str(row, titleKey));
        if (title != null) {
            return title;
        }
        return titleFromSourceFilename(str(row, "source_filename"), str(row, "doc_no"));
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

    private static String str(Map<String, Object> row, String key) {
        Object value = value(row, key);
        return str(value);
    }

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Clob) {
            try {
                Clob clob = (Clob) value;
                return clob.getSubString(1L, (int) clob.length());
            } catch (SQLException e) {
                throw new IllegalStateException("读取文本字段失败", e);
            }
        }
        return value.toString();
    }

    private static Integer intOrNull(Map<String, Object> row, String key) {
        Object value = value(row, key);
        return value == null ? null : ((Number) value).intValue();
    }

    private static Object value(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
