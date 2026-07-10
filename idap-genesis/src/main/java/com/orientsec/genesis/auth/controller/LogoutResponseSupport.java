package com.orientsec.genesis.auth.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;

/** Shared logout response handling that remains testable outside the intranet profile. */
public final class LogoutResponseSupport {

    private static final String GENESIS_SESSION_COOKIE = "gns_session";

    private LogoutResponseSupport() {}

    public static void clearCookiesAndRedirect(
            HttpServletResponse response, String tokenCookieName, String redirectTarget)
            throws IOException {
        if (!StringUtils.hasText(redirectTarget)) {
            throw new IllegalStateException("缺少 Genesis 登出跳转配置 ca.domainName");
        }
        boolean secure = isSecureHttpRedirect(redirectTarget);
        expireCookie(response, tokenCookieName, secure);
        expireCookie(response, GENESIS_SESSION_COOKIE, secure);
        response.sendRedirect(redirectTarget);
    }

    private static boolean isSecureHttpRedirect(String redirectTarget) {
        try {
            URI redirectUri = new URI(redirectTarget);
            String scheme = redirectUri.getScheme();
            if (redirectUri.getHost() == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalStateException("ca.domainName 必须是有效的 HTTP(S) 地址");
            }
            return "https".equalsIgnoreCase(scheme);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("ca.domainName 必须是有效的 HTTP(S) 地址", e);
        }
    }

    private static void expireCookie(
            HttpServletResponse response, String cookieName, boolean secure) {
        if (!StringUtils.hasText(cookieName)
                || !cookieName.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) {
            throw new IllegalArgumentException("非法认证 Cookie 名称");
        }
        StringBuilder header =
                new StringBuilder(cookieName)
                        .append("=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        if (secure) {
            header.append("; Secure");
        }
        response.addHeader("Set-Cookie", header.toString());
    }
}
