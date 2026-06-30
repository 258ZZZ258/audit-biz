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
  - `meta`:`request_id` + `route_type`(八路路由结果;judgmental→biz 渲染"AI 辅助判断,人工复核")+ `coverage?`。
  - `delta`:答案增量 `text` + `block_seq`(biz 据此拼 `answer_blocks[]`)。
  - `citation`:**轻量标识** `chunk_id`(+`clause_id?`/`confidence`/`ai_label?`)——**biz 回查 PG 装配完整 citation**(§8.2 Java 收口,audit-ai 热路径不依赖 PG)。
  - `done`:`finish_reason`(stop/**refused**=覆盖感知拒答,非错误/length/error)+ `token_usage?`。
  - `error`:`code`+`message`。

### 3.2 占位端点(签名+职责锁定,DTO 待各自 SPEC)

`POST /v1/compare`(异步比对,提交→任务引用,进度走 biz `async_tasks` §8.4)· `POST /v1/ocr`(发票图像→文本)·
`POST /v1/ingest-batch`(离线 S0–S5 语料生产)。v1 返回 `501`,不展开 DTO。

### 3.3 错误码(沿用 §8.3,本契约相关段)

- 流前错误:HTTP status + `ApiError{ error:{code,message,request_id} }`。流中错误:SSE `error` 事件后关闭。
- 段位:`E1xx–E8xx`(摄取侧,复用管线 §11.2)+ `B1xx`(鉴权/权限)/`B2xx`(业务校验)/`B3xx`(外部源)/`B4xx`(任务编排)。
- **本契约新增** `B104 内部令牌无效`(边界二服务认证失败)——§8.3 原 B1xx 是用户向,B104 是服务向;**建议 CP 回灌 v0.4 §8.3**(见 Open Questions)。

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

## 8. Open Questions(待人工/甲方输入,锁定后回填契约)

1. **`route_type` 完整枚举**:八路路由的 8 个取值与 biz 渲染映射(尤其哪些算 judgmental 要"人工复核"框)——待 RAG 设计路由章节确认。
2. **`coverage` 结构**:覆盖感知拒答回传给 biz 的结构(是否需要给前端展示"未覆盖"提示)——待 RAG §覆盖判定确认。
3. **`B104` 回灌**:内部令牌失败码是否正式并入 v0.4 §8.3(建议并入,以保错误码单一权威)。
4. **`answer_blocks` 粒度**:audit-ai 按 `block_seq` 切块的口径(段/句/路由步)是否需与前端渲染对齐——待 Track A 前端契约联调。
5. **限流/超时**:`/v1/query` 的超时与背压(SSE 长连)阈值——⚠ 待实测标定(§6.2 Resilience4j/bucket4j 在 biz 侧)。
6. **观测对齐**:`request_id` 是否直接复用 audit-ai 现有 Langfuse trace id 约定——待查 query observe 实现。

> 审批后进 **Phase 2 PLAN**(用 `planning-and-task-breakdown` 出 Track0 收尾 + Track A 切片的实现计划)。
