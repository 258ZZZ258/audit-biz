package com.dfzq.auditai.biz.web.error;

import com.dfzq.auditai.biz.authz.FilterValidationException;
import com.dfzq.auditai.biz.authz.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 统一错误体收口(§8.3):越权→B102、业务校验失败→B2xx。B101(未认证)由 RestAuthEntryPoint 出。 */
@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> forbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("B102", e.getMessage()));
    }

    @ExceptionHandler(FilterValidationException.class)
    public ResponseEntity<ApiError> filterInvalid(FilterValidationException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of("B201", e.getMessage()));
    }

    /**
     * @Valid 请求体校验失败(如 question 空)。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException e) {
        String msg =
                e.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                        .orElse("请求体校验失败");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of("B201", msg));
    }

    /** 请求体缺失 / JSON 无法解析。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of("B201", "请求体缺失或无法解析"));
    }
}
