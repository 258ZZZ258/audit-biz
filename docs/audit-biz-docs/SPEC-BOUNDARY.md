# SPEC: audit-biz ↔ audit-ai 边界契约（边界二，Track0）

> SDD 阶段:**Phase 1 SPECIFY**(待人工审批→进 PLAN)。规范单一源 = `openapi/boundary.v1.yaml`;本文件讲语义、约束、边界、成功标准。
> 上游设计:v0.4 §6.3 / §7(边界二)/ §8;RAG 设计 v1.5 §5.5(前置过滤)/ §5.9(citation 字段)/ 路由章节。

## 1. Objective(建什么 / 为什么 / 何为完成)

定义 **audit-biz(Java,唯一对外入口)→ audit-ai(Python,无状态端点)** 的内网服务间 REST 契约(v0.4 边界二)。
本版(v1)只锁**制度查询同步热路径** `POST /v1/query`(SSE),其余端点(`/compare` `/ocr` `/ingest-batch`)占位。

- **为什么先做它**:契约是两轨(biz 步行骨架 ‖ audit-ai FastAPI 端点)的唯一阻塞项;锁定后两侧可并行,biz 用 stub 顶 audit-ai。
- **用户/消费者**:本契约的两个消费者都是机器——audit-biz 的边界客户端(生产者侧 stub→真)、audit-ai 的 FastAPI 端点(Track B)。
- **成功长相**:见 §7 成功标准。核心 = 两侧能各自 codegen/校验同一份 OpenAPI,步行切片端到端跑通。

**范围边界(明确不含)**:
- 边界一(用户↔甲方 SSO 认证)、permitAll/`TODO-AUTH-001` → 归 **Track A 鉴权 SPEC**,本契约仅假定"biz 已认证授权完毕、过滤值已算好"。
- audit-biz → 前端 的 SSE/JSON 契约(answer_blocks[] + 完整 citations[],RAG §5.9)→ 归 **Track A**;本契约只到"audit-ai 回轻量标识、biz 回查装配"为止。
- `/compare` `/ocr` `/ingest-batch` 的 DTO → 各自 SPEC。

## 2. Transport / 选型

- 内网 **REST/JSON over HTTP**;流式用 **SSE(text/event-stream)**。audit-ai **仅对 audit-biz 网络可达**(网络隔离)。
- 生产者侧 audit-biz = Spring MVC `SseEmitter` 透传;消费者侧 audit-ai = FastAPI(Track B)。
- 契约规范 = **OpenAPI 3.0**(`openapi/boundary.v1.yaml`),两侧 codegen/校验的单一源。
- **服务间认证 = 静态共享密钥 header** `X-Internal-Token`(两侧 env `AUDIT_AI_INTERNAL_TOKEN` 注入,绝不入库)+ 网络隔离。
  **无身份**:本头只证明调用方是 audit-biz,不承载用户身份。缺失/不符 → `401 B104`。

## 3. 契约定义(以 OpenAPI 为准,此处讲语义)

### 3.1 `POST /v1/query`(制度查询,单流式端点)

- **请求**(`QueryRequest`):`query`(问题)+ `request_id`(ULID,贯穿 Langfuse 观测,非幂等键)+ `filters` + `options`。
- **`filters`(前置过滤,Java 预计算)**:`perm_tags[]`(密级/职级)、`corpus_types[]`(internal/external/qa/case/audit_project,Milvus 分区)、
  `project_id?`、`owner?`。**owner 仅当 corpus_types 含 `audit_project` 时生效,制度语料检索忽略**(§7.x)。
  → 检索前置过滤红线满足:**算在 Java、用在 Python**(RAG §5.5)。
