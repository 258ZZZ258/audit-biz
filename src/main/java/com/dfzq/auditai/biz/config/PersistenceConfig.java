package com.dfzq.auditai.biz.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 持久层配置。@MapperScan 放此(而非主类)——@WebMvcTest 切片不加载本 @Configuration, 免在无 MyBatis infra 的 web 切片里扫
 * mapper 报错;@SpringBootTest 全上下文正常加载。
 */
@Configuration
@MapperScan("com.dfzq.auditai.biz.citation")
public class PersistenceConfig {}
