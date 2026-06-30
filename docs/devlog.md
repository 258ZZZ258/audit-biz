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

## 待办 / 未决(TODO)

- [ ] **TODO-AUTH-001 · v0.4 §7 `permitAll` 鉴权方案存在越权风险**(来源:Codex 审查 finding `SEC-AUTH-001`,
  原记于 `audit-ai/.review/findings.json`,审的是 v0.4 设计正文)。
  - **风险**:v0.4 §7 把 Spring Security 过滤链"一律 `permitAll`、只验令牌 + 解析当前用户"——resource-server 会
    **解析**令牌但**不强制**受保护 API 必须认证,使 audit-biz(唯一对外边界)可能在 jCasbin 拿到可信用户**之前**
    就收到**未认证请求**,绕过授权。锚点原文:`- 过滤链关掉表单登录/session/方法级安全,一律 permitAll;Spring
    Security **只留"验令牌 + 解析当前用户"这一件事**`(v0.4 §7 附近)。
  - **处理路径**(spec Java 鉴权层时正面处理,二选一):① 修订 v0.4 §7——受保护 API **强制认证**,`permitAll`
    只留健康检查 / SSO 回调等**显式公开端点**,再对已认证 principal 跑 Casbin;② 带契约/§ 理由反驳该 finding。
  - **已做**:固化为审查红线 `audit-biz-code-review.mdc` #3(permitAll 不得覆盖受保护端点)+ `AGENTS.md` 硬约束。
  - **何时收口**:Java 鉴权层 SDD / 实现前必须决议,**勿带病进实现**。
