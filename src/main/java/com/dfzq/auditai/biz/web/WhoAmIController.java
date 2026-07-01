package com.dfzq.auditai.biz.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户（从 SSO 令牌解析）。受保护端点——未认证访问由 {@link com.dfzq.auditai.biz.security.SecurityConfig} 拦为 401。
 *
 * <p>仅暴露前端「当前角色」等所需的最小身份信息；完整权限范围由 jCasbin（A2）预计算。
 */
@RestController
public class WhoAmIController {

    @GetMapping("/api/v1/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user_id", jwt.getSubject());
        body.put("name", name != null ? name : jwt.getSubject());
        return body;
    }
}
