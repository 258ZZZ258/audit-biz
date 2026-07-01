package com.dfzq.auditai.biz.web.error;

import com.dfzq.auditai.biz.authz.FilterValidationException;
import com.dfzq.auditai.biz.authz.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
