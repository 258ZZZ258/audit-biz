package com.orientsec.idap.core.client;

import com.orientsec.idap.common.model.Filters;

/**
 * audit-ai 边界客户端(边界二:单向、无身份、共享密钥,boundary.v1.yaml /v1/query)。
 *
 * <p>边界只承载普通字段(filters + question + request_id)和共享密钥,不传用户身份/JWT。 引用四级回查装配(§8.2 Java 收口)在
 * biz;RAG/路由/重排/生成全在 audit-ai。
 */
public interface BoundaryClient {

    /**
     * @param requestId 契约必填(boundary.v1.yaml QueryRequest.request_id):与前端 query_id 同值, 由 audit-ai
     *     回显并注入 Langfuse trace,保前端 query_id ≡ 边界 request_id ≡ 观测 trace 一条链。
     */
    void query(
            String requestId,
            String question,
            Filters filters,
            QueryOptions options,
            Listener listener);

    /** 边界流式事件回调(对应 boundary.v1.yaml 的 SSE 事件)。 */
    interface Listener {
        void onMeta(
                String routeType, boolean aiLabel, boolean reviewRequired, Boolean exportEnabled);

        void onDelta(int blockSeq, String blockType, String text);

        /** 轻量引用标识(§8.2):biz 收集后按 clause_id/chunk_id 回查 PG 装配完整 citation。 */
        void onCitation(String clauseId, String chunkId, Double confidence, Boolean aiLabel);

        void onDone(String finishReason, Double confidence, java.util.List<String> exhaustedScope);

        void onError(String code, String message);
    }

    class QueryOptions {
        private final Boolean includeSuperseded;

        public QueryOptions(Boolean includeSuperseded) {
            this.includeSuperseded = includeSuperseded;
        }

        public Boolean includeSuperseded() {
            return includeSuperseded;
        }
    }
}