- **响应**:SSE 流。事件序 `meta` →(`delta`/`citation` 交错)→ `done`;任意时刻可 `error` 后关闭。
  **字段词汇与 audit-ai `query/query/contract.py`(§10 统一输出契约)逐字对齐**——见附录 A 对照表。
  - `meta`(响应级头):`request_id` + `route_type`(8 值枚举)+ **`ai_label`(bool,恒 true)** + **`review_required`(bool;route=judgmental→true,biz 渲染"AI 辅助判断,人工复核"框)** + `export_enabled?`。
  - `delta`:`block_seq` + `block_type`(text/table/case_card/clarify_question)+ `text`(增量或整块;`contract.py` `AnswerBlock.stream=false` 的块作单条整块)。
  - `citation`:**轻量标识,只回 `clause_id`(+`chunk_id?`,+`score?`)**——**biz 回查 PG 装配完整 citation**(§8.2 Java 收口,audit-ai 热路径不依赖 PG)。`confidence/ai_label/route_type` 是**响应级**(在 meta/done),非 per-citation(纠 v0.4 §8.1 散文之松)。**`score`(v1.1.0 加法)** = per-hit 检索融合分(归一 0–1),供 biz 装四-Tab「匹配度」;biz 不检索无法自算,可空降级(BOUNDARY-RECONCILIATION-001 §4)。
  - `done`(响应级尾):`finish_reason`(stop/**refused**=覆盖拒答/length/error)+ `confidence`(float)+ `exhausted_scope`(string[],拒答时填)+ `token_usage?`。
  - `error`:`code`+`message`。

### 3.2 占位端点(签名+职责锁定,DTO 待各自 SPEC)

`POST /v1/compare`(异步比对,提交→任务引用,进度走 biz `async_tasks` §8.4)· `POST /v1/ocr`(发票图像→文本)·
`POST /v1/ingest-batch`(离线 S0–S5 语料生产)。v1 返回 `501`,不展开 DTO。

### 3.3 错误码(沿用 §8.3,本契约相关段)

- 流前错误:HTTP status + `ApiError{ error:{code,message,request_id} }`。流中错误:SSE `error` 事件后关闭。
- 段位:`E1xx–E8xx`(摄取侧,复用管线 §11.2)+ `B1xx`(鉴权/权限)/`B2xx`(业务校验)/`B3xx`(外部源)/`B4xx`(任务编排)。
- **本契约新增** `B104 内部令牌无效`(边界二服务认证失败)——§8.3 原 B1xx 是用户向,B104 是服务向;**已回灌 v0.4 §8.3 B1xx 段**(T0.1 完成)。
- **本契约新增** `B105 查询热路径内部错误`(`/v1/query` SSE `error` 事件:检索/嵌入/生成阶段服务向失败)——同 B104 属 B1xx 服务向段;不泄内部细节(堆栈进日志/trace)。audit-ai `routes_boundary` 已发此码,待回灌 v0.4 §8.3。

### 3.4 版本化(One-Version Rule)

URL 前缀 `/v1/`;**只增不改**(加可选字段不破契约),破坏性变更 → `/v2`。任何时刻线上只存一个版本。

## 4. Project Structure(产物落点)

```
audit-biz/docs/audit-biz-docs/
├── SPEC-BOUNDARY.md              本文件(散文 SPEC,主本)
└── openapi/
    └── boundary.v1.yaml          规范单一源(OpenAPI 3.0)
```
audit-ai 侧留指针引用本主本(主本在 biz,Java 持边界契约;待回填)。

## 5. Commands(校验)

```
# 校验 OpenAPI 合法性(任选其一,Track 落地时钉死工具)
npx @redocly/cli lint docs/audit-biz-docs/openapi/boundary.v1.yaml
# 或 Python: python -m openapi_spec_validator docs/audit-biz-docs/openapi/boundary.v1.yaml
```
两侧消费:audit-biz 由 YAML 生成/校验边界客户端 DTO;audit-ai 由 YAML 生成/校验 FastAPI 请求/响应模型(pydantic)。

## 6. Boundaries(三档)

- **Always**:契约改动先改 `boundary.v1.yaml`(单一源)再改两侧码;过滤值一律 Java 预计算下传;owner 不进制度语料检索;`request_id` 贯穿。
- **Ask first**:任何破坏性契约变更(改/删既有字段、改 SSE 事件语义)、新增端点正式 DTO、共享密钥机制升级(→HMAC/mTLS)、route_type 枚举定稿。
- **Never**:在边界传用户身份/JWT;让 audit-ai 做权限判断或回查 PG 装 citation(违 §8.2);共享密钥入库或硬编码;检索后过滤(必须前置)。

## 7. Success Criteria(具体可测)

1. `boundary.v1.yaml` 通过 OpenAPI lint(§5 命令零报错)。
2. `/v1/query` 的请求 schema、SSE 五类事件 payload、错误模型、共享密钥 securityScheme 均在 YAML 中可被两侧 codegen 引用。
3. 契约满足全部红线:无身份、过滤值预计算下传、引用装配 Java 收口、owner 语料隔离、前置过滤。
4. 据本契约,audit-biz 可写出 stub 客户端、audit-ai 可写出 stub 端点,**步行切片(前端→biz→/v1/query→SSE)能用 stub 端到端跑通**。
5. `/compare` `/ocr` `/ingest-batch` 占位签名 + 职责在案,不阻塞 v1。

## 8. Open Questions

**已解(T0.1,读 audit-ai `contract.py`/`observe.py`)**:
- ✅ #1 `route_type` 8 值 = `evidence/change/case/enumerate/judgmental/statistical/clarify/refuse`;**用显式 `review_required` bool**(非靠 route_type 推断)。
- ✅ #2 coverage = `exhausted_scope: string[]`(拒答时填)+ route_type=`refuse` + `done.finish_reason=refused`。
- ✅ #3 `B104` 已回灌 v0.4 §8.3 B1xx 段。
- ✅ #6 trace:`request_id` 由 Track B 注入 Langfuse trace(`observe.py` 现按 name 内部建、未接外部 id)。

**剩余(实现/联调期)**:
4. **`answer_blocks` 粒度**:`block_seq`/`block_type` 切块口径与前端渲染对齐——待 Track A 前端契约联调。
5. **限流/超时**:`/v1/query` 超时与背压(SSE 长连)阈值——⚠ 待实测标定(§6.2 Resilience4j/bucket4j 在 biz 侧)。

---

## 附录 A:字段对照表(audit-ai §10 `contract.py` ↔ 边界 v1 ↔ biz→前端)

> T0.1 产物 / Checkpoint A 冻结对象。**边界只回轻量;回查字段由 biz 装配**(§8.2)。

| §10 `contract.py`(audit-ai 产出) | 边界 v1(audit-ai→biz)| biz→前端(回查/装配后)|
|---|---|---|
| `QueryResult.route_type`(StrEnum 8) | `meta.route_type`(enum 8) | 透传 + 渲染分支 |
| `QueryResult.ai_label`(bool) | `meta.ai_label`(bool) | 透传(AI 标识) |
| `QueryResult.review_required`(bool) | `meta.review_required`(bool) | judgmental→人工复核框 |
| `QueryResult.export_enabled`(bool) | `meta.export_enabled`(bool) | 控制导出按钮 |
| `AnswerBlock.{type,content,stream}` | `delta.{block_type,text,block_seq}` | 拼 `answer_blocks[]` |
| `QueryResult.confidence`(float) | `done.confidence` | 透传 |
| `QueryResult.exhausted_scope`(list) | `done.exhausted_scope` | 拒答时展示已穷尽范围 |
| `Citation.clause_id` | `citation.clause_id`(+`chunk_id?`)| **回查主键** |
| 检索融合分(retrieve 层,per-hit) | `citation.score?`(v1.1.0 加法) | 四-Tab「匹配度」直显(0–100%) |
| `Citation.{doc_title,doc_no,clause_path,page_start,page_end,version,status}` | **边界不回**(audit-ai 跳回查) | **biz 按 clause_id 回查 PG 填**→ `citations[]` |
| —(错误) | `error.{code,message}` / `ApiError` | 错误码体系(含 `B104`) |

**两处对齐纠偏(记录在案)**:
1. v0.4 §8.1 散文写"引用标识含 confidence/ai_label/route_type",实为 `contract.py` **响应级**字段(在 meta/done),非 per-citation → 边界按 `contract.py` 实装。
2. audit-ai 现 `Citation` 自回查 PG 填满(MVP=后端)→ 边界要 audit-ai **加轻量模式**(只回 clause_id),回查移 biz(Task A4 ‖ B3,见 PLAN 风险表)。

> ✅ **Checkpoint A 已冻结(2026-07-01)**:字段对照表锁定,`boundary.v1.yaml` 版本转正 **v1.0.0**。
> A 轨(biz)/ B 轨(audit-ai)据此并行;后续任何字段/语义改动走**破坏性变更流程**(§6 Ask first,或 `/v2`)。
