package com.dfzq.auditai.biz.authz;

/**
 * 过滤值预计算校验失败(如检索范围含 {@code audit_project} 但缺 {@code project_id})。
 *
 * <p>A3 请求层映射为 B2xx(业务校验)。fail-closed:宁拒不放,防跨项目召回(§4.5/§7.x)。
 */
public class FilterValidationException extends RuntimeException {

    public FilterValidationException(String message) {
        super(message);
    }
}
