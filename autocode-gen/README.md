# IDAP 代码生成入口

`IdapAutoCodeApp` 固定按截图要求为 `idap_user_info` 生成代码，并把实际生成工作委托给甲方
`AutoCodeBase`。外网工程不复制甲方模板和生成器实现。

内网补齐生成器依赖后执行：

```bash
mvn compile exec:java \
  -Dexec.mainClass=com.orientsec.idap.autocode.run.IdapAutoCodeApp \
  -pl autocode-gen
```

默认查找 `com.orientsec.idap.autocode.AutoCodeBase`。如果内网实际类名不同，可通过
`-Didap.autocode.base-class=完整类名` 指定；目标类需要提供静态方法
`doStart(String, List<String>, String[])`。
