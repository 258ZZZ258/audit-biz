package com.dfzq.auditai.biz.web.error;

/**
 * 统一错误体 {@code {"error":{"code","message","request_id"}}}(v0.4 §8.3 / 前端契约)。
 *
 * <p>字段 snake_case 由全局 Jackson SNAKE_CASE 策略产出(application.yml)。
 */
public record ApiError(Body error) {

    public record Body(String code, String message, String requestId) {}

    public static ApiError of(String code, String message) {
        return new ApiError(new Body(code, message, null));
    }
}
