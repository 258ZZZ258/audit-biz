# AGENTS.md

本文件指导 Codex(及其它编码代理)在 **audit-biz** 仓工作。**架构 / 边界契约 / 数据 / Java 栈 / 测试约定以
`CLAUDE.md` 为单一事实源**(本文件不重述,以免再次过时)——动代码前先读 `CLAUDE.md`,改边界契约前读
`docs/审计大模型系统_后端总体技术框架设计_v0_4.md`("裁机制不裁契约":cut mechanisms, never cut contracts)。

## 本仓是什么(一句话)

`audit-biz`:审计大模型系统一期的 **Java 后端**(Spring Boot MVC + MyBatis-Plus + jCasbin + 最小化
Spring Security + Apache POI)——**对前端唯一入口 + 全部有状态能力**(数据/权限/任务/对前端契约)。
无状态 AI 推理与文档解析在姊妹仓 `audit-ai`(Python,定位见 `CLAUDE.md`「姊妹仓 audit-ai」)。详见 `CLAUDE.md`。

## 开发协作分工(与 CLAUDE.md 一致)

- **Claude Code(规划 + 实现)**:需求规划、计划/任务分解、代码生成。skills `spec-driven-development` →
  `planning-and-task-breakdown` → `incremental-implementation` + `test-driven-development`,每阶段门控待人工批准。
- **Codex(代码审查)—— 本文件的主对象**:负责开发生命周期中的代码审查,skills `code-review-and-quality` +
  `security-and-hardening`。
- **审查修复闭环**:你(Codex)审查 → 发现写 `.review/findings.json`(按 `.cursor/rules/review-output.mdc`)→
  **由 Claude Code(原作者)逐条修复,或带契约/§ 理由反驳**(审查意见非总对)→ 你**复审**新 diff,直至无 critical/warning。
  **你只审不改:绝不自行修改实现代码**(修复归实现侧,保审查独立性——改动也须被独立验证);
  纯机械项(格式 / spotless / checkstyle)交工具,不劳代理。

## Codex 审查时的硬约束(务必校核,细节见 CLAUDE.md「硬契约 — 边界」+ v0.4)

- **边界单向无身份**:audit-biz↔audit-ai 仅共享密钥 + 网络隔离 + Java 传**预计算过滤值**(`perm_tags`/分区/`project_id`)
  当普通字段;**出现带身份内部 JWT、或让 audit-ai 认身份/做权限判断的代码,按高危标出**。Java 持前端契约 +
  引用四级回查装配,audit-ai 只回 `chunk_id`/`clause_id`;SSE 由 Java 透传。
- **鉴权授权**:受保护 API 必须经 SSO 验令牌(Spring Security resource-server)+ jCasbin 授权(六类权限点);
  **过滤链 `permitAll` 不得覆盖受保护端点**(否则未认证请求越过 Casbin)——仅健康检查/SSO 回调可 permitAll。
  行级 ABAC(owner 本人导入/生成者权限最大)不得绕过。
- **PG add-only**:列只增不删不改(名/类型/枚举);带 `created_at/by, updated_at/by`;敏感操作另写审计日志表。
- **一期红线**:不回写任何源系统、数据流单向只读、不改知识库/制度正文、不建审批/复核流转中心。
- **密钥**:DB 连接串、audit-ai 共享密钥、SSO/模型网关配置走 env/外部配置(`application-local*` 已 gitignore),明文入库标 🔴 并提醒轮换。
- **测试**:【待 Java 栈落地后固化】Maven/Gradle 构建 + JUnit5;SQL/迁移变更校核 add-only。

## 审查产出约定

代码审查完成后按 `.cursor/rules/review-output.mdc` 将全部发现写入 `.review/findings.json`(供 Review Lens 消费):
`version=1`、`findings[]` 含 file/start_line/end_line/severity/rule_id/spec_ref/message/anchor_text;
`anchor_text` 必须是目标文件逐字原文,写入前重读核对行号。
