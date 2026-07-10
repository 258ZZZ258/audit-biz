package com.orientsec.idap.common.utils;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;

/** Project-compatible logging helper used by controllers migrated from the intranet base. */
public final class LogHelper {

    private LogHelper() {}

    public static void log(Logger logger, Object... message) {
        if (logger != null) {
            logger.info(StrUtil.join(" ", message));
        }
    }

    public static void error(Logger logger, Exception exception, Object... context) {
        if (logger == null) {
            return;
        }
        log(logger, context);
        logger.error(exception == null ? "业务处理失败" : exception.getMessage(), exception);
    }
}
