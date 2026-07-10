package com.orientsec.idap.core.web.error;

import com.orientsec.idap.common.exception.NotFoundException;
import com.orientsec.idap.common.exception.NotImplementedException;
import com.orientsec.idap.common.model.ApiError;
import com.orientsec.idap.core.authz.FilterValidationException;
import com.orientsec.idap.core.authz.ForbiddenException;
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

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("B404", e.getMessage()));
    }

    @ExceptionHandler(NotImplementedException.class)
    public ResponseEntity<ApiError> notImplemented(NotImplementedException e) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiError.of("B501", e.getMessage()));
    }

    /** @Valid 请求体校验失败(如 question 空)。 */
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
