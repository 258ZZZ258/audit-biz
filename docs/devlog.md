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
