package com.orientsec.idap.core.authz;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** A2 验收:jCasbin 六类权限点授权(文件策略)。 */
class AuthorizerTest {

    private final Authorizer authorizer = new Authorizer();

    @Test
    void managerCanQueryAndExport() {
        assertThat(authorizer.permits("zhang", "regulation_query", "read")).isTrue();
        assertThat(authorizer.permits("zhang", "export", "execute")).isTrue();
    }

    @Test
    void auditorCanQueryButNotExport() {
        assertThat(authorizer.permits("li", "regulation_query", "read")).isTrue();
        assertThat(authorizer.permits("li", "export", "execute")).isFalse();
    }

    @Test
    void unknownSubjectDenied() {
        assertThat(authorizer.permits("stranger", "regulation_query", "read")).isFalse();
    }
}
