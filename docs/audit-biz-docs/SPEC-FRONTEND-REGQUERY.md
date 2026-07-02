# SPEC: audit-biz → 前端(genesis-ui)制度查询契约

> 设计法:`api-and-interface-design`(contract-first / 一致错误语义 / 边界校验 / 加字段不改字段 / 一致命名 / 分页)。
> 规范单一源 = `openapi/frontend.regquery.v1.yaml`。**依据产品原型 V3**:`/Users/apple/东方/制度查询/微信图片_20260629150748_221_1.png`(主页)、`_150816_223_1.png`(历史会话)。
> 上游:v0.4 §8.1(biz 回查装配对前端契约)/ RAG §5.9。**实现 SPEC-BOUNDARY 的 OQ#4**(answer_blocks↔前端渲染)。

## 1. Objective

定义 **audit-biz → 前端** 的制度查询页 REST/SSE 契约。本版(v1)只锁**核心对话+结果链**(原型主聊天 + 右侧上下文 + 结果区);
会话/留痕/导出/上传 占位(签名 + 原型字段清单在案)。

**这是与边界契约不同的第二张接口**:
```
前端 ──(本契约: SSO 有身份)──► audit-biz ──(boundary.v1: 无身份)──► audit-ai
        ◄── 富结构(分类/case卡/计数) ──        ◄── 轻量(route_type+answer_blocks+clause_id) ──
```
**装配链**:audit-ai 回轻量 → **biz 回查 PG(`chunks`/`doc_versions` 填条款引用 + `cases` 表填案例)+ 按 corpus/类型分组 + 计数** → 前端富结构。

## 2. Transport / Auth / 选型

- **鉴权**:甲方 SSO 标准协议令牌(`Authorization: Bearer <jwt>`,边界一,**有身份**)——与边界二 `X-Internal-Token` 完全不同。
- **流式**:`POST /api/v1/regulation/queries` 返回 SSE(text/event-stream)。
- **命名 snake_case**(对齐 §10 `contract.py` 与边界契约;biz 装配零字段映射)。**路径复数名词、无动词**;版本前缀 `/api/v1`。
- **规范** OpenAPI 3.0(`frontend.regquery.v1.yaml`),前端 codegen TS 类型 + biz codegen DTO。

## 3. 原型 → API 映射(两图)

| 原型 UI(图) | API |
|---|---|
| 主聊天发问 + 右侧"本轮对话上下文"面板(图1) | `POST /queries` SSE → `context` 事件(query_id/route_type/hit_skill/knowledge_scope/role/permission_scope/short_term_memory/review) |
| 系统摘要流式 / 案例启示摘要卡(图1) | `delta` 事件(block_type=text/case_card…) |
| 结果 Tab 命中制度(3)/条款(8)/监管规则(2)/相关案例(4)+ 推荐追问 + 耗时(图1) | `result` 事件(counts + regulations/clauses/rules/cases + case_insights + suggested_followups + elapsed_ms) |
| 相关案例表(案例名称/监管机构/处罚日期/违规主题/关联制度/核心问题/启示要点)(图1) | `CaseHit`(PG `cases` 表回查) |
| 查看原文 / 复制条款链接(图1) | `GET /clauses/{clause_id}` → `ClauseDetail`(full_text + 锚点 + deep_link) |
| 查看详情(案例)(图1) | `GET /cases/{case_id}` → `CaseDetail` |
| 历史会话:列表(共 68 条分页 + 搜索 + 状态筛选)+ 详情(图2) | **占位** `GET /sessions`、`GET /sessions/{id}`(字段见 §6) |
| 操作与留痕表 / 查看权限轨迹(图1) | **占位** `GET /queries/{id}/audit-trail`、`/permission-trail` |
| 导出查询报告(图1) | **占位** `POST /queries/{id}/export` → 异步任务 + `GET /tasks/{id}` |
| 上传文件 PDF/Word/Excel ≤50MB(图1) | **占位** `POST /files`(multipart)→ file_id |

## 4. 核心契约 `POST /api/v1/regulation/queries`(SSE)

请求 `QuerySubmit`:`question`(必)+ `session_id?`(null=新会话)+ `attachment_file_ids[]?` + `options{include_superseded?}`。
事件序 `context` →(`delta` 多个)→ `result` → `done`;任意时刻 `error`:
- **`context`**(右侧面板):query_id/session_id/current_question/route_type/hit_skill/knowledge_scope[]/role/permission_scope/short_term_memory?/review{required,status}。
- **`delta`**:block_seq + block_type(text|table|case_card|clarify_question)+ content(系统摘要增量;case_card=案例启示卡)。
- **`result`**(末尾装配富结构):elapsed_ms · counts{regulations,clauses,rules,cases} · regulations[]/clauses[](完整四级引用)/rules[] **各带 `match_score`(匹配度,源自边界 `citation.score`)** · cases[](cases 表)· case_insights[] · citation_advice[] · regulatory_digest[] · suggested_followups[] · confidence/ai_label/review_required/export_enabled。
- **`done`**:finish_reason(stop|refused|length|error)+ exhausted_scope[](拒答时)。
- **`error`**:`{ error:{code,message,request_id} }`。

