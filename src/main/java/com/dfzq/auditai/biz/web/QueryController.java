package com.dfzq.auditai.biz.web;

import com.dfzq.auditai.biz.authz.Authorizer;
import com.dfzq.auditai.biz.authz.FilterResolver;
import com.dfzq.auditai.biz.authz.ForbiddenException;
import com.dfzq.auditai.biz.client.BoundaryClient;
import com.dfzq.auditai.biz.dto.Filters;
import com.dfzq.auditai.biz.dto.QuerySubmit;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 制度查询 SSE 端点(frontend.regquery.v1.yaml {@code POST /regulation/queries})。
 *
 * <p><b>同步</b>:jCasbin 授权(越权→B102)+ 预计算 {@link Filters}(校验失败→B2xx),fail-fast; <b>异步</b>:边界客户端(A3
 * stub)流式事件桥接为前端 SSE(context/delta/done)。 {@code result} + PG 引用回查装配在 A4(§8.2 Java 收口)。
 */
@RestController
public class QueryController {

    private static final String OBJ = "regulation_query";
    private static final String ACT = "read";
    private static final long SSE_TIMEOUT_MS = 60_000L;

    private final Authorizer authorizer;
    private final FilterResolver filterResolver;
    private final BoundaryClient boundaryClient;
    private final AsyncTaskExecutor executor;

    public QueryController(
            Authorizer authorizer,
            FilterResolver filterResolver,
            BoundaryClient boundaryClient,
            AsyncTaskExecutor executor) {
        this.authorizer = authorizer;
        this.filterResolver = filterResolver;
        this.boundaryClient = boundaryClient;
        this.executor = executor;
    }

    @PostMapping(value = "/api/v1/regulation/queries", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter query(@AuthenticationPrincipal Jwt jwt, @RequestBody QuerySubmit body) {
        // 同步 fail-fast:先鉴权 + 算过滤值,失败经 GlobalErrorHandler 出 B102/B2xx,不进流式。
        if (!authorizer.permits(jwt.getSubject(), OBJ, ACT)) {
            throw new ForbiddenException("无制度查询权限(" + OBJ + ":" + ACT + ")");
        }
        Filters filters = filterResolver.resolve(jwt); // 可能抛 FilterValidationException → B2xx
        String queryId = UUID.randomUUID().toString();

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        executor.execute(() -> stream(emitter, queryId, body.question(), filters));
        return emitter;
    }

    private void stream(SseEmitter emitter, String queryId, String question, Filters filters) {
        try {
            boundaryClient.query(
                    question,
                    filters,
                    new BoundaryClient.Listener() {
                        @Override
                        public void onMeta(String routeType, boolean reviewRequired) {
                            send(
                                    emitter,
                                    "context",
                                    Map.of(
                                            "query_id", queryId,
                                            "route_type", routeType,
                                            "review_required", reviewRequired));
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
                        public void onDone(String finishReason) {
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
