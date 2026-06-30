# Tasks: 制度查询步行骨架(SPEC/PLAN-BOUNDARY 的可执行任务单)

> SDD 阶段:**Phase 3 TASKS**(待人工审批 → 进 Phase 4 IMPLEMENT,逐任务 TDD)。源:`PLAN-BOUNDARY.md`。
> **基线决策(已定)**:Maven · JDK 17 LTS(信创发行版待甲方)· Spring Boot 3.x · 根包 `com.dfzq.auditai.biz` · B104 回灌 v0.4 §8.3。
> **跨仓**:`T0.1`/`A*`/`I1` 在 **audit-biz 仓**;`B1–B4` 在 **audit-ai 仓**(姊妹仓,见 CLAUDE.md 定位)。契约冻结后 A 轨 ‖ B 轨,各 worktree。

## Definition of Done(每个任务都过的标准底线)

- 对应单测/集成测**先红后绿**(TDD);改动波及范围测试通过。
- lint 绿:biz `mvn spotless:check`(或 checkstyle)/ audit-ai `ruff check`。
- 交 Codex 审(`code-review-and-quality` + 安全敏感项加 `security-and-hardening`),修复闭环至无 critical/warning。
- 不破红线(无身份边界 / 过滤值预计算 / 引用 Java 收口 / owner 语料隔离 / add-only / 密钥外置)。

---

## Phase 0:契约收尾

### Task T0.1:OpenAPI 对齐 §10 契约 + B104 回灌
**Description:** 把 `boundary.v1.yaml` 与 audit-ai `query/query/contract.py`(§10 统一输出契约)逐字段对齐,并把 B104 正式写入 v0.4 §8.3。产出**两侧字段对照表**附于 SPEC。
**Acceptance:**
- [ ] `ai_label` 改 **bool**;新增 `review_required`(bool)、`exhausted_scope`(string[])、answer_block `type`(text|table|case_card|clarify_question)。
- [ ] `citation` 事件**轻量化**:仅 `chunk_id`/`clause_id`/`confidence`/`ai_label`(无 doc_title/page/version 等回查字段);`request_id→Langfuse trace` 在 SPEC 注明。
- [ ] v0.4 §8.3 B1xx 段新增 `B104 内部令牌无效(服务向)`。
**Verify:** OpenAPI lint 零报错;`contract.py` ↔ OpenAPI 字段对照表无缺漏。
**Dependencies:** None。**Files:** `openapi/boundary.v1.yaml`、`SPEC-BOUNDARY.md`、`docs/审计大模型系统_后端总体技术框架设计_v0_4.md`(§8.3)。**Scope:** S。

### ✅ Checkpoint A:契约冻结(人工 + 字段对照表确认)→ A 轨 / B 轨可并行

---

## Phase A:biz 步行骨架(audit-biz 仓,对 stub)

### Task A0:Spring Boot 脚手架
**Description:** Maven + JDK 17 + Spring Boot 3.x 工程;根包 `com.dfzq.auditai.biz`;`AuditBizApplication` 起;`/health` permitAll;pom 声明 MyBatis-Plus / jCasbin / `spring-boot-starter-oauth2-resource-server` / POI(后续任务配置)。
**Acceptance:**
- [ ] 应用启动成功;`GET /health` → 200。
- [ ] 包目录建好:`config/ controller/ security/ authz/ client/ citation/ dto/`。
**Verify:** `mvn -q test`(冒烟测绿);`mvn spring-boot:run` 后 `curl /health`。
**Dependencies:** None(可 ‖ T0.1)。**Files:** `pom.xml`、`src/main/java/com/dfzq/auditai/biz/AuditBizApplication.java`、`controller/HealthController.java`、`src/main/resources/application.yml`、`src/test/java/.../AuditBizApplicationTests.java`。**Scope:** M。