> **来源分档(对齐 audit-ai SPEC-API §12)**:`match_score` 源自边界 `citation.score`(v1.1.0);发布/生效日期、发文机关、文号、条款节选 源自 `doc_versions`/`chunks`(本地 stand-in 列少→null,I1 连共享 PG 填);`theme`/`core_requirement`/`citation_advice`/`regulatory_digest`/案例 `core_issue`/`insight` 为 LLM 提炼或 L2 富集,**默认关 → 空/null,前端降级隐藏,零臆造**(红线)。

## 5. 明细端点(核心)

- `GET /clauses/{clause_id}` → `ClauseDetail`:条款原文 + 四级锚点(doc/clause_path/page/version/status)+ `deep_link`(复制条款链接)。
- `GET /cases/{case_id}` → `CaseDetail`:`CaseHit` + full_text + source_url。

## 6. 占位端点 + 原型字段清单(v1 不展开 DTO,留 SPEC-SESSIONS 等)

- **`GET /sessions`**(图2):分页(`PaginatedMeta`:page/page_size/total_items/total_pages)+ `q`(搜索标题)+ `status` 筛选;
  `SessionSummary{session_id,title,agent_type,role,created_at,status}`。
- **`GET /sessions/{id}`**(图2):+ user_question、system_summary(系统摘要)、counts(计数卡)、query_ids[]。
- **`POST /sessions`**(新会话)· **`DELETE /sessions/{id}`**(清空会话)· **`POST /sessions/{id}/reopen`**(重新打开)。
- **`GET /queries/{id}/audit-trail`**(图1 操作与留痕):`{time,actor,dept,action,action_point,status}[]`。
- **`GET /queries/{id}/permission-trail`**(查看权限轨迹)。
- **`POST /queries/{id}/export`** → 异步任务(POI 渲染,§6.2)+ `GET /tasks/{id}` 轮询。
- **`POST /files`**(multipart,PDF/Word/Excel ≤50MB)→ `{file_id}`。

## 7. 错误 / 分页 / 版本 / 一致性(api-and-interface-design)

- **统一错误体** `{ error:{ code, message, request_id? } }`,HTTP status 映射:401 未认证(B101)/403 越权(B102)/404 不存在/422 校验失败/500 服务端。
- **分页** list 端点用 `PaginatedMeta` + `data[]`;query param camel? 否——**统一 snake**(`page`/`page_size`/`q`/`status`)。
- **版本** `/api/v1`;**只增不改**(加可选字段不破前端),破坏性 → `/v2`(One-Version Rule)。
- **边界校验**:`QuerySubmit` 在 biz 入口校验;**audit-ai 回的数据视为不可信**,装配前校验形态(api-and-interface-design §3)。

## 8. Boundaries(三档)

- **Always**:契约改先改 `frontend.regquery.v1.yaml` 再改两侧;snake_case;加字段而非改/删;result 富结构由 biz 回查装配(不让前端直连 audit-ai/PG)。
- **Ask first**:破坏性变更、占位端点转正式 DTO、SSE 事件语义变更、把某结果块从 result 改为单独 GET。
- **Never**:前端契约泄露 audit-ai/Milvus/内部令牌等实现细节(Hyrum 法则);用动词 URL;不同端点返回不一致错误体;list 端点无分页。

## 9. Success Criteria

1. `frontend.regquery.v1.yaml` 过 OpenAPI lint。
2. 每端点有 typed 请求/响应 schema;错误体统一;list 端点带分页。
3. `result` 富结构字段与原型两图逐项可对(§3 映射表无缺)。
4. 字段 100% snake_case;`/api/v1` 版本前缀;新字段皆可选(向后兼容)。
5. 装配链可走通:audit-ai 轻量 → biz 回查 → 本契约富结构(与 SPEC-BOUNDARY 字段对照表一致)。

## 10. Open Questions

- FE-OQ1 `route_type` → `hit_skill` 显示名映射表(8 路各显示什么 Skill 名)——待产品/前端确认。
- FE-OQ2 `result` 是否需把 regulations/clauses/rules/cases 做**分页**(原型计数小 3/8/2/4,暂全量内嵌;大结果再议)。
- FE-OQ3 多轮澄清(R7 clarify / 继续追问)的 session 续接语义——与 SPEC-SESSIONS 一并定。
- FE-OQ4 `deep_link`(复制条款链接)格式与跳转目标(站内锚点 vs 原文 PDF 页)——待前端联调。

> 本设计是 Track A 前端契约的 SPEC;落地任务并入 PLAN-BOUNDARY 的 Phase A(A3 产 SSE 装配、A4 回查填 clauses/cases)。
