package com.dfzq.auditai.biz.authz;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.util.Util;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * jCasbin 授权唯一 enforcer(六类权限点,v0.4 §7)。**不用 Spring Security 授权层**。
 *
 * <p>A2 用文件策略(classpath {@code casbin/model.conf} + {@code policy.csv});生产切 PG {@code casbin_rule}
 * JDBC adapter(接 PG 那步)。请求级「越权 → B102」的串接在 A3(有 /query 端点后)。
 *
 * <p>经 {@code getInputStream()} 拷临时文件后再交 jCasbin ——**jar-safe**(getFile() 在 repackaged jar 内会
 * FileNotFoundException,AUTHZ-BOOT-001)。
 */
@Component
public class Authorizer {

    static {
        // 全局关 jCasbin 日志(默认 INFO 打印完整 Policy/Role links + 每次 enforce 的 sub/obj/act)——
        // 生产切 PG casbin_rule 后会泄露真实用户-角色绑定与授权决策明细(AUTHZ-LOG-001)。
        // 置于静态块:类加载即生效,先于构造期的 Policy 转储。业务审计另由 Java 侧结构化留痕。
        Util.enableLog = false;
    }

    private final Enforcer enforcer;

    public Authorizer() {
        this.enforcer =
                new Enforcer(
                        materialize("casbin/model.conf", "casbin-model", ".conf"),
                        materialize("casbin/policy.csv", "casbin-policy", ".csv"));
    }

    /** 把 classpath 资源(可能内嵌于 jar)拷到临时文件,返回可被 jCasbin 读取的绝对路径。 */
    private static String materialize(String classpath, String prefix, String suffix) {
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            File tmp = File.createTempFile(prefix, suffix);
            tmp.deleteOnExit();
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tmp.getAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException("加载 Casbin 资源失败: " + classpath, e);
        }
    }

    /** 主体 subject 对权限点 object 执行 action 是否被允许。 */
    public boolean permits(String subject, String object, String action) {
        return enforcer.enforce(subject, object, action);
    }
}
