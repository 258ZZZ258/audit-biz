# BOUNDARY-RECONCILIATION-001:audit-ai query-api(PR#39)↔ 冻结边界 CP-A 对账

> **状态**:决策已定(用户拍板 2026-07-02,AskUserQuestion)。本文件 = 对账结论 + 给 audit-ai 的 `/v1/query`
> 薄壳 build recipe + biz 侧加法修订清单。**契约规范单一源仍是** `openapi/boundary.v1.yaml`(v1.0.0,Checkpoint A 冻结);
> 语义主本 `SPEC-BOUNDARY.md`。本文件是"两份漂移"的收敛记录,**回灌 audit-ai**(v0.4 §15)。
> **本文件是文档,不是新契约**:audit-ai 的 `/v1/query` 已在 `boundary.v1.yaml`/`SPEC-BOUNDARY.md` §3.1 定义完毕;
> 这里只做"实建 vs 契约"差异对照 + 落地指引 + 一处加法修订(`citation.score`)。

---

## 1. 触发与结论

- **触发**:audit-ai `origin/main` 并入 **PR#39 `feat/query-api`**——query 智能体的 HTTP 层完工。
- **核对结论**:该 API **不是** `boundary.v1.yaml` 里 biz 要调的 `POST /v1/query`,而是**对着产品原型(制度查询智能体页面 V3)
  直连前端**建的一套**会话式富 API**(`/api/query/v1/conversations|clauses|exports|suggestions|uploads`)。
  它**建时完全不感知 biz 边界契约**——在 audit-ai `query/query/api/*` + `docs/query-agent-docs/SPEC-API.md` 全量
  grep `audit-biz / boundary / X-Internal-Token / perm_tags / corpus_types / request_id` **零命中**。疑为 v0.3 单后端
  (Python 独占前端契约)时代形状,未随 v0.4 翻转(CP-011:Java 独占 PG 与前端契约)收敛。
- **关键判断**:**漂移在 HTTP 薄壳形状,不在 AI 内核**。§10 `contract.py` 域契约
  (`QueryResult / AnswerBlock / Citation`)本就是 Checkpoint A 的冻结对齐对象(见 SPEC-BOUNDARY 附录 A),
  `QueryAgent.ask` 产出的 `QueryResult` 可**原样复用**。故守边界对 audit-ai 是"加一层薄壳",非重写。

**决策(2026-07-02)**:
1. **守住 v0.4 冻结边界**——`boundary.v1.yaml` v1.0.0 仍是主本;audit-ai 在现有 `QueryAgent.ask` 之上**加薄壳
   `POST /v1/query`**(无身份 + 收 `filters` + SSE 五事件),会话/身份/导出/引用回查**不进边界**(归 biz)。
2. **四-Tab 结构化结果归 biz**——biz 从 PG 按 `chunk_id` 回查装配(与引用四级回查同源);边界只回轻量 id +
   **per-hit `score`**(加法),四-Tab 分桶由 biz 据 PG `corpus_type` 派生。
3. **否决**「重裁边界、采纳 audit-ai 会话式富 API」——与甲方硬约束(前端交互必须 Java)+ 无身份边界 +
   前置过滤红线冲突,需解冻 CP-A + 甲方级重批,代价过大。

---

## 2. 差异对照(实建 vs 冻结契约)

