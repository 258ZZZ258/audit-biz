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

## 本地后端栈(Docker)

现阶段仅 **PostgreSQL**(独立端口 **5544**,不撞 audit-ai 栈 5432/5433)。首次起库自动建表 + seed(语料回查表 + `casbin_rule`)。

```bash
docker compose up -d      # 起 PG(首次自动 docker/initdb/*.sql:建表 + seed)
docker compose down       # 停(留数据卷)
docker compose down -v    # 停 + 清库
```

应用连库(A4 引用回查用;连接串/密钥经 **env** 注入,绝不入库):

```bash
DB_URL=jdbc:postgresql://localhost:5544/audit_biz \
SPRING_DATASOURCE_USERNAME=audit_biz \
SPRING_DATASOURCE_PASSWORD=audit_biz \
java -jar target/audit-biz-0.0.1-SNAPSHOT.jar
```

> **注**:`chunks/doc_versions/cases` 是本地 dev 回查用的**最小 stand-in**(audit-ai Alembic 管权威语料表,列更全);
> 真集成(I1)连**共享 PG**。`casbin_rule` 是 biz 自有(A2 现用文件策略,切 PG 策略时用)。

## 状态

步行骨架(Track A,对 stub)建设中:A0 脚手架 · A1 SSO 验令牌 · A2 jCasbin 授权 · A3 /query SSE · A4 引用回查装配(见 `docs/devlog.md`)。
剩 I1(接真 audit-ai)。SDD 产物在 `docs/audit-biz-docs/`。
