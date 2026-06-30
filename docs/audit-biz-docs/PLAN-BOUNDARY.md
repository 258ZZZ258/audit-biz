# Implementation Plan: 制度查询步行骨架(SPEC-BOUNDARY v1 的落地)

> SDD 阶段:**Phase 2 PLAN**(待人工审批 → 进 Phase 3 TASKS)。配套 `SPEC-BOUNDARY.md` + `openapi/boundary.v1.yaml`。
> 方法:`planning-and-task-breakdown`(依赖图 + 垂直切片 + 检查点)。

## Overview

把 SPEC-BOUNDARY v1 变成**最薄的一条端到端制度查询线**:
前端 → audit-biz(SSO 验令牌 + jCasbin + 预计算过滤值)→ `POST /v1/query`(SSE,无身份)→ audit-ai(流式 + **轻量引用**)
→ **biz 回查 PG 装配四级引用** → SSE 回前端。一条线验证整套 v0.4 架构(鉴权 / 无身份边界 / SSE / Java 引用收口),不追 RAG 深度。

## Open Questions 解决(PLAN 期读 audit-ai 所得)

- **#1 route_type**:8 值已确认(`query/query/contract.py::RouteType`):evidence/change/case/enumerate/judgmental/statistical/clarify/refuse。
  **且有显式 `review_required: bool`**(R5 判定型=true)→ 边界直接带此 flag,biz 不靠 route_type 推断"人工复核框"。
- **#2 coverage**:= `QueryResult.exhausted_scope: list[str]`(覆盖感知拒答时填,§8.2)+ route_type=`refuse` + `done.finish_reason=refused`。
- **#6 trace**:Langfuse trace 现按 name 内部建、**未接外部 request_id**(`observe.py`)→ Track B 把边界 `request_id` 作 trace id/metadata 注入,实现 biz↔ai 链路关联。

## Architecture Decisions

1. **边界 SSE 对齐 audit-ai §10 契约(`contract.py`)**:复用其词汇 route_type(8)/answer_blocks(`type`∈text|table|case_card|clarify_question,`content`,`stream`)/
   confidence/**ai_label(bool,非 string)**/review_required/exhausted_scope/export_enabled。Track B 工作量降为"流式吐既有 QueryResult + 轻量引用"。→ 驱动 T0.1。
2. **引用回查归属翻转(关键)**:audit-ai 现 `Citation` 由它自己回查 PG 填满(MVP=后端)。v0.4 §8.2 要 audit-ai **无状态、只回 clause_id/chunk_id 轻量引用**,Java 回查装配。
   → Track B 给智能体加**轻量引用模式**(跳过 PG 回查 enrichment,只回 clause_id+confidence+ai_label),回查移到 biz(A4)。**切片最大架构动作 + 风险**。
3. **audit-ai 流式化**:现 `ask()` 产 batch QueryResult(`stream` flag 已预留但 API 非流)→ Track B 在 graph/ask 外加 SSE 适配层,**不动 stage 纯函数**。
4. **步行骨架深度**:首切片**证架构非证 RAG 质量**——最小检索 + 真/canned 答案;auth 用测试令牌 + 最小 jCasbin 策略。
5. **biz 工程基线**:Spring Boot MVC +(Maven 倾向、待 A0 决策)+ 包结构待定。

## Task List(垂直切片,依赖序)

### Phase 0:契约收尾(Foundation)
- **T0.1** 修订 `boundary.v1.yaml` 对齐 §10 + 锁 OQ#1/#2/#6:`ai_label`→bool、加 `review_required`/`exhausted_scope`/answer_block `type`、citations 轻量化、`request_id→trace` 注明。
  - Acceptance:字段与 `contract.py` 一一对照无缺;citations 仅 clause_id/chunk_id+confidence+ai_label。
  - Verify:OpenAPI lint 过 + 两侧字段对照表。Files:`openapi/boundary.v1.yaml`、SPEC §3 微调。Dep:none。Scope:S。

### ✅ Checkpoint A:契约冻结(人工 + 两侧字段对照表确认)