| # | 维度 | 冻结契约 `boundary.v1.yaml` v1.0.0 | audit-ai 实建(PR#39) | 冲突性质 | 证据(`../audit-ai`) |
|---|---|---|---|---|---|
| BR-1 | 端点/形状 | `POST /v1/query` 单端点、**无状态一次性** SSE | `POST /api/query/v1/conversations/{cid}/messages`(**须先建会话**)+ `/clauses` + `/exports` 会话资源树;SSE 靠 `Accept: text/event-stream` 分支 | 形状不一致 | `query/query/api/app.py`、`routes_messages.py` |
| BR-2 | **前置过滤(红线)** | 必收 `filters{perm_tags[],corpus_types[],project_id?,owner?}`,据此构 Milvus 前置过滤 | `AskBody{query,attachments,include_superseded,corpus(internal\|external)}`——**无字段承载过滤值**,`corpus` 是单值弱子集 | **破一期红线 + SPEC-BOUNDARY §6 Never「检索后过滤」**:权限前置过滤无法生效 | `routes_messages.py::AskBody` |
| BR-3 | 服务间认证/身份 | `X-Internal-Token` 共享密钥、**无身份**(不承载用户身份) | `auth.py` 带 subject/role 主体 +「Casbin/SSO 留接缝」 | 违「无身份边界」;授权归属越界(授权归 biz jCasbin) | `query/query/api/auth.py`、SPEC-API §2/§13 |
| BR-4 | 引用回查归属 | 边界只回 `clause_id(+chunk_id?)`;**四级回查由 biz 收口装配**(§8.2) | `GET /clauses/{clause_id}` 由 audit-ai 回查 PG(原文/释义/定义) | 违 SPEC-BOUNDARY §6 Never「让 audit-ai 回查 PG 装 citation」 | `query/query/api/routes_clauses.py` |
| BR-5 | 状态/会话归属 | Java 独占 PG 状态(v0.4 翻转);边界无状态 | 新建 PG `query_*` 会话/消息表,持久化会话历史 | 两份状态漂移(会话状态两个 owner) | SPEC-API §0/§3;`routes_conversations.py`、`service.py::store` |
| BR-6 | 导出归属 | 报告/Excel/Word 渲染归 biz(POI/docx4j) | `POST /conversations/{cid}/messages/{mid}/export` 出 xlsx | 导出归属越界 | `query/query/api/routes_export.py`、`export_xlsx.py` |
| BR-7 | SSE 事件词汇 | `meta / delta / citation / done / error` | `structured / answer_delta / done`(SPEC-API §6) | 事件词汇不一致 | SPEC-API §6;`query/query/api/sse.py` |
| BR-8 | 四-Tab 结构化结果 | 契约未覆盖(边界只回轻量 id) | `StructuredResult`(命中制度/条款/监管规则/案例 + 匹配度…) | 真实前端需求,但**归属 biz**(决策②) | SPEC-API §4;`service.py::structured_for` |

> **注**:BR-1/3/5/6 = audit-ai 建了**归 biz 的东西**;BR-2 = **红线级**,直接接会让权限过滤失效,是"不能直接接"的硬理由;
> BR-4 = SPEC-BOUNDARY §6 明列 Never;BR-8 = 真实前端需求,决策②归 biz 装配。

---

## 3. 给 audit-ai 的动作:加薄壳 `POST /v1/query`(build recipe)

**目标**:在**不改 AI 内核**(`QueryAgent.ask` / 检索 / 生成 / `contract.py`)的前提下,新增一层符合
`boundary.v1.yaml` 的薄壳端点。等价于:`filters → Milvus 前置过滤 → QueryAgent.ask → QueryResult → SSE(五事件)`。

### 3.1 端点与鉴权
- 新增 `POST /v1/query`(**独立于** `/api/query/v1/*`;可新建 boundary router 或独立 app)。
- **鉴权 = 静态共享密钥 header** `X-Internal-Token` == env `AUDIT_AI_INTERNAL_TOKEN`;缺失/不符 → `401 B104`。
- **无身份**:**不要**复用 `auth.py` 的 subject/role;此端点不解析、不承载用户身份。仅"证明调用方是 audit-biz"。

### 3.2 请求(`QueryRequest`,见 `boundary.v1.yaml#/components/schemas/QueryRequest`)
```jsonc
{
  "query": "……",                 // 必填,minLength 1
  "request_id": "01J…ULID",       // 必填,回显 + 注入 Langfuse trace(见 3.5)
  "filters": {                     // 必填
    "perm_tags": ["…"],            // 必填(可空数组);密级/职级标签 → Milvus 标量前置过滤位
    "corpus_types": ["internal"],  // 必填;enum internal/external/qa/case/audit_project → Milvus 分区
    "project_id": null,            // 可空;项目资料联合检索隔离位,制度查询为 null
    "owner": null                  // 可空;**仅当 corpus_types 含 audit_project 生效**,制度检索忽略
  },
  "options": { "top_k": 8, "include_superseded": false }  // 可选
}
```

