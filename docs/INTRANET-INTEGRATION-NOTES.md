# 内网甲方基座对接记录

> 本文只记录截图已确认的信息和待确认事项。在截图收集完成前，不据此直接替换外网可构建配置。

## 长期开发基线（2026-07-10 确认）

当前目录和代码是从甲方内网工程融合得到的外网开发基线。后续开发必须维持现有多模块布局、
`com.orientsec.idap` / `com.orientsec.genesis` 包结构、JDK 8 与 Maven 3.6.3 兼容性、甲方数据源和
MyBatis 契约、统一响应模型、Profile 配置键及 Java↔Python 制度查询边界。

除非经过明确评审和用户确认，不得恢复为融合前的根目录单模块结构，不得重新引入
`com.dfzq.auditai.biz` 作为主代码路径，也不得用 Boot 3/Jakarta、WebFlux、JPA、Spring Cloud 或
其他新技术栈替换甲方基线。内网私有制品通过兼容接缝复用，外网默认构建仍须保持可编译、可测试，
所有凭据继续由环境变量或部署平台注入。

本机 Codex 工作区的详细执行约束同时记录在根目录 `AGENTS.md`；该文件按既有约定只作为本地代理
指令，不纳入 Git。受 Git 管理的长期事实以本节和本文其余融合记录为准。

## 融合执行原则

用户提供的所有内网代码截图均视为需要落地的甲方契约。截图收集完成后，每一项必须归入以下一种处理方式：

1. **直接融合**：包名、类名、接口签名、启动注解、响应模型、模块依赖和配置结构可直接落入外网工程。
2. **兼容适配**：依赖内网私服或运行环境的部分，在外网保留可构建实现，同时提供清晰的内网接入点。
3. **复用甲方实现**：Genesis、Dubbo、代码生成器等甲方已有能力不复制伪造，制度查询代码按其公开契约调用。
4. **明确忽略**：仅限 IDE 文件、演示数据、无业务关系的测试脚手架、重复配置和敏感值；必须记录忽略原因。

不得因为外网暂时无法解析私有依赖，就遗漏截图中对模块边界、扫描方式、配置键或调用契约的要求。

当前处理状态：

| 截图内容 | 计划处理方式 |
|---|---|
| 根 POM 坐标、模块、JDK 和插件约定 | 直接融合，并保持 Maven 3.6.3 兼容 |
| `genesis-parent` 和 Genesis 私有依赖 | 兼容适配，内网复用甲方制品 |
| `IdapAppServer` 注解、扫描和自动配置排除 | 直接融合 |
| `PackageBase`、`ForPackage` | 直接融合 |
| `Result`、`ResultCode`、`ResultGenerator` | 直接融合，普通 JSON 接口统一使用 |
| WebFlux 排除 | 直接融合，并继续禁止 WebFlux |
| 甲方手工数据源与 MyBatis 配置 | 直接融合类名、Bean 名、配置键和 Mapper 扫描方式 |
| 制度查询 PostgreSQL 数据源 | 复用 `datasource.idap` 手工数据源规范，按 JDBC URL 切换数据库 |
| `IdapUserInfoMapperExt.xml` | 已确认项目中存在，命名空间与截图一致 |
| MySQL 用户表初始化和增量脚本 | 直接融合表结构；清库和演示数据明确忽略 |
| `LogHelper`、滚动日志配置 | 直接融合，日志级别与路径支持环境变量覆盖 |
| `application-share/authdev/idaptest` | 兼容适配，保留配置键但不写入真实地址和密钥 |
| `IdapAutoCodeApp` | 兼容适配，入口已落地，运行时复用甲方 `AutoCodeBase` |
| 内网 URL、密码、Token、证书 | 忽略真实值，仅保留配置键和占位符 |

## 当前已融合代码

