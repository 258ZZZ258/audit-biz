package com.orientsec.idap.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientsec.idap.core.mapper.OperationLogMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 查询链路操作留痕。日志写失败不能反向影响只读查询。 */
@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);
    private static final String ACTION = "regulation_query";
    private static final String ACTION_POINT = "制度查询";

    private final OperationLogMapper mapper;
    private final ObjectMapper objectMapper;

    public OperationLogService(OperationLogMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public void accepted(String traceId, String actor, String sessionId) {
        Map<String, Object> detail = baseDetail(sessionId);
        record(traceId, actor, "accepted", detail);
    }

    public void completed(
            String traceId,
            String actor,
            String sessionId,
            int citationCount,
            String finishReason) {
        Map<String, Object> detail = baseDetail(sessionId);
        detail.put("citation_count", citationCount);
        detail.put("finish_reason", finishReason);
        record(traceId, actor, "completed", detail);
    }

    public void failed(
            String traceId, String actor, String sessionId, String code, String message) {
        Map<String, Object> detail = baseDetail(sessionId);
        detail.put("code", code);
        detail.put("message", message);
        record(traceId, actor, "failed", detail);
    }

    private Map<String, Object> baseDetail(String sessionId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("session_id", sessionId);
        return detail;
    }

    private void record(String traceId, String actor, String status, Map<String, Object> detail) {
        try {
            mapper.insert(traceId, actor, ACTION, ACTION_POINT, status, toJson(detail));
        } catch (RuntimeException e) {
            log.warn("操作日志写入失败(traceId={},status={}): {}", traceId, status, e.toString());
        }
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
