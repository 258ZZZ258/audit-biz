# Java 多模块落位

本工程按甲方 `idap-proj-parent` 目录重构，制度查询代码只落在以下五个有效模块中。

| 模块 | 职责 | 当前代码 |
|---|---|---|
| `idap-common` | 跨层数据契约和错误模型 | `com.orientsec.idap.common.model`、统一错误对象和通用异常 |
| `idap-ddl` | 版本化数据库脚本 | PostgreSQL 根目录脚本、`ddl/mysql` 甲方用户表脚本 |
| `autocode-gen` | 甲方代码生成入口 | `IdapAutoCodeApp`，运行时委托内网 `AutoCodeBase` |
| `idap-service` | 业务能力和前端接口 | `com.orientsec.idap.core` 下的 Controller、配置、Mapper、jCasbin、audit-ai 客户端、引用装配和日志 |
| `idap-server` | 可运行应用 | `com.orientsec.idap.server.IdapAppServer`、环境配置和 Casbin 资源 |
| `idap-test` | 跨模块验证 | MockMvc、Security、引用回查和 Testcontainers 测试 |

`idap-job`、`idap-ui` 目前只保留父工程接缝。`autocode-gen` 已落地截图中的 `idap_user_info`
生成入口，但不复制甲方私有生成器和模板。`idap-genesis` 的默认源码只保留
可独立构建的公共接缝；`src/intranet/java` 中放置依赖 Genesis 私有制品的认证 Controller 和 Service 契约，
仅在 Maven `intranet` Profile 下参与编译。认证 Controller 还需配置 `GENESIS_AUTH_ENABLED=true`，并由
内网工程提供 `GenesisUserService` 实现 Bean；默认关闭，避免私有依赖或实现未就绪时暴露半成品接口。
后续接入甲方原工程时，应优先复用甲方模块已有依赖管理和基础类，只迁入上述职责对应的包。

## 依赖约束

```text
idap-test
    ↓
idap-server
    ↓
idap-service
    ↓
idap-common

idap-ddl (独立版本化脚本模块)
```

- `idap-common` 不依赖 Spring MVC、数据库或业务服务。
- `idap-ddl` 只维护版本化 SQL，不承载 Mapper 或业务编排。
- `idap-service` 不依赖 `idap-server`，避免业务层反向引用启动层。
- Controller、Security 和 MyBatis 配置位于 `idap-service/com.orientsec.idap.core`，由 `idap-server` 统一启动和暴露。
- Python 仍负责 RAG/重排/路由/生成，Java 模块不得复制相关逻辑。

## 甲方目录映射

```text
idap-common/src/main/java/com/orientsec/idap/common
idap-ddl/src/main/resources/ddl
idap-service/src/main/java/com/orientsec/idap/core
idap-server/src/main/java/com/orientsec/idap/server
idap-test/src/test/java/com/orientsec/idap/core
```

`autocode-gen` 的入口通过反射委托 `com.orientsec.idap.autocode.AutoCodeBase`；实际类名不同时可通过
`idap.autocode.base-class` 指定。`idap-genesis` 的 `com.orientsec.genesis.auth` 属于甲方现有基座，
本外网工程不复制或伪造其内部实现。
