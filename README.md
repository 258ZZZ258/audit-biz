# audit-biz

审计大模型系统一期的 **Java 后端服务**(Spring Boot)——对前端的唯一入口,承载全部有状态能力:
鉴权授权、业务数据 CRUD、费用规则引擎、报告/Excel/Word 渲染、任务状态机、引用回查装配、外部数据源接入、操作/审计日志。

无状态的 AI 推理与文档解析在姊妹仓 **audit-ai**(Python/FastAPI);两者之间为**单向、无身份**调用
(共享密钥 + 网络隔离,Java 下传预计算的权限过滤值)。

> 姊妹仓定位:本机 `../audit-ai`,远端 `ssh://git@ssh.github.com:443/258ZZZ258/audit-ai.git`。
> 跨仓参考哪些文件见 `CLAUDE.md`「姊妹仓 audit-ai」。

## 文档

- 后端总体设计(边界契约主本):`docs/审计大模型系统_后端总体技术框架设计_v0_4.md`
- 时间轴 / 模块开发记忆:`docs/devlog.md` + `docs/devlogs/*`
- 协作流程与栈约定:`CLAUDE.md`

## 状态

🌱 Greenfield —— 仓库骨架已建,Java 工程脚手架(构建工具 / 包结构 / Spring Boot 应用)尚未落地,按 SDD 评审后逐步建设。
