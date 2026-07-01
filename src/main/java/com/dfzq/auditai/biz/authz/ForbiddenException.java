package com.dfzq.auditai.biz.authz;

/** jCasbin 授权拒绝(越权)。请求层映射为 B102(§8.3)。 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
