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
- **审查修复(Codex,3 条 warning)**:
  - `API-VALIDATION-001`——入口没校验 `QuerySubmit` → 加 `spring-boot-starter-validation` + `@NotBlank/@Size`,`@Valid`;
    `GlobalErrorHandler` 覆盖 `MethodArgumentNotValidException`/`HttpMessageNotReadableException` → 422 B201;补缺/空 question、坏 JSON 用例。
  - `BOUNDARY-REQUEST-ID-001`——`BoundaryClient` 缺 `request_id`(契约必填,前端 query_id ≡ 边界 request_id ≡ 观测 trace 一条链)→
    签名加 `requestId`,控制器传同一个 queryId;测试用捕获替身断言 request_id 下传 = query_id。
  - `SSE-FRONTEND-CONTRACT-001`——context 缺 `session_id`/`current_question`、review 层级错 → 按 ContextEvent 契约补齐
    (session_id=body.sessionId∥queryId、current_question=question、review{required,status});测试改**解析 SSE JSON 断言形态**。
  - **踩坑**:MockMvc `getContentAsString()` 默认非 UTF-8 解 SSE → 中文乱码,须 `getContentAsString(UTF_8)`。

## 2026-07-01 · A4 引用四级回查装配 + result 事件(Testcontainers)

- **落地**:`CitationMapper`(MyBatis-Plus,按 **chunk_id** 批量读 `chunks⋈doc_versions`)+ `CitationAssembler`
  (装 `Citation`{chunkId/clauseId/docTitle/docNo/clausePath/page/version/status};**无 id 短路、回查失败/缺失降级空引用不崩**,§8.2 韧性)。
  `BoundaryClient` 加 `onCitation`;`QueryController` 收集 chunk_id → `onDone` 回查装配发 **`result` 事件**(counts + clauses)→ done。
- **数据源**:MyBatis-Plus + PG 驱动;`spring.datasource` env 驱动 + `hikari.initialization-fail-timeout:-1`(**无 PG 也能启动**,按需连接)。@MapperScan 放 `PersistenceConfig`(非主类,免 @WebMvcTest 切片扫 mapper 报错)。
- **验收**:17 单测(surefire)绿 + spotless + **jar 启动实测通过**;`CitationAssemblerIT`(Testcontainers PG 回查真装配)——见下 Docker 怪癖。
- **踩坑**:
  - **application.yml 层级**:插 `mybatis-plus:` 顶层键时把 `spring.security...jwk-set-uri` 挤到 mybatis-plus 下 → 无 JwtDecoder、全 @SpringBootTest 挂。YAML 缩进敏感,改后逐块核。
  - **Testcontainers 本机跑不了(环境非代码)**:本机 **Docker 29.5.3** 要求 API ≥1.40,docker-java 默认发 1.32 → 400 `client version 1.32 is too old`;`DOCKER_API_VERSION`/换 socket/升 TC 1.20.4 均无效(docker-java ping 前不协商)。**IT 交 GitHub CI 验**(标准 Docker 接受 1.32)。
- **审查修复(Codex,2 critical)**:
  - `CONFIG-SECRET-DEFAULTS-001`——application.yml 提交了 PG url/账号/密码默认值(弱凭据入库)→ 改**嵌入式 H2 默认**(无凭据、dev/CI 可启动)+ 加 h2 runtime;生产经 env `DB_URL`+`SPRING_DATASOURCE_*` 覆盖真 PG。去掉 hikari fail-timeout hack(H2 常在)。
  - `BOUNDARY-CITATION-KEY-001`——只收 `chunkId` 会把契约合法的 `clause_id`(必填)引用全丢 → **clause_id 优先 + chunk_id 兼容 fallback**(契约合规,不改冻结契约;完整对齐留 TODO 与 B 轨定)。
- **待对齐(TODO-CITE-KEY-001)**:回查主键实为 **chunk_id**(audit-ai chunks 表主键,无独立 clause 列);冻结契约 §8.1 写 clause_id 必填/chunk_id 可选,与实现相反。见 TODO。

## 2026-07-02 · 边界对账:audit-ai query-api(PR#39)与冻结边界 CP-A 冲突 → 守边界

- **触发**:audit-ai `origin/main` 并入 **PR#39 `feat/query-api`**(query HTTP 层完工)。核对发现它**不是**
  `boundary.v1.yaml` v1.0.0(Checkpoint A 冻结)那个端点,而是**对着产品原型直连前端**建的会话式富 API
  (疑 v0.3 单后端遗留形状)——全仓 grep 无 `audit-biz/boundary/X-Internal-Token/perm_tags`,**建时完全不知 biz 边界**。
- **关键判断**:漂移在 **HTTP 薄壳形状**,**不在 AI 内核**——§10 `contract.py` 域契约(`QueryResult/AnswerBlock/Citation`)
  本就是 CP-A 的冻结对齐对象(SPEC-BOUNDARY 附录 A),`QueryAgent.ask` 产 `QueryResult` 可原样复用。故守边界成本低。
