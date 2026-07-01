package com.dfzq.auditai.biz.web;

import com.dfzq.auditai.biz.authz.Authorizer;
import com.dfzq.auditai.biz.authz.FilterResolver;
import com.dfzq.auditai.biz.authz.ForbiddenException;
import com.dfzq.auditai.biz.citation.CitationAssembler;
import com.dfzq.auditai.biz.client.BoundaryClient;
import com.dfzq.auditai.biz.dto.Citation;
import com.dfzq.auditai.biz.dto.Filters;
import com.dfzq.auditai.biz.dto.QuerySubmit;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final AsyncTaskExecutor executor;

    public QueryController(
            Authorizer authorizer,
            FilterResolver filterResolver,
            BoundaryClient boundaryClient,
            CitationAssembler citationAssembler,
            AsyncTaskExecutor executor) {
        this.authorizer = authorizer;
        this.filterResolver = filterResolver;
        this.boundaryClient = boundaryClient;
        this.citationAssembler = citationAssembler;
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

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        executor.execute(() -> stream(emitter, queryId, sessionId, body.question(), filters));
        return emitter;
    }

    private void stream(
            SseEmitter emitter,
            String queryId,
            String sessionId,
            String question,
            Filters filters) {
        List<String> citationKeys = new ArrayList<>();
        try {
            boundaryClient.query(
                    queryId,
                    question,
                    filters,
                    new BoundaryClient.Listener() {
                        @Override
                        public void onMeta(String routeType, boolean reviewRequired) {
                            send(
                                    emitter,
                                    "context",
                                    Map.of(
                                            "query_id",
                                            queryId,
                                            "session_id",
                                            sessionId,
                                            "current_question",
                                            question,
                                            "route_type",
                                            routeType,
                                            "review",
                                            Map.of(
                                                    "required",
                                                    reviewRequired,
                                                    "status",
                                                    reviewRequired ? "pending" : "none")));
                        }

                        @Override
                        public void onDelta(int blockSeq, String blockType, String text) {
                            send(
                                    emitter,
                                    "delta",
                                    Map.of(
                                            "block_seq", blockSeq,
                                            "block_type", blockType,
                                            "content", text));
                        }

                        @Override
                        public void onCitation(String clauseId, String chunkId) {
                            // 契约必填 clause_id 优先(fallback chunk_id)。audit-ai 的 clause_id **就是
                            // chunk_id 的值**
                            // (anchors.py 明写 clause_id(=chunk_id)、r1_evidence.py "clause_id":
                            // c.chunk_id,§7.3),
                            // chunks 表主键即 chunk_id、无独立 clause 列。故按 clause_id 值回查 chunks.chunk_id 即
                            // 「按 clause_id 回查」,恒匹配、不丢引用(clause_id-only 路径见
                            // RegulationQueryCitationIT)。
                            String key =
                                    (clauseId != null && !clauseId.isBlank()) ? clauseId : chunkId;
                            if (key != null && !key.isBlank()) {
                                citationKeys.add(key);
                            }
                        }

                        @Override
                        public void onDone(String finishReason) {
                            // §8.2 Java 收口:按 chunk_id 回查 PG 装配完整引用,发 result(counts + clauses)。
                            List<Citation> clauses = citationAssembler.assemble(citationKeys);
                            send(
                                    emitter,
                                    "result",
                                    Map.of(
                                            "counts",
                                            Map.of("clauses", clauses.size()),
                                            "clauses",
                                            clauses));
                            send(emitter, "done", Map.of("finish_reason", finishReason));
                            emitter.complete();
                        }

                        @Override
                        public void onError(String code, String message) {
                            send(
                                    emitter,
                                    "error",
                                    Map.of("error", Map.of("code", code, "message", message)));
                            emitter.complete();
                        }
                    });
        } catch (RuntimeException e) {
            emitter.completeWithError(e);
        }
    }

    private static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
