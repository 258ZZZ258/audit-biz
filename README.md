# idap-proj-parent · audit-biz

审计大模型系统一期 Java 后端的 Maven 多模块工程。应用对前端提供唯一入口，负责 SSO 验令牌、
jCasbin 授权、权限过滤值预计算、Spring MVC SSE、PostgreSQL 引用装配和操作日志；无状态 AI
推理与检索仍由姊妹项目 `audit-ai` 承担。

## 模块

```text
idap-proj-parent
├── autocode-gen   甲方代码生成模块接缝
├── idap-common    跨层 Model、统一错误模型
├── idap-ddl       版本化数据库 DDL
├── idap-genesis   Genesis 平台集成接缝
├── idap-job       异步任务/定时任务接缝
├── idap-server    Spring Boot 启动类和运行配置
├── idap-service   Controller、Mapper、授权、引用和业务服务
├── idap-test      单元测试与集成测试
└── idap-ui        甲方 UI 模块接缝；Vue 源码仍在独立 audit-vue 项目
```

依赖方向固定为 `idap-server → idap-service → idap-common`，`idap-ddl` 独立维护版本化 SQL，测试统一放在
`idap-test`。详细落位见 `docs/MODULE-LAYOUT.md`。

Java 包名遵循甲方基座：公共契约使用 `com.orientsec.idap.common.model`，业务代码使用
`com.orientsec.idap.core`，启动类为 `com.orientsec.idap.server.IdapAppServer`。

## 技术基线

- JDK 8
- Spring Boot 2.7.18 + Spring MVC `SseEmitter`
- MyBatis-Plus
- jCasbin
- Spring Security resource-server，仅负责令牌验证

禁止引入 Spring Boot 3/Jakarta、WebFlux、JPA、Spring Cloud 和 `@PreAuthorize`。

## 构建与运行

```bash
./mvnw clean verify

# 只构建可运行服务及其依赖
./mvnw -pl idap-server -am clean package

java -jar idap-server/target/idap-server-1.0.0-SNAPSHOT-exec.jar
```

应用默认使用内存 H2。连接共享 PostgreSQL 和 audit-ai 时通过环境变量注入：

```bash
DB_URL=jdbc:postgresql://localhost:5433/audit_pipeline \
SPRING_DATASOURCE_USERNAME=pipeline \
SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
SSO_JWK_SET_URI=http://localhost:18080/jwks.json \
AUDIT_AI_BASE_URL=http://localhost:8771 \
AUDIT_AI_INTERNAL_TOKEN="$AUDIT_AI_INTERNAL_TOKEN" \
java -jar idap-server/target/idap-server-1.0.0-SNAPSHOT-exec.jar
```

## 本地数据库

`compose.yaml` 提供独立开发 PostgreSQL，首次启动执行
`idap-ddl/src/main/resources/ddl/*.sql`。其中
`chunks/doc_versions/cases` 只是本地引用回查 stand-in；三栈联调应连接 audit-ai 使用的共享 PG。
甲方 MySQL 用户表版本脚本单独位于 `idap-ddl/src/main/resources/ddl/mysql`，不会被本地
PostgreSQL 容器自动执行。

```bash
cp .env.example .env
docker compose up -d
```

## 文档

- 后端总体设计：`docs/审计大模型系统_后端总体技术框架设计_v0_4.md`
- Java↔Python 边界：`docs/audit-biz-docs/SPEC-BOUNDARY.md`
- 前端制度查询契约：`docs/audit-biz-docs/SPEC-FRONTEND-REGQUERY.md`
