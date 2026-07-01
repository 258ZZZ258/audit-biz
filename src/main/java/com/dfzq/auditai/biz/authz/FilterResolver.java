package com.dfzq.auditai.biz.authz;

import com.dfzq.auditai.biz.dto.Filters;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * 从当前用户(SSO 令牌)预计算检索前置过滤值(§7 边界二:算在 Java、用在 Python)。
 *
 * <p>claim 口径为 dev 约定(真 SSO claim schema 待甲方 §12):{@code perm_tags}(密级/职级)、 {@code
 * corpus_scope}(可检索分区)、{@code project_id}。**owner 行级 ABAC**:仅当范围含 {@code audit_project} 时
 * owner=当前用户(§7.x);制度语料不带 owner,避免误伤共享知识库。
 */
@Component
public class FilterResolver {

    private static final String AUDIT_PROJECT = "audit_project";

    public Filters resolve(Jwt jwt) {
        List<String> permTags = orEmpty(jwt.getClaimAsStringList("perm_tags"));
        List<String> corpusTypes = orEmpty(jwt.getClaimAsStringList("corpus_scope"));
        String projectId = jwt.getClaimAsString("project_id");
        String owner = corpusTypes.contains(AUDIT_PROJECT) ? jwt.getSubject() : null;
        return new Filters(permTags, corpusTypes, projectId, owner);
    }

    private static List<String> orEmpty(List<String> v) {
        return v != null ? List.copyOf(v) : List.of();
    }
}
