package com.orientsec.idap.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientsec.idap.common.model.Filters;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 调 audit-ai /v1/query 的阻塞式 HTTP SSE 客户端。边界二无身份,只带共享密钥。 */
@Component
@ConditionalOnProperty(name = "audit.ai.stub.enabled", havingValue = "false", matchIfMissing = true)
public class HttpBoundaryClient implements BoundaryClient {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 65_000;

    private final ObjectMapper mapper;
    private final String endpoint;
    private final String internalToken;

    public HttpBoundaryClient(
            ObjectMapper mapper,
            @Value("${audit.ai.base-url:http://127.0.0.1:8771}") String baseUrl,
            @Value("${audit.ai.internal-token:${AUDIT_AI_INTERNAL_TOKEN:}}") String internalToken) {
        this.mapper = mapper;
        this.endpoint = trimRight(baseUrl, "/") + "/v1/query";
        this.internalToken = internalToken;
    }

    @Override
    public void query(
            String requestId,
            String question,
            Filters filters,
            QueryOptions options,
            Listener listener) {
        if (!StringUtils.hasText(internalToken)) {
            throw new IllegalStateException("缺少 audit-ai 共享密钥配置:AUDIT_AI_INTERNAL_TOKEN");
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("X-Internal-Token", internalToken);
            byte[] body =
                    mapper.writeValueAsBytes(requestBody(requestId, question, filters, options));
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body);
            }

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                listener.onError("B104", readText(conn.getErrorStream()));
                return;
            }
            readSse(conn.getInputStream(), listener);
        } catch (IOException e) {
            listener.onError("E502", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private Map<String, Object> requestBody(
            String requestId, String question, Filters filters, QueryOptions options) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("request_id", requestId);
        body.put("query", question);
        Map<String, Object> filterBody = new LinkedHashMap<>();
        filterBody.put("perm_tags", nullToEmpty(filters.permTags()));
        filterBody.put("corpus_types", nullToEmpty(filters.corpusTypes()));
        filterBody.put("project_id", filters.projectId());
        filterBody.put("owner", filters.owner());
        body.put("filters", filterBody);
        if (options != null && options.includeSuperseded() != null) {
            body.put(
                    "options",
                    Collections.singletonMap("include_superseded", options.includeSuperseded()));
        }
        return body;
    }

    private void readSse(InputStream input, Listener listener) throws IOException {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String event = null;
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    dispatch(event, data.toString(), listener);
                    event = null;
                    data.setLength(0);
                } else if (line.startsWith("event:")) {
                    event = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:")) {
                    if (data.length() > 0) {
                        data.append('\n');
                    }
                    data.append(line.substring("data:".length()).trim());
                }
            }
            if (event != null || data.length() > 0) {
                dispatch(event, data.toString(), listener);
            }
        }
    }

    private void dispatch(String event, String rawData, Listener listener) throws IOException {
        if (!StringUtils.hasText(event) || !StringUtils.hasText(rawData)) {
            return;
        }
        JsonNode data = mapper.readTree(rawData);
        if ("meta".equals(event)) {
            listener.onMeta(
                    text(data, "route_type"),
                    bool(data, "ai_label", true),
                    bool(data, "review_required", false),
                    nullableBool(data, "export_enabled"));
        } else if ("delta".equals(event)) {
            listener.onDelta(
                    data.path("block_seq").asInt(), text(data, "block_type"), text(data, "text"));
        } else if ("citation".equals(event)) {
            listener.onCitation(
                    text(data, "clause_id"),
                    text(data, "chunk_id"),
                    number(data, "confidence", "score"),
                    nullableBool(data, "ai_label"));
        } else if ("done".equals(event)) {
            listener.onDone(
                    text(data, "finish_reason"),
                    number(data, "confidence"),
                    stringList(data.path("exhausted_scope")));
        } else if ("error".equals(event)) {
            listener.onError(text(data, "code"), text(data, "message"));
        }
    }

    private static List<String> nullToEmpty(List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static boolean bool(JsonNode node, String field, boolean defaultValue) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? defaultValue : value.asBoolean();
    }

    private static Boolean nullableBool(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asBoolean();
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asDouble();
    }

    private static Double number(JsonNode node, String primary, String fallback) {
        Double value = number(node, primary);
        return value != null ? value : number(node, fallback);
    }

    private static List<String> stringList(JsonNode node) {
        if (!node.isArray()) {
            return Collections.emptyList();
        }
        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());
    }

    private static String readText(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static String trimRight(String value, String suffix) {
        String result = value;
        while (result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }
}
