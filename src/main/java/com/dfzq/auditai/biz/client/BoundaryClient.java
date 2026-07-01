package com.dfzq.auditai.biz.client;

import com.dfzq.auditai.biz.dto.Filters;

/**
 * audit-ai 边界客户端(边界二:单向、无身份、共享密钥,boundary.v1.yaml /v1/query)。
 *
 * <p>A3 用 {@link StubBoundaryClient};真 HTTP 客户端(SSE 解析)在 I1 接入。流式事件经 {@link Listener} 回推。
 * 引用四级回查装配(§8.2 Java 收口)在 A4;A3 只桥接 meta/delta/done。
 */
public interface BoundaryClient {

    /**
     * @param requestId 契约必填(boundary.v1.yaml QueryRequest.request_id):与前端 query_id 同值, 由 audit-ai
     *     回显并注入 Langfuse trace,保前端 query_id ≡ 边界 request_id ≡ 观测 trace 一条链。
     */
    void query(String requestId, String question, Filters filters, Listener listener);

    /** 边界流式事件回调(对应 boundary.v1.yaml 的 SSE 事件)。 */
    interface Listener {
        void onMeta(String routeType, boolean reviewRequired);

        void onDelta(int blockSeq, String blockType, String text);

        void onDone(String finishReason);

        void onError(String code, String message);
    }
}
