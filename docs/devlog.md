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

## 2026-06-30 · 后端起步路线(契约先行 + biz 步行骨架优先)

- **背景核实**:audit-ai `demo-web` 是 stdlib-HTTP 内部工作台(**非 FastAPI**),query 无 HTTP 层,**全仓无 FastAPI**
  → v0.4 §8 那套 biz 要调的服务端点(/retrieve /generate /compare /ocr /ingest-batch)**尚未落地**。
- **决策(用户拍板)**:
  - ① 真正的第一步是**锁边界契约**(v0.4 §8 → SPEC):阻塞两轨、零代码最便宜,锁完两轨可并行(一侧拿另一侧 stub 顶)。
  - ② 实现从 **biz 锚定的「步行骨架」垂直切片**起 —— 制度查询一条线:前端 → biz(SSO 验令牌 + jCasbin + 预计算
    `perm_tags`)→ 单向无身份调 audit-ai `/retrieve`(**先 stub,后真**)→ SSE 透传。最早暴露 Java/鉴权最高风险 + 验证整套架构。
  - ③ **「audit-ai web 升级」= 用 FastAPI 暴露 §8 端点**(包裹已有 query/pipeline),是边界的 audit-ai 半边、**次轨**;
    先做 `/retrieve`+`/generate` 喂切片,替换 biz 侧 stub。
- **否决**:「先把 audit-ai 端点全做完再开 biz」(Java/鉴权风险后置)、「先把 biz 骨架全搭完再集成」(晚暴露集成风险)
  —— 都不如最薄垂直切片早验架构。
- **轨道**:Track0 边界契约 SPEC(落 `docs/audit-biz-docs/`,audit-ai 引用)→ Track A biz 切片(脚手架→SSO→jCasbin→查询端点)
  ‖ Track B audit-ai FastAPI 端点。每轨走 SDD(spec→plan→tasks→TDD)+ Codex 审查闭环。
- **关联**:切片的 SSO/permitAll 正是 TODO-AUTH-001 的收口点。

## 2026-07-01 · A0 Spring Boot 脚手架(Phase 4 IMPLEMENT)

- **落地**:Maven + Spring Boot 3.3.5 + JDK 17,根包 `com.dfzq.auditai.biz`;`AuditBizApplication` + `/health`(web)
  + Maven Wrapper(`mvnw`,自举只需 JDK)。TDD:`HealthControllerTest`(/health→200 `{status:UP}`)+
  `AuditBizApplicationTests`(contextLoads)→ `mvn test` **2 passed / BUILD SUCCESS**(JDK 17.0.19 真验证)。
- **对 TASKS A0 措辞的修正(决策)**:TASKS 原写"pom 提前声明 MyBatis-Plus/oauth2-resource-server/jCasbin/POI";
  实测**这些 starter 无 DataSource/issuer 配置会让 A0 启动失败**,违反"每步留可运行态"。→ A0 只放 web+test 最小可启动集,
  DB/安全/Casbin/POI **各随 A1/A2/后续任务引入**(届时同步配 issuer/datasource)。
- **环境怪癖(team / CI 须知)**:需 **JDK 17**;mac `brew install openjdk@17` 是 **keg-only** →
  构建前 `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`(否则 `mvn`/`java` 找不到);Maven 用 `brew install maven` 或仓内 `./mvnw`。

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