### Task A1:SSO resource-server 验令牌(TODO-AUTH-001 收口)
**Description:** 最小化 Spring Security resource-server(先按 OAuth2/OIDC JWT);过滤链**受保护端点强制认证**,permitAll 仅白名单 `{/health, /sso/callback}`;从令牌解析当前用户。
**Acceptance:**
- [ ] 受保护端点:无/无效令牌 → 401 `B101`;有效令牌 → 放行且 `CurrentUser` 可取。
- [ ] permitAll 白名单显式枚举,**绝不 `permitAll()` 全放**(TODO-AUTH-001)。
**Verify:** 测试:受保护无令牌→401、mock JWT→200、白名单无令牌→200。
**Dependencies:** A0。**Files:** `config/SecurityConfig.java`、`security/CurrentUserArgumentResolver.java`、`application.yml`(issuer/jwks)、`test/SecurityConfigTest.java`。**Scope:** M。

### Task A2:jCasbin 授权 + 过滤值预计算
**Description:** jCasbin enforcer(策略存 PG `casbin_rule`)做授权;按当前用户**预计算** `filters`(perm_tags/corpus_types),owner/project_id 按 §7.x 规则。
**Acceptance:**
- [ ] 越权 → `B102`;`Filters` 由用户属性算出。
- [ ] owner 仅在 corpus_types 含 `audit_project` 时填,制度语料不带 owner。
**Verify:** 测试:授权 allow/deny;`FilterResolver` 单测(含 owner 隔离)。
**Dependencies:** A1。**Files:** `config/CasbinConfig.java`、`authz/Authorizer.java`、`authz/FilterResolver.java`、`resources/casbin/model.conf`、`test/FilterResolverTest.java`。**Scope:** M。

### Task A3:/query 控制器 + 边界客户端(stub) + SseEmitter
**Description:** `POST /api/regulation/query`(对前端)→ 控制器 → `BoundaryClient`(接口 + `StubBoundaryClient`)带 `X-Internal-Token` 调 `/v1/query` → `SseEmitter` 透传 meta/delta/citation/done。
**Acceptance:**
- [ ] stub 流式事件经 `SseEmitter` 透传前端;`X-Internal-Token` 已带。
- [ ] route_type=judgmental/`review_required=true` 标识透传(前端据此渲染人工复核框)。
**Verify:** 集成测(MockMvc)断言前端 SSE 收齐事件序。
**Dependencies:** A2 + T0.1。**Files:** `controller/QueryController.java`、`client/BoundaryClient.java`、`client/StubBoundaryClient.java`、`dto/boundary/*.java`、`test/QueryControllerTest.java`。**Scope:** M。

### Task A4:引用四级回查装配(回查翻转的 biz 半)
**Description:** Java 按 `clause_id` 读 PG `chunks`/`doc_versions` 装配完整 `Citation`(doc_title/doc_no/clause_path/page/version/status,§7.3/§8.2),拼成对前端契约 `answer_blocks[] + citations[]`。
**Acceptance:**
- [ ] 给定 clause_id → 完整 citation;缺失 → 降级(跳过/标记),不崩。
- [ ] audit-ai 回的轻量引用**不含**回查字段(契约合规)。
**Verify:** 针对 seed PG 的测试;缺失 clause_id 路径覆盖。
**Dependencies:** A3 + PG 语料表可达。**Files:** `citation/CitationAssembler.java`、`citation/CitationMapper.java`、`dto/frontend/*.java`、`test/CitationAssemblerTest.java`。**Scope:** M。

### ✅ Checkpoint B:biz 切片对 stub 端到端(人工)

---

## Phase B:audit-ai 真端点(audit-ai 仓,契约冻结后 ‖ Phase A)