### 3.3 前置过滤(红线,BR-2 的收口)
- **必须**据 `filters` 构造 Milvus filter,在**检索前**生效(RAG v1.5 §5.5「检索前置过滤」):
  `corpus_types` → 分区选择;`perm_tags` → 标量过滤位;`project_id`/`owner` → 行级隔离(owner 仅 `audit_project`)。
- 复用/扩 `query` 现有检索层的 filter 构造(把 biz 下传的过滤值作为**普通字段**注入,不做权限判断——权限已在 Java 算好)。
- ⚠ **这是 audit-ai 侧唯一的实质新工作**;其余是薄壳序列化。若现检索层不支持 `perm_tags` 标量位,需补(RAG §5.5 红线)。

### 3.4 响应(SSE 五事件,从 `QueryResult` 映射;词汇见 SPEC-BOUNDARY 附录 A)
事件序:`meta` →(`delta`/`citation` 交错)→ `done`;任意时刻可 `error` 后关闭。
- `meta`:`{request_id, route_type(8值), ai_label:true, review_required, export_enabled?}` ← `QueryResult` 头部字段。
- `delta`:`{block_seq, block_type(text/table/case_card/clarify_question), text}` ← `AnswerBlock`(`stream=false` 的块作单条整块下发)。
- `citation`:`{clause_id, chunk_id?, score}` ← 命中项。**`clause_id` 即 chunk_id 值**(anchors.py 现状,biz 已坐实 TODO-CITE-KEY-001)。
  **`score` = 加法新增**(见 §4):per-hit 检索融合分,供 biz 装四-Tab 匹配度。
- `done`:`{finish_reason(stop/refused/length/error), confidence?, exhausted_scope?, token_usage?}` ← `QueryResult` 尾部。
- `error`:`{code, message}`(段位 E1xx–E8xx / B1xx–B4xx,含 B104)。

