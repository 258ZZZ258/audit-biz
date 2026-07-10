package com.orientsec.idap.ddl;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

class MysqlUserInfoDdlContractTest {

    @Test
    void createsTheScreenshotUserTableContract() throws Exception {
        String ddl = read("ddl/mysql/V1.0.0__init_user_info.sql");

        assertThat(ddl)
                .contains("CREATE TABLE IF NOT EXISTS `idap_user_info`")
                .contains("`user_id` VARCHAR(32) NOT NULL")
                .contains("UNIQUE KEY `uk_user_name` (`user_name`)")
                .contains("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    @Test
    void evolvesTheTableWithoutDeletingExistingUsers() throws Exception {
        String ddl = read("ddl/mysql/V1.0.1__add_user_info_columns.sql");

        assertThat(ddl)
                .contains("`avatar` VARCHAR(255)")
                .contains("`department_id` VARCHAR(32)")
                .contains("`role_id` VARCHAR(32)")
                .contains("`idx_department_id` (`department_id`)")
                .contains("`idx_role_id` (`role_id`)")
                .doesNotContainIgnoringCase("DELETE FROM")
                .doesNotContainIgnoringCase("INSERT INTO");
    }

    private String read(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
}