### Task B1:FastAPI app + /v1/query + 共享密钥 + 内网 bind
**Description:** audit-ai 内新增 FastAPI 服务层暴露 `POST /v1/query`;`X-Internal-Token` 校验(env `AUDIT_AI_INTERNAL_TOKEN`);pydantic 请求模型(对齐 OpenAPI);仅内网 bind。
**Acceptance:**
- [ ] 缺/错令牌 → 401 `B104`;请求体非法 → 422;路由通。
**Verify:** pytest:坏令牌→401、坏体→422、stub 200。
**Dependencies:** T0.1。**Files(audit-ai):** `query/query/service/app.py`、`service/auth.py`、`service/models.py`、`query/tests/test_service_app.py`。**Scope:** S/M。

### Task B2:SSE 流式包查询智能体
**Description:** 在 `graph`/`ask` 外加 SSE 适配层,把 `QueryResult` 流式吐为 meta/delta/citation/done;**不动 stage 纯函数**;复用 `AnswerBlock.stream` 语义。
**Acceptance:**
- [ ] 真 `ask()` → 正确 SSE 事件序(meta→delta/citation 交错→done)。
**Verify:** pytest 流式测断言事件序。
**Dependencies:** B1。**Files(audit-ai):** `service/app.py`、`service/sse.py`、`query/tests/test_service_sse.py`。**Scope:** M。

### Task B3:轻量引用模式 + request_id→trace(回查翻转的 ai 半)★
**Description:** 边界路径**跳过 PG 回查 enrichment**,citation 只回 clause_id/chunk_id+confidence+ai_label+review_required+exhausted_scope;把边界 `request_id` 注入 Langfuse trace(id/metadata)。
**Acceptance:**
- [ ] 输出 citation **无回查字段**(doc/page/version 等);`done` 带 review_required/exhausted_scope。
- [ ] observe trace id == request_id(开 observe 时)。
**Verify:** pytest:citation 形态轻量;trace 注入 request_id。
**Dependencies:** B2。**Files(audit-ai):** `service/app.py`、`service/models.py`、`query/observe.py`(小改)、tests。**Scope:** M。**关键架构任务。**

### Task B4:filters → Milvus 前置过滤
**Description:** 把边界 `filters`(perm_tags/corpus_types/project_id/owner)映射为 Milvus 标量前置过滤;**owner 仅当 corpus_types 含 audit_project**,不作用于制度语料。
**Acceptance:**
- [ ] 过滤位在检索前生效;owner 语料隔离规则成立。
**Verify:** pytest:过滤构造;owner 不入制度语料用例。
**Dependencies:** B2。**Files(audit-ai):** `retrieve/hybrid.py`(或过滤构造处)、`service/app.py`、tests。**Scope:** M。

### ✅ Checkpoint C:audit-ai 端点契约合规(人工 + Codex 审)

---

## Phase I:集成

### Task I1:stub → 真 audit-ai,端到端跑通
**Description:** biz `StubBoundaryClient` 换 `HttpBoundaryClient`(真调 audit-ai `/v1/query`);制度查询端到端在真栈(PG+Milvus+BGE-M3+audit-ai+biz)跑通。
**Acceptance:**
- [ ] 端到端:鉴权→预计算过滤→无身份边界→SSE→biz 回查装配→前端 SSE,全链路通。
**Verify:** **模型门控集成(只有 Claude 能跑,干净栈)**:跑前 `demo down -v && demo up`。
**Dependencies:** A4 + B3 + B4。**Files:** `client/HttpBoundaryClient.java`、配置接线、集成测。**Scope:** M。

### ✅ Checkpoint D:步行骨架端到端(人工 + 合并前全栈门跑一次)

---

## 剩余 Open Questions(实现期/联调期解决,不阻塞起步)
- OQ#4 answer_blocks 切块粒度与前端渲染对齐(A3/前端联调)。
- OQ#5 `/v1/query` 超时/背压阈值 ⚠(实测标定,A3/I1)。
- SSO 最终协议(甲方,§12)→ 定 A1 用哪个 Spring Security 子模块。

> 审批后进 **Phase 4 IMPLEMENT**:从 **T0.1** 起(契约冻结是并行前提),逐任务 `incremental-implementation` + `test-driven-development`,每任务过 DoD + Codex 审。
