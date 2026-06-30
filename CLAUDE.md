# CLAUDE.md — audit-biz

审计大模型系统一期 **Java 后端**(audit-biz):**对前端的唯一入口 + 全部有状态的东西**(数据 / 权限 / 任务 /
对前端契约)。无状态的 AI 推理与文档解析在**姊妹仓 `audit-ai`(Python/FastAPI)**——两套语言是甲方硬约束、
省不掉;本仓与 audit-ai 之间是**单向、无身份**调用。整体后端设计见 `docs/审计大模型系统_后端总体技术框架设计_v0_4.md`
(本仓持有的**边界契约主本**);AI/RAG/管线内部细节在 audit-ai 仓的 v1.6/v1.5 设计,本仓**只引用接口契约,不重复**。

> **本文件只放"始终要遵守的核心"——边界契约、栈、约定。** 模块级开发记忆 / 决策 / 踩坑写到 `docs/devlogs/*`,
> `docs/devlog.md` 为时间轴索引。**这是 greenfield 仓库:很多约定尚未落地,标 `【待定】` 的别擅自锁,按 SDD 评审。**

## 三层记忆分工(写记录前先问"归哪层")

- **git** = WHAT/WHEN/WHO + commit 粒度 why(机械完整,**能从 diff/log 看出来的只归这里**)。
- **in-repo devlog**(`docs/devlog.md` 索引 + `docs/devlogs/*`)= **git 给不了的**——决策 + 为什么(尤其**否决方案**)、
  跨改动状态综合、非显然踩坑 / 环境怪癖 / 契约约束(随代码入库,给团队 + agent)。
- **agent auto-memory**(`~/.claude/.../memory/`,私有)= 跨会话环境怪癖。**用户偏好(中文沟通、决策点用选择题)
  已在用户全局 `~/.claude/CLAUDE.md`,本仓不重复。**
- **判据**:能从 diff/log 直接看出来 → 谁都别写;会随代码演进、要给团队看 → devlog;只帮 agent 跨会话 → auto-memory。

## 开发协作流程(分工 — 始终遵守,同 audit-ai)

- **Claude Code(规划 + 实现)**:`spec-driven-development`(写规格)→ `planning-and-task-breakdown`(出计划/任务)→
  `incremental-implementation` + `test-driven-development`(逐任务 TDD)。每阶段门控待人工批准再进。默认不自评。
- **Codex(代码审查)**:`code-review-and-quality` + `security-and-hardening`;发现写 `.review/findings.json` →
  Claude(原作者)逐条修复或带 `spec_ref` 反驳 → Codex 复审,直至无 critical/warning。修复归实现侧,审查者不自改。
- SDD 产物落 `docs/<模块>-docs/`。**多开会话用独立 git worktree**(隔离工作树,不在他人正用的树里切分支)。

## 架构定位(v0.4 / CP-011)

- **audit-biz 吃掉所有有状态的东西**:对外 API + 鉴权(SSO 标准协议验令牌)+ 授权(jCasbin 六类权限点)+
  业务 CRUD + 费用规则引擎(SQL)+ 报告/Excel/Word 渲染(POI/docx4j)+ 任务提交与状态(PG 状态机)+
  **引用四级回查装配**(v0.4 翻转:Java 独占 PG 与前端契约,由它回查填充)+ 操作/审计日志 + 外部数据源接入。
- **audit-ai 是无状态端点**:在线 `/retrieve /generate /compare /ocr`(无身份,按传入过滤值检索)、
  离线 `/ingest-batch`(语料生产者,写 PG 语料表 + Milvus)。它**只返回 chunk_id/clause_id**,不持前端契约。

## 硬契约 — 边界(audit-biz ↔ audit-ai)

- **单向、无身份、共享密钥 + 网络隔离**:Java 把**预计算好的权限过滤值**(`perm_tags` / 分区 / `project_id`)
  当**普通字段**下传;Python 据此构造 Milvus filter(满足"检索前置过滤"红线)。**不签发带身份的内部 JWT**。
- **Java 持前端契约 + PG 权威**;引用四级定位由 Java 回查 PG 装配(audit-ai 只回 ID)。
- SSE:Java **透传** audit-ai 的流。
- 跨文档协调点(权限点口径 / 统一任务状态表 / 引用回查归属)以**本仓 v0.4 单一主本**为准,**严防与 audit-ai 侧两份漂移**
  (见 v0.4 §15,建议以 CP 回灌 v1.5/v1.6)。

## 数据 / 中间件(复用管线设计,不改)

