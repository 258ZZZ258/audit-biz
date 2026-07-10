package com.orientsec.idap.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class IntranetProfileConfigurationTest {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void preservesScreenshotKeysWithoutEmbeddingIntranetSecrets() throws Exception {
        assertThat(property("application-share.yml", "server.port"))
                .isEqualTo("${SERVER_PORT:8090}");
        assertThat(property("application-share.yml", "management.endpoints.web.exposure.include"))
                .isEqualTo("${MANAGEMENT_ENDPOINTS:health,info}");
        assertThat(property("application-authdev.yml", "ca.clientSecret"))
                .isEqualTo("${CA_CLIENT_SECRET:}");
        assertThat(property("application-authdev.yml", "dubbo.registry.address"))
                .isEqualTo("${DUBBO_REGISTRY_ADDRESS:N/A}");
        assertThat(property("application-authdev.yml", "spring.redis.password"))
                .isEqualTo("${REDIS_PASSWORD:}");
        assertThat(property("application-idaptest.yaml", "datasource.idap.jdbc-url"))
                .isEqualTo(
                        "${IDAP_DB_URL:${DB_URL:jdbc:h2:mem:auditai;DB_CLOSE_DELAY=-1;MODE=PostgreSQL}}");
        assertThat(property("application-idaptest.yaml", "datasource.idap.password"))
                .isEqualTo("${IDAP_DB_PASSWORD:${SPRING_DATASOURCE_PASSWORD:}}");
    }

    private Object property(String resource, String key) throws Exception {
        List<PropertySource<?>> sources = loader.load(resource, new ClassPathResource(resource));
        return sources.get(0).getProperty(key);
    }
}
