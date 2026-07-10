package com.orientsec.idap.core.controller;

import com.orientsec.idap.common.model.Citation;
import com.orientsec.idap.common.model.Filters;
import com.orientsec.idap.common.model.QuerySubmit;
import com.orientsec.idap.core.authz.Authorizer;
import com.orientsec.idap.core.authz.FilterResolver;
import com.orientsec.idap.core.authz.ForbiddenException;
import com.orientsec.idap.core.client.BoundaryClient;
import com.orientsec.idap.core.client.BoundaryClient.QueryOptions;
import com.orientsec.idap.core.service.CitationAssembler;
import com.orientsec.idap.core.service.OperationLogService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.validation.Valid;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 制度查询 SSE 端点(frontend.regquery.v1.yaml {@code POST /regulation/queries})。
 *
 * <p><b>同步</b>:入口校验(@Valid→B2xx)+ jCasbin 授权(越权→B102)+ 预计算 {@link Filters}(校验失败→B2xx),fail-fast;
 * <b>异步</b>:边界客户端(A3 stub)流式事件桥接为前端 SSE(context/delta/done)。 {@code result} + PG 引用回查装配在 A4(§8.2
 * Java 收口)。
 */
@RestController
public class QueryController {

    private static final String OBJ = "regulation_query";
    private static final String ACT = "read";
    private static final long SSE_TIMEOUT_MS = 60_000L;

    private final Authorizer authorizer;
    private final FilterResolver filterResolver;
    private final BoundaryClient boundaryClient;
    private final CitationAssembler citationAssembler;
    private final OperationLogService operationLogService;
    private final AsyncTaskExecutor executor;

    public QueryController(
            Authorizer authorizer,
            FilterResolver filterResolver,
            BoundaryClient boundaryClient,
            CitationAssembler citationAssembler,
            OperationLogService operationLogService,
            AsyncTaskExecutor executor) {
        this.authorizer = authorizer;
        this.filterResolver = filterResolver;
        this.boundaryClient = boundaryClient;
        this.citationAssembler = citationAssembler;
        this.operationLogService = operationLogService;
        this.executor = executor;
    }