- **PostgreSQL** 权威事实库(制度/版本/条款/比对/案例/业务表族/权限/日志),**兼任务状态机(Phase 1 不用 Temporal)**。
- **Milvus 2.4** 检索投影(可从 PG 全量重建);**MinIO** 原件/导出/发票附件/模板;**Langfuse** 观测;**Redis【待定·可选】**
  (一期 ~100 并发可暂用 PG 替代,是否启用待甲方确认部署环境)。
- **写序一致性**(复用管线 §12.1):PG → Milvus upsert → flush → PG 置 INDEXED;不存在脏向量。业务表族纯 PG 事务、不投影
  (发票 OCR 文本除外,入 `corpus_type=expense_doc` 分区)。**所有 PG 表 add-only,带 `created_at/by, updated_at/by`,敏感操作另写审计日志表。**

## Java 栈(最小常规栈,交接友好 — v0.4 已锁)

- **Spring Boot MVC**(不用 WebFlux)+ **MyBatis-Plus**(费用规则是 SQL 重活,比 JPA 透明,信创 Java 圈事实标准)+
  **jCasbin**(授权,六类权限点)+ **最小化 Spring Security**(仅 resource-server 验令牌,不用其授权层)+ **Apache POI/docx4j**。
- 砍掉:Spring Cloud 微服务全家桶、WebFlux、独立 API 网关产品(BFF 就是这个 Spring Boot 应用本身)。
- **【待定】构建工具**:倾向 **Maven**(信创 Java 默认),vs Gradle —— 待评审锁定。**【待定】**包结构 / lint(spotless 或
  checkstyle)/ 测试框架(JUnit 5 + 倾向)/ 国产化 JDK 发行版 —— 均待 SDD 评审,勿擅自锁。

## 一期红线(与既有文档完全一致,不放宽)

不回写任何源系统;数据流严格**单向只读**;知识库/制度正文**不自动修改**;**不建设审批/复核流转中心**。

## 姊妹仓 audit-ai(定位与跨仓参考)

audit-biz **不含 Python/AI 代码**;需查 audit-ai 的接口契约 / 数据 schema / 内部实现 / 设计时,按以下定位:

- **本机路径**:`../audit-ai`(约定:两仓在 `~/Projects/` 下做姊妹目录)。**不存在则克隆到同级**:
  `git clone ssh://git@ssh.github.com:443/258ZZZ258/audit-ai.git ../audit-ai`
- **权威远端**(机器无关,CI / 他人机用这个):`ssh://git@ssh.github.com:443/258ZZZ258/audit-ai.git`

**audit-biz agent 最常需要跨仓参考的 audit-ai 文件**(均在 `../audit-ai/` 下,先读对应 devlog 再改):

| 用途 | audit-ai 路径 |
|---|---|
| `audit_corpus` 检索投影 schema(Java 下传过滤值要对齐字段名/类型) | `libs/common/common/milvus_schema.py` |
| PG 字段/枚举(权威语料表;Java 业务表 add-only 不与之冲突) | `libs/common/common/pg_models.py` |
| chunk_id 公式(引用四级回查装配按它对齐) | `libs/common/common/chunk_id.py` |
| 导入契约 / IR schema | `libs/common/common/manifest.py` · `ir.py` |
| RAG 查询 / 解析管线实现(Java 调的 /retrieve /generate /compare /ingest-batch 内部) | `query/query/` · `pipeline/pipeline/` |
| AI 内部权威设计 | `docs/制度查询与制度比对智能体_RAG技术框架设计_v1.5.md` · `docs/文档处理与语料库构建_技术框架设计_v1.6.md` |
| 模块开发记忆(决策/踩坑,lazy 按需读) | `docs/devlogs/*` · `docs/query-agent-docs/query_devlog.md` |

> ⚠ audit-ai 当前形态是 CLI demo(`demo`/`demo-web`);v0.4 §8 描述的对 audit-biz 的 HTTP 服务端点
> (/retrieve /generate /compare /ocr /ingest-batch)**可能尚未全部落地**——**契约以本仓 v0.4 §8 为准**,
> audit-ai 侧 HTTP 层建成后在此回填实际端点/DTO。

## 设计文档指针

- 边界总览(本仓主本):`docs/审计大模型系统_后端总体技术框架设计_v0_4.md`(经张翼飞复核后方可作汇报底稿)。
- AI 内部(姊妹仓):`../audit-ai/docs/制度查询与制度比对智能体_RAG技术框架设计_v1.5.md` · `../audit-ai/docs/文档处理与语料库构建_技术框架设计_v1.6.md`。
