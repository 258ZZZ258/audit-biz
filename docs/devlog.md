# audit-biz 开发记忆(时间轴索引)

> 细节下沉 `docs/devlogs/*`;本文件只做阶段索引。三层记忆分工见 `CLAUDE.md`。

## 2026-06-30 · 仓库初始化(从 audit-ai 拆出 Java 后端)

- **决策**:按 v0.4(CP-011)双语言基座,**与前端交互的一切走 Java(audit-biz),智能体/解析保留 Python(audit-ai)**
  ——甲方硬约束"前端交互层必须 Java"。故新建独立 `audit-biz` 仓库,与 `audit-ai` 物理隔离(语言/CI/团队各自独立)。
- **否决方案**:① v0.3 纯 Python 单后端——因上述甲方约束不成立,作废;② 后端 monorepo(audit-biz/ + audit-ai/ 同仓)
  ——选了两独立仓库,因 audit-ai 已成熟在原路径、其 agent 记忆按绝对路径键,合并会使现有 auto-memory 命名空间失效。
- **记忆迁移**(两独立仓库拓扑):audit-ai 原地不动 → 其 devlog/记忆零迁移;跨仓用户偏好(中文沟通、决策点用选择题)
  **提升到用户全局 `~/.claude/CLAUDE.md`**,本仓不重复;边界契约主本(v0.4)归本仓,audit-ai 侧留指针(**待回填**)。
- **【待定】**:Java 工程脚手架(Maven/Gradle、包结构、Spring Boot 应用骨架)、lint/测试约定、国产化 JDK —— 待 SDD 评审。

## 2026-06-30 · 后端起步路线(契约先行 + biz 步行骨架优先)

- **背景核实**:audit-ai `demo-web` 是 stdlib-HTTP 内部工作台(**非 FastAPI**),query 无 HTTP 层,**全仓无 FastAPI**
  → v0.4 §8 那套 biz 要调的服务端点(/retrieve /generate /compare /ocr /ingest-batch)**尚未落地**。
- **决策(用户拍板)**:
  - ① 真正的第一步是**锁边界契约**(v0.4 §8 → SPEC):阻塞两轨、零代码最便宜,锁完两轨可并行(一侧拿另一侧 stub 顶)。
  - ② 实现从 **biz 锚定的「步行骨架」垂直切片**起 —— 制度查询一条线:前端 → biz(SSO 验令牌 + jCasbin + 预计算
    `perm_tags`)→ 单向无身份调 audit-ai `/retrieve`(**先 stub,后真**)→ SSE 透传。最早暴露 Java/鉴权最高风险 + 验证整套架构。
  - ③ **「audit-ai web 升级」= 用 FastAPI 暴露 §8 端点**(包裹已有 query/pipeline),是边界的 audit-ai 半边、**次轨**;
    先做 `/retrieve`+`/generate` 喂切片,替换 biz 侧 stub。
- **否决**:「先把 audit-ai 端点全做完再开 biz」(Java/鉴权风险后置)、「先把 biz 骨架全搭完再集成」(晚暴露集成风险)
  —— 都不如最薄垂直切片早验架构。
- **轨道**:Track0 边界契约 SPEC(落 `docs/audit-biz-docs/`,audit-ai 引用)→ Track A biz 切片(脚手架→SSO→jCasbin→查询端点)
  ‖ Track B audit-ai FastAPI 端点。每轨走 SDD(spec→plan→tasks→TDD)+ Codex 审查闭环。
- **关联**:切片的 SSO/permitAll 正是 TODO-AUTH-001 的收口点。

## 2026-07-01 · A0 Spring Boot 脚手架(Phase 4 IMPLEMENT)

- **落地**:Maven + Spring Boot 3.3.5 + JDK 17,根包 `com.dfzq.auditai.biz`;`AuditBizApplication` + `/health`(web)
  + Maven Wrapper(`mvnw`,自举只需 JDK)。TDD:`HealthControllerTest`(/health→200 `{status:UP}`)+
  `AuditBizApplicationTests`(contextLoads)→ `mvn test` **2 passed / BUILD SUCCESS**(JDK 17.0.19 真验证)。
- **对 TASKS A0 措辞的修正(决策)**:TASKS 原写"pom 提前声明 MyBatis-Plus/oauth2-resource-server/jCasbin/POI";
  实测**这些 starter 无 DataSource/issuer 配置会让 A0 启动失败**,违反"每步留可运行态"。→ A0 只放 web+test 最小可启动集,
  DB/安全/Casbin/POI **各随 A1/A2/后续任务引入**(届时同步配 issuer/datasource)。
