package com.orientsec.idap.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.orientsec.idap.core.base.IdapTestServer;
import com.orientsec.idap.core.config.ConfigConstant;
import com.orientsec.idap.core.mapper.IdapUserInfoMapper;
import com.orientsec.idap.core.mapper.ext.IdapUserInfoMapperExt;
import com.orientsec.idap.core.model.IdapUserInfo;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(classes = IdapTestServer.class)
@Sql(statements = "DROP TABLE IF EXISTS idap_user_info")
@Sql(scripts = "classpath:ddl/V1.0.2__init_idap_user_info.sql")
class IdapUserInfoServiceIT {

    @Autowired private IdapUserInfoService service;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private IdapUserInfoMapper mapper;
    @Autowired private IdapUserInfoMapperExt mapperExt;

    @Autowired
    @Qualifier(ConfigConstant.Data_Source_NAME)
    private DataSource dataSource;

    @Autowired
    @Qualifier(ConfigConstant.SqlSession_Factory_NAME)
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    @Qualifier(ConfigConstant.JdbcTemplate)
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsAndUpdatesUserThroughMybatisPlus() {
        IdapUserInfo user = new IdapUserInfo();
        user.setUserId("U100");
        user.setUserName("测试用户");
        user.setStatus((byte) 1);
        user.setDeleted((byte) 0);
        user.setDepartmentId("DEPT001");
        user.setRoleId("ROLE_ADMIN");
        user.setCreateTime(new Date());

        assertThat(service.save(user)).isTrue();
        assertThat(service.getById("U100").getUserName()).isEqualTo("测试用户");
        assertThat(service.getById("U100").getRoleId()).isEqualTo("ROLE_ADMIN");

        user.setStatus((byte) 0);
        assertThat(service.updateById(user)).isTrue();
        assertThat(service.getById("U100").getStatus()).isEqualTo((byte) 0);

        List<Map<String, Object>> rows =
                mapper.selectListToMap(
                        Wrappers.<IdapUserInfo>lambdaQuery().eq(IdapUserInfo::getUserId, "U100"));
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("USER_ID", "U100");
    }

    @Test
    void exposesIntranetNamedBeansAndPrimaryMapperExtension() {
        assertThat(applicationContext.containsBean(ConfigConstant.Data_Source_NAME)).isTrue();
        assertThat(applicationContext.containsBean(ConfigConstant.SqlSession_Factory_NAME))
                .isTrue();
        assertThat(applicationContext.containsBean(ConfigConstant.Transaction_Manager)).isTrue();
        assertThat(applicationContext.containsBean(ConfigConstant.SqlSession_Template)).isTrue();
        assertThat(applicationContext.containsBean(ConfigConstant.MapperScannerConfigurer))
                .isTrue();
        assertThat(applicationContext.containsBean(ConfigConstant.JdbcTemplate)).isTrue();
        assertThat(dataSource).isNotNull();
        assertThat(sqlSessionFactory).isNotNull();
        assertThat(jdbcTemplate).isNotNull();
        assertThat(mapper).isSameAs(mapperExt);
    }
}