### Phase A:biz 步行骨架(对 stub audit-ai)
- **A0** Spring Boot 脚手架(Maven、包结构、应用起、`/health` permitAll)。需决策:Maven/Gradle、包结构、JDK。Verify:`mvn test` 起 + health 200。Dep:none(可 ‖ T0.1)。Scope:M。
- **A1** SSO resource-server 验令牌:**受保护端点强制认证**,permitAll 仅 `/health`+SSO 回调 = **TODO-AUTH-001 收口**。Verify:无令牌→401、有效→放行,permitAll 边界单测。Dep:A0。Scope:M。
- **A2** jCasbin 授权 + 预计算 `filters`(perm_tags/corpus_types from 当前用户)。Verify:越权→B102、策略单测。Dep:A1。Scope:M。
- **A3** `/query` 控制器 → 边界客户端(**stub**,带 `X-Internal-Token`)→ `SseEmitter` 透传前端。Verify:集成测 stub 流 → 前端 SSE 收齐 meta/delta/citation/done。Dep:A2 + T0.1。Scope:M。
- **A4** 引用四级回查装配(Java 按 clause_id 读 PG `chunks`/`doc_versions` 填完整 `Citation`,§7.3/§8.2)。Verify:给定 clause_id→完整 citation,缺失→降级不崩。Dep:A3 + PG 语料表可达。Scope:M。

### ✅ Checkpoint B:biz 切片对 stub 端到端(人工)

### Phase B:audit-ai 真端点(可与 Phase A 并行,契约冻结后)
- **B1** FastAPI app + `/v1/query` 路由 + 共享密钥校验(`X-Internal-Token`,env)+ 内网 bind。Verify:缺/错令牌→401 B104、pydantic 请求体校验(由 OpenAPI 生成)。Dep:T0.1。Scope:S/M。
- **B2** SSE 流式包查询智能体(stream answer_blocks → meta/delta/citation/done 事件)。Verify:真 `ask()`→SSE 事件序正确。Dep:B1。Scope:M。
- **B3** **轻量引用模式**(跳 PG 回查,只回 clause_id/chunk_id+confidence+ai_label+review_required+exhausted_scope)+ `request_id`→Langfuse trace 注入。Verify:输出无回查字段、trace id=request_id。Dep:B2。Scope:M。**关键架构任务**。
- **B4** `filters`→Milvus 前置过滤(perm_tags/corpus_types/project_id/owner;**owner 仅 audit_project**)。Verify:过滤位生效、owner 不入制度语料。Dep:B2。Scope:M。

### ✅ Checkpoint C:audit-ai 端点契约合规(人工 + Codex 审)

### Phase I:集成
- **I1** biz 边界客户端 stub→真 audit-ai;端到端制度查询跑通(真栈 PG+Milvus+BGE-M3+audit-ai+biz)。Verify:模型门控集成(只有 Claude 能跑,干净栈)。Dep:A4 + B3 + B4。Scope:M。

### ✅ Checkpoint D:步行骨架端到端(人工 + 全栈门跑一次)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| 引用回查翻转使 audit-ai 现 `contract.py` 与 §8.2 冲突 | High | B3 **加轻量模式**而非改既有 `to_dict`;biz A4 接管回查;T0.1 两侧字段对照表锁死 |
| audit-ai 现为 batch、流式化改动面 | Med | B2 在 graph/`ask` 外加 SSE 适配层,**不动 stage 纯函数**;`answer_blocks.stream` 已预留 |
| TODO-AUTH-001 permitAll 越权 | High | A1 受保护强制认证、permitAll 白名单化 + 边界单测 |
| SSO 协议未定(§12 待甲方) | Med | A1 先按 OAuth2/OIDC resource-server 走,协议变只换 starter |
| 跨语言契约漂移 | Med | OpenAPI 单一源 + 两侧 codegen/校验(T0.1 钉工具) |
| 集成需真栈+BGE-M3(Codex/CI 跑不了) | Med | 全栈门由 Claude 干净栈跑(CLAUDE.md 纪律);biz/ai 单元各自 CI |
| 跨 worktree 栈单例并发 | Low | 集成串行,跑前 `demo down -v && demo up` |

## Open Questions(剩余,需你/甲方)

- **OQ#3** `B104` 是否正式回灌 v0.4 §8.3(建议是)。
- **OQ#4** answer_blocks 切块粒度与前端渲染对齐(Track A 前端联调)。
- **OQ#5** `/v1/query` 超时/背压阈值 ⚠(实测标定)。
- **A0 决策**:Maven vs Gradle、包结构、信创 JDK 版本。
- **SSO 最终协议**(甲方,§12)——决定 A1 用哪个 Spring Security 子模块。

## Parallelization

- `T0.1 ‖ A0`(契约修订与脚手架无依赖)。
- **契约冻结(Checkpoint A)后 Phase A(对 stub)‖ Phase B(真端点)** —— 各 worktree/会话,最后 I1 集成。
- A4(biz 回查)与 B3(ai 轻量引用)是回查翻转的两半,**共同遵守 T0.1 字段对照表**。

> 审批后进 **Phase 3 TASKS**(把各 Task 展成带验收/验证/文件清单的可执行单,从 T0.1 + A0 起)。
