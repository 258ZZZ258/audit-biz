package com.orientsec.idap.core.client;

import com.orientsec.idap.common.model.Filters;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 边界客户端 stub:仅测试/本地显式打开时回放契约事件。生产默认走 {@link HttpBoundaryClient}。 */
@Component
@ConditionalOnProperty(name = "audit.ai.stub.enabled", havingValue = "true")
public class StubBoundaryClient implements BoundaryClient {

    @Override
    public void query(
            String requestId,
            String question,
            Filters filters,
            QueryOptions options,
            Listener listener) {
        listener.onMeta("evidence", true, false, false);
        listener.onDelta(0, "text", "本地边界桩输出。");
        listener.onCitation("stub-clause-1", "stub-chunk-000000000001", 0.82, true);
        listener.onDone("stop", 0.82, java.util.Collections.emptyList());
    }
}