### 3.5 观测
- 把请求 `request_id` **注入 Langfuse trace id**,保「前端 query_id ≡ 边界 request_id ≡ 观测 trace」一条链
  (SPEC-BOUNDARY §8 已解 #6;`observe.py` 现按 name 内部建,需接受外部 id)。

### 3.6 明确**不进边界**(留 biz,勿在 `/v1/query` 复刻)
- ❌ 会话/消息持久化(`query_*` 表、`store`)——状态归 biz PG。
- ❌ `GET /clauses/{id}` 式 PG 引用回查——四级回查由 biz 收口(§8.2)。
- ❌ 导出(xlsx/报告)——归 biz(POI/docx4j)。
- ❌ 用户身份/Casbin/SSO——授权归 biz jCasbin,边界无身份。
- ℹ️ audit-ai 已建的 `/api/query/v1/*` 会话式 API 是否留作他用(如内部工作台)或弃,由 audit-ai 侧定;**但它出 biz 边界**,
  biz 只认 `/v1/query`。四-Tab 的 `structured_for` 逻辑可留 audit-ai 复用/参考,但**边界不透传** `structured`(决策②)。

---

## 4. biz 侧加法修订(不阻塞,可并行 SDD)

均为**加法演进**(One-Version Rule §3.4,不破冻结契约):

1. **`openapi/boundary.v1.yaml` → v1.1.0**:`QueryCitationEvent` 加可选 `score`(number 0–1,per-hit 融合分归一)。
   理由:四-Tab 匹配度需 per-hit 分,biz 无法自算(不检索)。属"加可选字段不破契约"。
2. **`frontend.regquery.v1.yaml`**:四-Tab 已在 `result` 事件(**扁平** `regulations/clauses/rules/cases` + `counts`,
   建仓时按原型 V3 建),**不重构成 `structured` 嵌套**(避免破坏 A4 已实现的 `result` 发射)。本次(v1.1.0-draft)补:
   各 hit 加 `match_score`(接边界 `citation.score`)+ 原型缺字段(发布/生效日期、发文机关、文号、核心要求、适用主题、
   引用建议 `citation_advice`、要求提炼卡 `regulatory_digest`);LLM/L2 富集字段 nullable 默认降级(零臆造)。
3. **扩 A4 `CitationAssembler` → 四-Tab 装配**:biz 按 `chunk_id` 回查 PG `chunks ⋈ doc_versions`(+ `cases`),
   据 `corpus_type` 分桶(制度/条款/监管规则/案例),`score` 取自边界 `citation.score`。基于 `StubBoundaryClient` 先跑通,I1 换真源。
   → 与既有 `Citation` 四级回查同源,复用 `CitationMapper`。

---

## 5. 红线核对(守边界后)

| 一期红线 / SPEC-BOUNDARY §6 | 守边界方案是否满足 |
|---|---|
| 检索前置过滤(算在 Java、用在 Python) | ✅ `/v1/query` 收 `filters` 构 Milvus 前置过滤(§3.3) |
| 无身份边界(不传用户 JWT) | ✅ `X-Internal-Token` 无身份;不复用 `auth.py` subject |
| 引用四级回查 Java 收口 | ✅ 边界只回轻量 id + score;biz 装配(§4.3) |
| Java 持前端契约 + PG 权威 | ✅ 会话/状态/导出/四-Tab 归 biz |
| 单向只读、不回写源系统 | ✅ 薄壳只读检索;biz PG add-only |

---

## 6. 时序与阻塞

- **阻塞 I1**(`StubBoundaryClient` → 真 HTTP 客户端 + SSE 解析):待 audit-ai 交付 §3 的 `/v1/query`。
- **不阻塞**:§4 的 biz 加法(契约 bump + 四-Tab 装配)现可推进(基于 stub)。
- **回灌**(v0.4 §15):本文件结论回灌 audit-ai(其侧留指针引用本主本);audit-ai `/v1/query` 落地后,
  在 biz `TODO-BOUNDARY-RECON-001` 回填实际 commit / DTO,并据实核对 §3 逐项。

## 7. 附:audit-ai 证据文件清单(`../audit-ai`,`origin/main` @ PR#39)
- `query/query/api/app.py`——FastAPI 工厂,base path `/api/query/v1`。
- `query/query/api/routes_messages.py`——`AskBody`(无 filters);SSE 靠 `Accept` 分支(BR-1/BR-2/BR-7)。
- `query/query/api/auth.py`——subject/role 身份 stub(BR-3)。
- `query/query/api/routes_clauses.py`——`/clauses/{id}` PG 回查(BR-4)。
- `query/query/api/routes_conversations.py`、`service.py`——会话持久化 `query_*`(BR-5)。
- `query/query/api/routes_export.py`、`export_xlsx.py`——导出(BR-6)。
- `query/query/api/service.py::structured_for`、SPEC-API §4——四-Tab(BR-8,归 biz 装配)。
- `docs/query-agent-docs/SPEC-API.md`——audit-ai 侧 API 设计规格(前端接缝口径)。

## 8. 跨仓引用坐标(remote ↔ remote)

保证两仓**远端**互相可解析本方案(不依赖同机 `../` 相对路径):

- **audit-biz(本仓 = 主本)**:`https://github.com/258ZZZ258/audit-biz`
  - 本方案:`docs/audit-biz-docs/BOUNDARY-RECONCILIATION-001.md`
  - 契约(规范单一源):`docs/audit-biz-docs/openapi/boundary.v1.yaml`(v1.1.0)
- **audit-ai(姊妹仓 = 照做方)**:`https://github.com/258ZZZ258/audit-ai`
  - 指针(引本主本,入库):`docs/query-agent-docs/BOUNDARY-biz-contract-pointer.md`
  - 可执行 finding(gitignore·同机 scratch,不入库):`.review/findings.json` → `boundary.contract.query-api-drift`
  - 待改代码:`query/query/api/*`(**main 分支**;新增 `/v1/query` 薄壳)

> §2/§7 里的 `../audit-ai/...` 是同机相对路径;对应远端坐标即上表 audit-ai 仓
> (`../` 只在两仓做姊妹目录的机器上解析;远端/他机/CI 用上表 URL)。
