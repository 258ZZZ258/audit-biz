package com.dfzq.auditai.biz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * audit-biz 启动类。
 *
 * <p>包结构规划（随任务落地）：web/(控制器) · security/(SSO 验令牌, A1) · authz/(jCasbin + 过滤值预计算, A2) ·
 * client/(audit-ai 边界客户端, A3) · citation/(PG 回查装配, A4) · dto/ · config/。
 */
@SpringBootApplication
public class AuditBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditBizApplication.class, args);
    }
}