- **环境怪癖(team / CI 须知)**:需 **JDK 17**;mac `brew install openjdk@17` 是 **keg-only** →
  构建前 `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`(否则 `mvn`/`java` 找不到);Maven 用 `brew install maven` 或仓内 `./mvnw`。

## 2026-07-01 · A1 SSO resource-server 验令牌(TODO-AUTH-001 收口)

- **落地**:`spring-boot-starter-oauth2-resource-server`;`SecurityConfig`(无状态 + csrf off + **受保护端点强制认证**、
  permitAll 仅 `{/health,/sso/callback}`)+ `WhoAmIController`(`GET /api/v1/me` 从 JWT 解析当前用户)。
  JWKS 走 `jwt.jwk-set-uri`(env `SSO_JWK_SET_URI`,dev/CI 占位;Nimbus 懒取不阻启动;真 SSO 协议待甲方 §12)。
- **TDD/验收**:`SecurityConfigTest`(/health→200 · /api/v1/me 无令牌→**401** · mock jwt→200+解析 user_id/name)
  + `HealthControllerTest`(安全链在位仍公开,`@Import SecurityConfig` + `@MockBean JwtDecoder`)→ `mvn verify` **5 passed / BUILD SUCCESS**。
- **收口 TODO-AUTH-001**:protected→401 证明**非全 permitAll**(纠 v0.4 §7 草案)。走 feature 分支 → PR(CI + Codex 审)。
- **审查修复(Codex `TEST-VERIFY-001`,两轮)**:Mockito inline mock maker 依赖 byte-buddy **self-attach**,受限沙箱/JDK21+/信创会挂
  (`Could not init inline Byte Buddy mock maker / Could not self-attach to current VM`)。**本机复现法**:
  `mvn test -DargLine=-XX:+DisableAttachMechanism`(全禁 attach,贴 Codex 锁死沙箱;`allowAttachSelf=false` **不够**——byte-buddy 有外部进程 attach 兜底,本机会过)。
  - **第一轮去 `@MockBean` 不够**:Spring 测试基建(MockitoTestExecutionListener 等)仍初始化 inline maker,与有无 @MockBean 无关——禁 attach 下**仍全挂**(复现证实)。
  - **真修**:`src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` = `mock-maker-subclass`(子类生成,不走 attach)。
    禁 attach 复现条件下 **5 passed**(修前 3 errors),正常 `mvn verify` 亦绿。**约定**:保留此配置;确需 inline(mock final 类)再单议。

## 2026-07-01 · A2 jCasbin 授权 + 过滤值预计算

- **落地**:`jcasbin 1.55.0`;`Authorizer`(唯一 enforcer、六类权限点、文件策略 `casbin/model.conf`+`policy.csv`)
  + `FilterResolver`(JWT claim → `Filters`{permTags/corpusTypes/projectId/owner};**owner 仅 audit_project** §7.x)+ `dto.Filters`。
- **TDD/验收**:`AuthorizerTest`(manager 可查/导出 · auditor 不可导出 · 未知主体拒)+ `FilterResolverTest`
  (claim→filters · owner 仅 audit_project · 缺 claim 空表)→ `mvn verify` **11 passed**(禁 attach 复跑亦绿)。
- **范围决策(薄增量)**:① **不接 PG**——文件策略,避开"`casbin_rule` 谁迁移"的 schema 归属跨仓决策(audit-ai Alembic 现管 PG),
  生产切 PG JDBC adapter(接 PG 那步与 A4 一起);② **请求级「越权→B102」挪 A3**(有 /query 端点再串 Authorizer + 错误体)。
- **审查修复(Codex)**:
  - `AUTHZ-BOOT-001`(**critical**)——`Authorizer` 用 `ClassPathResource.getFile()` 让 **repackaged jar 起不来**
    (`java -jar` 复现 `FileNotFoundException ... cannot be resolved to absolute file path` @ authorizer bean;我先前"dev-only"辩解站不住——**默认打包产物必须可运行**)。
    改 **InputStream 拷临时文件**的 jar-safe 加载 → 修后 `java -jar` **启动成功**(`Started AuditBizApplication`)。**复现法**:`mvn package -DskipTests && java -jar target/*.jar`。
  - `AUTHZ-FILTER-001`(warning)——`audit_project` 缺 `project_id` 会退化成跨项目召回同 owner 资料(§4.5/§7.x)→
    **fail-closed 拒绝**(`FilterValidationException`,A3 映射 B2xx)+ 补缺 project_id 拒绝单测。
  - `AUTHZ-LOG-001`(warning,复审新增)——jCasbin 默认 INFO 打印完整 Policy/Role links + 每次 enforce 的 sub/obj/act,
    生产切 PG 后泄露用户-角色绑定与授权决策 → `Util.enableLog=false`(**静态块**,类加载即生效、先于构造期 Policy 转储)
    + application.yml `org.casbin.jcasbin: warn` 双保险。`mvn verify` 输出实测 jCasbin 日志已静默。业务审计另由 Java 侧结构化留痕。