- **四处硬冲突**(证据文件均在 `../audit-ai`):
  - 端点形状:契约 `POST /v1/query` 单端点、无状态一次性 SSE ↔ 实建 `POST /api/query/v1/conversations/{cid}/messages`
    (**需先建会话**)+ `/clauses` + `/export` 会话资源树(`query/query/api/app.py`、`routes_messages.py`)。
  - **前置过滤红线**:契约必收 `perm_tags/corpus_types/project_id/owner` ↔ `AskBody` 只有
    `query/attachments/include_superseded/corpus(internal|external)`,**收不了过滤值** → 直接接 = 权限前置过滤失效
    (破一期红线 + SPEC-BOUNDARY §6 Never「检索后过滤」)。
  - 无身份:契约 `X-Internal-Token` 无身份 ↔ `query/query/api/auth.py` 带 subject/role +「Casbin/SSO 留接缝」(授权归属越界)。
  - 状态/回查/导出归属:v0.4 翻转 = Java 独占 PG 状态 + 引用四级回查 + 前端契约 ↔ audit-ai 自建 `query_*` 会话表 +
    `/clauses/{id}` 四级回查(`routes_clauses.py`)+ `/export` xlsx → 违 SPEC-BOUNDARY §6 Never「让 audit-ai 回查 PG 装 citation」,两份漂移。
- **决策(用户拍板 2026-07-02,AskUserQuestion)**:
  - ① **守住 v0.4 冻结边界**(`boundary.v1.yaml` v1.0.0 仍主本):要求 audit-ai 在**现有 `QueryAgent.ask` 之上加薄壳**
    `POST /v1/query`(无身份 + 收 filters + SSE `meta/delta/citation/done/error`);会话/身份/导出/回查**不进边界**(归 biz)。
    其 `QueryAgent.ask`/`structured_for` 域逻辑保留复用;已建的 `/api/query/v1/*` 会话式 API 是否留作他用或弃,由 audit-ai 侧定,**但出边界**。
  - ② **四-Tab 结构化结果归 biz**:biz 从 PG 按 `chunk_id` 回查装配(与引用四级回查同源),守「Java 持前端契约」;
    边界只回轻量 id + **per-hit `score`**(加法),四-Tab 分桶由 biz 据 PG `corpus_type` 派生。
- **否决**:「重裁边界、采纳 audit-ai 会话式富 API」——与甲方硬约束(前端交互必须 Java)+ 无身份边界 + 前置过滤红线冲突,
  需解冻 CP-A + 甲方级重批,代价过大。
- **产物**:`docs/audit-biz-docs/BOUNDARY-RECONCILIATION-001.md`(对账 + 给 audit-ai 的 `/v1/query` 薄壳 build recipe + 加法修订清单)。
  回灌路径见 v0.4 §15(以 CP 回灌 audit-ai)。
- **影响**:**I1(真 HTTP 客户端替 `StubBoundaryClient`)阻塞**,待 audit-ai 交付 `/v1/query` 薄壳;biz 侧四-Tab 装配 +
  两份契约加法(见下 TODO)**不阻塞**,现可推进。见 `TODO-BOUNDARY-RECON-001`。

## 待办 / 未决(TODO)

- [x] **TODO-CITE-KEY-001 · 回查主键 clause_id vs chunk_id**(✅ 坐实,2026-07-01):查 audit-ai `query/query/generate/anchors.py`
  明写 **`clause_id(=chunk_id)`**、`Citation(clause_id=cid)`(cid 即 chunk_id)、`r1_evidence.py "clause_id": c.chunk_id`——
  **audit-ai 的 clause_id 就是 chunk_id 的值**(chunks 表主键即 chunk_id、无独立 clause 列)。故契约"clause_id 必填回查键"**功能上成立**:
  A4 按 clause_id 值回查 `chunks.chunk_id` 恒匹配、不丢引用(BOUNDARY-CITATION-KEY-001 收口:走 Codex 方案 a,坐实注释 + `RegulationQueryCitationIT` clause_id-only 测试)。
  **不需破坏性契约变更**;可选后续:给 boundary.v1.yaml `clause_id` 补一句"= chunk PK"的说明(additive,非破坏)。

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

- [ ] **TODO-BOUNDARY-RECON-001 · audit-ai `/v1/query` 薄壳适配(I1 阻塞项)**(2026-07-02 开):详见
  `docs/audit-biz-docs/BOUNDARY-RECONCILIATION-001.md`。audit-ai query-api(PR#39)与冻结边界 CP-A 冲突,决策=守边界。
  - **阻塞 I1**:待 audit-ai 交付符合 `boundary.v1.yaml` 的 `POST /v1/query`(无身份 `X-Internal-Token` + `filters`→Milvus 前置过滤
    + SSE 五事件 + `citation.score` 加法)后,biz 做 I1(`StubBoundaryClient` → 真 HTTP 客户端 + SSE 解析)。
  - **biz 侧不阻塞并行项**:边界 `boundary.v1.yaml` bump **v1.1.0**(加 `citation.score`)+ 前端契约 `frontend.regquery.v1.yaml`
    加 `result.structured` 四-Tab(加法);扩 A4 `CitationAssembler` → 四-Tab PG 回查装配(基于 stub 先跑通)。均走 SDD。
  - **跟踪**:回灌 audit-ai(v0.4 §15)后,记其 `/v1/query` 落地 commit / 实际 DTO 于本 TODO。
