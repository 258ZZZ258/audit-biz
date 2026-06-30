package com.dfzq.auditai.biz.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查端点。
 *
 * <p>A1 引入 Spring Security resource-server 后，{@code /health} 列入 permitAll 白名单
 * （仅 health + SSO 回调显式公开，受保护端点强制认证——TODO-AUTH-001）。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
