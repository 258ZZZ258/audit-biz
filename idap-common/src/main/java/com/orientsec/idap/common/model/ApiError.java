package com.orientsec.idap.common.model;

/**
 * 统一错误体 {@code {"error":{"code","message","request_id"}}}(v0.4 §8.3 / 前端契约)。
 *
 * <p>字段 snake_case 由全局 Jackson SNAKE_CASE 策略产出(application.yml)。
 */
public class ApiError {

    private final Body error;

    public ApiError(Body error) {
        this.error = error;
    }

    public Body getError() {
        return error;
    }

    public static ApiError of(String code, String message) {
        return new ApiError(new Body(code, message, null));
    }

    public static class Body {
        private final String code;
        private final String message;
        private final String requestId;

        public Body(String code, String message, String requestId) {
            this.code = code;
            this.message = message;
            this.requestId = requestId;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getRequestId() {
            return requestId;
        }
    }
}