- 根坐标和所有子模块版本已切换为 `1.0.0-SNAPSHOT`。
- 根模块顺序、JDK 8 编译配置、版本文件插件和截图中的公共依赖版本已融合。
- `intranet` Maven Profile 已声明 `genesis-parent:2.3.0` 以及 Genesis 私有依赖；默认外网构建不解析私服制品。
- `idap-server`、`idap-service`、`idap-common`、`idap-genesis` 的依赖方向已按截图调整。
- `IdapAppServer` 的 Dubbo、组件扫描、自动配置排除和启动日志已融合。
- `PackageBase`、`ForPackage`、`IdapTestServer` 已落地。
- `Result`、`ResultCode`、`ResultGenerator` 已落地，普通 JSON Controller 已统一包装响应；SSE 端点继续保持事件流契约。
- `IdapUserInfoController` 及其 `IdapUserInfo`、Mapper、Service、ServiceImpl、`LogHelper`、用户表 DDL 已按甲方分层直接融合；实体已补齐 `role_id`。
- `IdapUserInfo` 已按截图补齐显式 `@TableId`/`@TableField`、访问器和 `toString()`；`IdapUserInfoMapper` 已补齐 `selectListToMap`，并通过 `@Primary` 的 `IdapUserInfoMapperExt` 提供扩展入口。
- `DataSourceConfigBase`、`ConfigConstant`、`IdapDataSourceConfig` 已直接融合；数据源、`JdbcTemplate`、`SqlSessionFactory`、事务管理器、`SqlSessionTemplate` 和 `MapperScannerConfigurer` 均使用截图约定的命名 Bean。
- Mapper XML 路径统一为 `classpath*:com/orientsec/idap/core/mapper/**/*.xml`，Mapper 扫描范围由 `ForPackage` 定位。
- 用户管理已覆盖列表、详情、新增、更新、批量软删除、状态修改和密码重置接口；密码重置继续保持甲方截图中的占位行为，待内网认证服务接管。
- 外网默认 Dubbo application、`N/A` registry 和 consumer check 配置已补齐，内网配置可通过环境变量或 Profile 覆盖。
- MySQL `V1.0.0`/`V1.0.1` 用户表脚本已按截图保存在 `ddl/mysql`；截图中的清库语句和演示账号不会随版本迁移执行。
- `share`、`authdev`、`idaptest`、`jobtest` Profile 和控制台/业务/错误滚动日志已落地；默认激活列表与截图一致，内网地址和密钥仍必须由环境变量注入。
- `IdapAutoCodeApp` 已固定生成 `idap_user_info`，并通过可替换类名的调用桥复用内网 `AutoCodeBase`。
- 外网全量 `mvn verify` 已通过（46 项测试，含 8 项 PostgreSQL Testcontainers 集成测试），覆盖命名数据源 Bean、Mapper 扩展、用户管理 HTTP/MyBatis-Plus、MySQL DDL 合约、Profile/凭据检查及生产启动类健康检查。

## 已确认环境

- JDK：`1.8.0_20`
- Maven：`3.6.3`
- Maven 坐标：`com.orientsec.idap:idap-proj-parent:1.0.0-SNAPSHOT`
- 私有父工程：`com.orientsec.genesis:genesis-parent:2.3.0`
- Java 编译级别：`1.8`
- Maven Compiler Plugin：`3.11.0`

## 已确认模块依赖

```text
idap-server
    -> idap-service
        -> idap-common
        -> idap-genesis
            -> idap-common
```

- `idap-server` 只直接依赖 `idap-service`。
- `idap-service` 直接依赖 `idap-common`、`idap-genesis`。
- `idap-genesis` 负责 Genesis、Dubbo、LDAP 等内网集成。
- `idap-common` 集中提供 Web、MyBatis-Plus、Druid、MySQL、PageHelper 等基础依赖。

## 已确认主要版本

- Genesis：`2.3.0`
- Dubbo：`2.7.8`
- ZooKeeper：`3.4.12`
- MyBatis-Plus：`3.5.12`
- MyBatis：`3.5.19`
- MySQL Connector/J：`8.0.11`
- Druid：`1.1.20`
- PageHelper：`5.3.1`

