package com.orientsec.idap.common.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.github.pagehelper.PageInterceptor;
import javax.sql.DataSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Shared manual MyBatis data source configuration used by IDAP modules. */
public abstract class DataSourceConfigBase {

    protected SqlSessionFactory buildSqlSessionFactory(DataSource dataSource, String mapperLocation)
            throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        PageInterceptor pageHelperPlugin = new PageInterceptor();
        Interceptor[] plugins = new Interceptor[] {pageHelperPlugin};
        bean.setPlugins(plugins);
        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources(mapperLocation));
        return bean.getObject();
    }

    protected MapperScannerConfigurer buildMapperScannerConfigurer(
            String sqlSessionFactory, String baseBasePackage) {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setSqlSessionFactoryBeanName(sqlSessionFactory);
        mapperScannerConfigurer.setBasePackage(baseBasePackage);
        return mapperScannerConfigurer;
    }
}
