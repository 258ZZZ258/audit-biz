package com.dfzq.auditai.biz.authz;

import java.io.IOException;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * jCasbin 授权唯一 enforcer(六类权限点,v0.4 §7)。**不用 Spring Security 授权层**。
 *
 * <p>A2 用文件策略(classpath {@code casbin/model.conf} + {@code policy.csv});生产切 PG {@code casbin_rule}
 * JDBC adapter(接 PG 那步)。请求级「越权 → B102」的串接在 A3(有 /query 端点后)。
 */
@Component
public class Authorizer {

    private final Enforcer enforcer;

    public Authorizer() {
        try {
            // dev/test 从 classpath(target/classes)取文件路径;生产走 PG adapter,不依赖此路径。
            String modelPath =
                    new ClassPathResource("casbin/model.conf").getFile().getAbsolutePath();
            String policyPath =
                    new ClassPathResource("casbin/policy.csv").getFile().getAbsolutePath();
            this.enforcer = new Enforcer(modelPath, policyPath);
        } catch (IOException e) {
            throw new IllegalStateException("加载 Casbin 模型/策略失败", e);
        }
    }

    /** 主体 subject 对权限点 object 执行 action 是否被允许。 */
    public boolean permits(String subject, String object, String action) {
        return enforcer.enforce(subject, object, action);
    }
}
