package com.dfzq.auditai.biz.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dfzq.auditai.biz.dto.Filters;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/** A2 验收:JWT claim → 预计算 Filters,含 owner 行级 ABAC 规则(§7.x)。 */
class FilterResolverTest {

    private final FilterResolver resolver = new FilterResolver();

    private Jwt jwt(List<String> permTags, List<String> corpusScope, String projectId) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("zhang");
        if (permTags != null) {
            b.claim("perm_tags", permTags);
        }
        if (corpusScope != null) {
            b.claim("corpus_scope", corpusScope);
        }
        if (projectId != null) {
            b.claim("project_id", projectId);
        }
        return b.build();
    }

    @Test
    void mapsClaimsToFilters() {
        Filters f = resolver.resolve(jwt(List.of("L2"), List.of("internal", "external"), null));
        assertThat(f.permTags()).containsExactly("L2");
        assertThat(f.corpusTypes()).containsExactly("internal", "external");
        assertThat(f.owner()).isNull(); // 制度语料不带 owner
    }

    @Test
    void ownerSetOnlyForAuditProject() {
        Filters withProject = resolver.resolve(jwt(List.of(), List.of("audit_project"), "P-1"));
        assertThat(withProject.owner()).isEqualTo("zhang");
        assertThat(withProject.projectId()).isEqualTo("P-1");

        Filters regulation = resolver.resolve(jwt(List.of(), List.of("internal"), null));
        assertThat(regulation.owner()).isNull();
    }

    @Test
    void auditProjectWithoutProjectIdRejected() {
        // §4.5/§7.x fail-closed:audit_project 缺 project_id → 拒绝,防跨项目召回。
        assertThatThrownBy(() -> resolver.resolve(jwt(List.of(), List.of("audit_project"), null)))
                .isInstanceOf(FilterValidationException.class);
    }

    @Test
    void missingClaimsYieldEmptyLists() {
        Filters f = resolver.resolve(jwt(null, null, null));
        assertThat(f.permTags()).isEmpty();
        assertThat(f.corpusTypes()).isEmpty();
    }
}