    @PostMapping(value = "/api/v1/regulation/queries", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter query(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody QuerySubmit body) {
        // 同步 fail-fast:校验(@Valid)→ 鉴权 → 算过滤值,失败经 GlobalErrorHandler 出 B2xx/B102,不进流式。
        if (!authorizer.permits(jwt.getSubject(), OBJ, ACT)) {
            throw new ForbiddenException("无制度查询权限(" + OBJ + ":" + ACT + ")");
        }
        Filters filters = filterResolver.resolve(jwt); // 可能抛 FilterValidationException → B2xx
        // 前端 query_id ≡ 边界 request_id ≡ audit-ai Langfuse trace(§3.1),一处生成、一处透传。
        String queryId = UUID.randomUUID().toString();
        String sessionId = StringUtils.hasText(body.sessionId()) ? body.sessionId() : queryId;
        String actor = jwt.getSubject();
        operationLogService.accepted(queryId, actor, sessionId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        executor.execute(
                () ->
                        stream(
                                emitter,
                                queryId,
                                sessionId,
                                actor,
                                body.question(),
                                filters,
                                options(body.options())));
        return emitter;
    }

    private static QueryOptions options(QuerySubmit.Options options) {
        return new QueryOptions(options == null ? null : options.includeSuperseded());
    }

    private void stream(
            SseEmitter emitter,
            String queryId,
            String sessionId,
            String actor,
            String question,
            Filters filters,
            QueryOptions options) {
        List<String> citationKeys = new ArrayList<>();
        Map<String, Double> confidencesByCitationKey = new LinkedHashMap<>();
        boolean[] reviewRequired = {false};
        boolean[] aiLabel = {true};
        Boolean[] exportEnabled = {null};
        Double[] responseConfidence = {null};
        AtomicReference<List<String>> exhaustedScope =
                new AtomicReference<>(Collections.emptyList());
        try {
            boundaryClient.query(
                    queryId,
                    question,
                    filters,
                    options,
                    new BoundaryClient.Listener() {
                        @Override
                        public void onMeta(
                                String routeType,
                                boolean receivedAiLabel,
                                boolean required,
                                Boolean receivedExportEnabled) {
                            reviewRequired[0] = required;
                            aiLabel[0] = receivedAiLabel;
                            exportEnabled[0] = receivedExportEnabled;
                            QueryController.this.sendContext(
                                    emitter, queryId, sessionId, question, routeType, required);
                        }

                        @Override
                        public void onDelta(int blockSeq, String blockType, String text) {
                            send(emitter, "delta", deltaPayload(blockSeq, blockType, text));
                        }

                        @Override
                        public void onCitation(
                                String clauseId,
                                String chunkId,
                                Double confidence,
                                Boolean citationAiLabel) {
                            // 契约必填 clause_id 优先(fallback chunk_id)。audit-ai 的 clause_id **就是
                            // chunk_id 的值**
                            // (anchors.py 明写 clause_id(=chunk_id)、r1_evidence.py "clause_id":
                            // c.chunk_id,§7.3),
                            // chunks 表主键即 chunk_id、无独立 clause 列。故按 clause_id 值回查 chunks.chunk_id 即
                            // 「按 clause_id 回查」,恒匹配、不丢引用(clause_id-only 路径见
                            // RegulationQueryCitationIT)。
                            String key =
                                    (clauseId != null && !clauseId.trim().isEmpty())
                                            ? clauseId
                                            : chunkId;
                            if (key != null && !key.trim().isEmpty()) {
                                citationKeys.add(key);
                                if (confidence != null) {
                                    confidencesByCitationKey.put(key, confidence);
                                }
                            }
                        }

                        @Override
                        public void onDone(
                                String finishReason,
                                Double confidence,
                                List<String> receivedExhaustedScope) {
                            responseConfidence[0] = confidence;
                            exhaustedScope.set(
                                    receivedExhaustedScope == null
                                            ? Collections.emptyList()
                                            : receivedExhaustedScope);
                            // §8.2 Java 收口:按 chunk_id 回查 PG 装配完整引用,发 result(counts + clauses)。
                            List<Citation> clauses =
                                    citationAssembler.assemble(
                                            citationKeys, confidencesByCitationKey);
                            operationLogService.completed(
                                    queryId, actor, sessionId, clauses.size(), finishReason);
                            send(
                                    emitter,
                                    "result",
                                    resultPayload(
                                            clauses,
                                            reviewRequired[0],
                                            aiLabel[0],
                                            responseConfidence[0],
                                            exportEnabled[0],
                                            exhaustedScope.get()));
                            send(emitter, "done", singleton("finish_reason", finishReason));
                            emitter.complete();
                        }

                        @Override
                        public void onError(String code, String message) {
                            operationLogService.failed(queryId, actor, sessionId, code, message);
                            send(emitter, "error", singleton("error", errorPayload(code, message)));
                            emitter.complete();
                        }
                    });
        } catch (RuntimeException e) {
            operationLogService.failed(queryId, actor, sessionId, "E500", e.getMessage());
            emitter.completeWithError(e);
        }
    }

    private void sendContext(
            SseEmitter emitter,
            String queryId,
            String sessionId,
            String question,
            String routeType,
            boolean required) {
        send(emitter, "context", contextPayload(queryId, sessionId, question, routeType, required));
    }

    private static Map<String, Object> resultPayload(
            List<Citation> clauses,
            boolean reviewRequired,
            boolean aiLabel,
            Double confidence,
            Boolean exportEnabled,
            List<String> exhaustedScope) {
        List<Map<String, Object>> regulations = regulationsFromClauses(clauses);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("counts", countsPayload(regulations.size(), clauses.size(), 0, 0));
        result.put("regulations", regulations);
        result.put("clauses", clauses);
        result.put("rules", Collections.emptyList());
        result.put("cases", Collections.emptyList());
        result.put("case_insights", Collections.emptyList());
        result.put("citation_advice", Collections.emptyList());
        result.put("regulatory_digest", Collections.emptyList());
        result.put("suggested_followups", Collections.emptyList());
        result.put("review_required", reviewRequired);
        result.put("ai_label", aiLabel);
        result.put("confidence", confidence);
        result.put("export_enabled", exportEnabled);
        result.put("exhausted_scope", exhaustedScope);
        return result;
    }

    private static List<Map<String, Object>> regulationsFromClauses(List<Citation> clauses) {
        Map<String, Map<String, Object>> byDoc = new LinkedHashMap<>();
        for (Citation clause : clauses) {
            String docId = clause.version() != null ? clause.version() : clause.docTitle();
            if (docId == null || docId.trim().isEmpty()) {
                continue;
            }
            Map<String, Object> hit =
                    byDoc.computeIfAbsent(
                            docId,
                            ignored -> {
                                Map<String, Object> value = new LinkedHashMap<>();
                                value.put("doc_id", docId);
                                value.put("doc_title", clause.docTitle());
                                value.put("doc_no", clause.docNo());
                                value.put("match_score", clause.matchScore());
                                value.put("clause_excerpt", clause.snippet());
                                value.put("version", clause.version());
                                value.put("status", clause.status());
                                value.put("hit_clause_count", 0);
                                return value;
                            });
            hit.put("hit_clause_count", ((Integer) hit.get("hit_clause_count")) + 1);
            Double currentScore = (Double) hit.get("match_score");
            if (clause.matchScore() != null
                    && (currentScore == null || clause.matchScore() > currentScore)) {
                hit.put("match_score", clause.matchScore());
            }
        }
        List<Map<String, Object>> regulations = new ArrayList<>(byDoc.values());
        regulations.sort(QueryController::compareByMatchScoreDesc);
        return regulations;
    }

    @SuppressWarnings("unchecked")
    private static int compareByMatchScoreDesc(
            Map<String, Object> left, Map<String, Object> right) {
        return Comparator.nullsLast(Comparator.<Double>naturalOrder().reversed())
                .compare((Double) left.get("match_score"), (Double) right.get("match_score"));
    }

    private static Map<String, Object> contextPayload(
            String queryId, String sessionId, String question, String routeType, boolean required) {
        Map<String, Object> review = new LinkedHashMap<>();
        review.put("required", required);
        review.put("status", required ? "pending" : "none");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query_id", queryId);
        payload.put("session_id", sessionId);
        payload.put("current_question", question);
        payload.put("route_type", routeType);
        payload.put("review", review);
        return payload;
    }

    private static Map<String, Object> deltaPayload(int blockSeq, String blockType, String text) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("block_seq", blockSeq);
        payload.put("block_type", blockType);
        payload.put("content", text);
        return payload;
    }

    private static Map<String, Object> countsPayload(
            int regulations, int clauses, int rules, int cases) {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("regulations", regulations);
        counts.put("clauses", clauses);
        counts.put("rules", rules);
        counts.put("cases", cases);
        return counts;
    }

    private static Map<String, Object> errorPayload(String code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        return error;
    }

    private static Map<String, Object> singleton(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    private static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
