package com.orientsec.idap.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class LogHelperTest {

    @Test
    void ignoresMissingLogger() {
        assertThatCode(() -> LogHelper.log(null, "ignored")).doesNotThrowAnyException();
        assertThatCode(() -> LogHelper.error(null, new IllegalStateException("ignored"), "ctx"))
                .doesNotThrowAnyException();
    }

    @Test
    void logsContextAndExceptionWithStableErrorMessage() {
        Logger logger = mock(Logger.class);
        IllegalStateException exception = new IllegalStateException("sensitive detail");

        LogHelper.error(logger, exception, "查询用户失败", "U100");

        assertThat(mockingDetails(logger).getInvocations())
                .anySatisfy(
                        invocation -> {
                            assertThat(invocation.getMethod().getName()).isEqualTo("info");
                            assertThat(invocation.getArguments()).containsExactly("查询用户失败 U100");
                        });
        verify(logger).error("sensitive detail", exception);
    }
}
