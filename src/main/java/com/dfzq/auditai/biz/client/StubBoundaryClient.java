package com.dfzq.auditai.biz.client;

import com.dfzq.auditai.biz.dto.Filters;
import org.springframework.stereotype.Component;

/**
 * 边界客户端 stub:回放 canned 事件,让 A 轨在 B 轨(audit-ai FastAPI /v1/query)落地前端到端跑通。
 *
 * <p>I1 用真 HTTP 客户端替换(带 {@code X-Internal-Token} 调 /v1/query 并解析 SSE)。
 */
@Component
public class StubBoundaryClient implements BoundaryClient {

    @Override
    public void query(String question, Filters filters, Listener listener) {
        listener.onMeta("evidence", false);
        listener.onDelta(0, "text", "[stub] 已收到问题:" + question);
        listener.onDelta(0, "text", "(边界真端点 B 轨落地后替换本 stub)");
        listener.onDone("stop");
    }
}