- **约定**:claim 口径(perm_tags/corpus_scope/project_id)为 dev 约定,真 SSO schema 待甲方 §12;文件策略仅 A2,生产切 PG JDBC adapter。

## 2026-07-01 · Checkpoint A:边界契约冻结

- **冻结**:`SPEC-BOUNDARY` 附录 A 字段对照表锁定,`openapi/boundary.v1.yaml` 版本 `1.0.0-draft → 1.0.0`。
  A 轨(biz A3/A4)/ B 轨(audit-ai B1–B4)据此**并行**;后续任何字段/语义改动走**破坏性变更流程**(§6 Ask first,或 `/v2`),不再随手改。
- **前置已就绪**:T0.1 已把契约对齐 audit-ai §10 `contract.py`(route_type 8 值 / ai_label bool / citation 轻量 / review_required·exhausted_scope),
  两处纠偏(confidence/ai_label/route_type 为响应级;引用回查翻转 A4‖B3)在案。
- **进度**:T0.1 · A0 · A1 · A2 已合并 main;A3 起(第一次真用 boundary.v1.yaml)。

## 2026-07-01 · A3 制度查询 SSE 端点(接边界契约 + 串鉴权/授权/过滤)

- **落地**:`POST /api/v1/regulation/queries`(SSE)→ **同步** jCasbin 授权(越权→B102)+ FilterResolver 预计算(校验失败→B2xx)fail-fast
  → **异步** `BoundaryClient`(A3 `StubBoundaryClient`,callback 流式)桥接为前端 SSE `context/delta/done`。
  统一错误体 `{error:{code,message,request_id}}`:`GlobalErrorHandler`(B102/B2xx)+ `RestAuthEntryPoint`(401 **B101**,替 Spring Security 空体)。
  Jackson 全局 **SNAKE_CASE**(前端/边界契约命名决策)。
- **TDD/验收**:`QueryControllerTest`(授权→SSE context/delta/done · 越权→403 B102 · 未认证→401 B101)→
  `mvn verify` **15 passed**;禁 attach 复跑 15 passed;**jar 启动实测通过**(补 A2 教训:mvn 绿≠jar 可跑)。
- **范围**:`result` 事件 + PG 引用四级回查装配留 **A4**(§8.2 Java 收口);边界 stub → 真 HTTP 客户端在 I1。
- **踩坑**:`SecurityConfig` 挂 `RestAuthEntryPoint` 后,`HealthControllerTest`(@WebMvcTest 切片)须一并 `@Import RestAuthEntryPoint`(切片不自动含 @Component)。

## 待办 / 未决(TODO)

- [x] **TODO-AUTH-001 · v0.4 §7 `permitAll` 鉴权方案存在越权风险**(✅ A1 收口 2026-07-01)(来源:Codex 审查 finding `SEC-AUTH-001`,
  原记于 `audit-ai/.review/findings.json`,审的是 v0.4 设计正文)。
  - **风险**:v0.4 §7 把 Spring Security 过滤链"一律 `permitAll`、只验令牌 + 解析当前用户"——resource-server 会
    **解析**令牌但**不强制**受保护 API 必须认证,使 audit-biz(唯一对外边界)可能在 jCasbin 拿到可信用户**之前**
    就收到**未认证请求**,绕过授权。锚点原文:`- 过滤链关掉表单登录/session/方法级安全,一律 permitAll;Spring
    Security **只留"验令牌 + 解析当前用户"这一件事**`(v0.4 §7 附近)。
  - **处理路径**(spec Java 鉴权层时正面处理,二选一):① 修订 v0.4 §7——受保护 API **强制认证**,`permitAll`
    只留健康检查 / SSO 回调等**显式公开端点**,再对已认证 principal 跑 Casbin;② 带契约/§ 理由反驳该 finding。
  - **已做**:固化为审查红线 `audit-biz-code-review.mdc` #3(permitAll 不得覆盖受保护端点)+ `AGENTS.md` 硬约束。
  - **收口**:✅ A1 已实现 —— `SecurityConfig` 受保护端点强制认证 + permitAll 白名单 `{/health,/sso/callback}`;
    `SecurityConfigTest` protected→401 验证(2026-07-01)。真 SSO 协议(§12 待甲方)定后只换 issuer/starter。
