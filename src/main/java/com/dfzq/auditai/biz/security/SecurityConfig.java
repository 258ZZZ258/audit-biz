package com.dfzq.auditai.biz.security;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 最小化 Spring Security resource-server：只做「验令牌 + 解析当前用户」（§7）。授权归 jCasbin（A2），此处不做方法级安全。
 *
 * <p><b>TODO-AUTH-001 收口</b>：受保护端点<b>强制认证</b>（{@code anyRequest().authenticated()}）， {@code
 * permitAll} 仅限显式公开白名单（健康检查 + SSO 回调）——不采用 v0.4 §7 草案「一律 permitAll」。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 公开端点白名单：仅健康检查 + SSO 回调。其余一律需认证。 */
    static final String[] PUBLIC_ENDPOINTS = {"/health", "/sso/callback"};

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // 无状态 API，不用 CSRF
                .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(PUBLIC_ENDPOINTS)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