## 已确认启动方式

甲方启动类为 `com.orientsec.idap.server.IdapAppServer`，使用：

- `@SpringBootApplication`
- `@EnableDubbo`
- `@Slf4j`
- 通过 `basePackageClasses` 扫描 `com.orientsec.idap.core.PackageBase`
- 通过 `basePackageClasses` 扫描 `com.orientsec.genesis.auth.PackageBase`
- 排除启动类自身的重复组件扫描

启动时显式排除以下自动配置：

- `DataSourceAutoConfiguration`
- `MongoAutoConfiguration`
- `MongoDataAutoConfiguration`
- `WebFluxAutoConfiguration`

因此制度查询不依赖 Spring Boot 默认数据源自动配置，也不得引入 WebFlux。当前 PostgreSQL 与外网 H2 测试均通过甲方手工 `datasource.idap` 配置体系接入。

已确认的数据源 Bean 契约：

```text
idapDataSource
idapJdbcTemplate
idapSqlSessionFactory
idapTransactionManager
idapSqlSessionTemplate
idapMapperScannerConfigurer
```

已确认包定位类：

```text
com.orientsec.idap.core.PackageBase
com.orientsec.genesis.auth.PackageBase
com.orientsec.idap.core.mapper.ForPackage
```

三个类当前均为空类，只用于组件或 Mapper 包定位。

## 已确认统一响应契约

`com.orientsec.idap.common.model.Result<T>` 包含：

```text
int code
String message
T data
```

- setter 返回 `Result`，支持链式调用。
- `toString()` 使用 Hutool `JSONUtil.toJsonPrettyStr(this)`。

`ResultCode` 当前定义：

```text
SUCCESS(200)
FAIL(400)
UNAUTHORIZED(401)
NOT_FOUND(404)
INTERNAL_SERVER_ERROR(500)
```

`ResultGenerator` 当前提供：

```text
genSuccessResult()
genSuccessResult(T data)
genFailResult(Exception e)
```

- 成功消息固定为 `SUCCESS`。
- 失败结果记录异常堆栈，返回 `FAIL(400)` 和异常消息。
- 普通 JSON Controller 复用该响应契约；SSE 流式端点不包裹 `Result`，避免破坏 `SseEmitter` 事件协议。

## 制度查询仍保持的技术约束

- Java 采用 JDK 8、Spring Boot 2.x、Spring MVC `SseEmitter`。
- Java 只负责令牌校验、jCasbin 授权、权限过滤值下传、SSE 透传、引用回查和操作日志。
- RAG、重排、路由和生成全部由 Python 负责。
- 制度查询数据流只读，引用必须完成条款、文档、页码、版本/状态溯源。
- 不引入 Boot 3/Jakarta、WebFlux、JPA、Spring Cloud、`@PreAuthorize`。

## 待内网制品确认

- `AutoCodeBase` 的实际完整类名及代码生成器私有依赖坐标。
- Genesis 认证过滤链在私服制品下的最终集成运行结果。
- 是否还存在第二套业务数据源，以及多数据源事务边界。

## 兼容性注意事项

- 外网无法解析 `genesis-parent` 和 Genesis 私有依赖，外网工程需继续保持独立可构建。
- 回内网时应复用甲方现有 POM、Genesis、通用类和配置，不整体覆盖基座模块。
- 最终验证必须使用内网同版本 JDK `1.8.0_20` 和 Maven `3.6.3` 再执行一次。
- 截图中出现过 Redis、ZooKeeper、数据库和 CA 的明文凭据；仓库仅保留环境变量占位，原凭据应在对应平台轮换。
- 截图中的虚拟线程配置要求高版本 JDK，与当前 JDK 8/Boot 2.7 基线不兼容，因此未启用。
- Actuator 未照搬截图中的全量公开和明文健康详情，默认仅开放 `health,info`，需要扩大范围时由受控环境变量显式设置。
