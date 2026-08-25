package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.idb.auth.common.constant.OperationStatus;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;

/**
 * The two service exceptions carry a client-safe {@code ApiResponse} alongside a
 * detailed log message. The split matters: the log message may name the user and
 * the failure, the response must not.
 */
class ServiceExceptionTest {

    @Test
    @DisplayName("LogOnlyException keeps the log detail out of the client response")
    void logOnlySeparatesLogFromResponse() {
        LogOnlyException ex = LogOnlyException.of(
                "User %s attempted to change password with incorrect old password", "Old password is incorrect",
                "alice");

        assertThat(ex.getMessage()).contains("alice").contains("incorrect old password");
        assertThat(ex.getResponse().getMessage()).isEqualTo("Old password is incorrect");
        assertThat(ex.getResponse().getStatus()).isEqualTo(OperationStatus.ERROR);
        // The client-facing message must not name the account.
        assertThat(ex.getResponse().getMessage()).doesNotContain("alice");
    }

    @Test
    @DisplayName("a null log message is allowed - the response is the only payload")
    void nullLogMessageIsAllowed() {
        LogOnlyException ex = LogOnlyException.of(null, "User not found");
        assertThat(ex.getMessage()).isNull();
        assertThat(ex.getResponse().getMessage()).isEqualTo("User not found");
    }

    @Test
    @DisplayName("TraceableException prefixes the caller location and keeps the cause")
    void traceablePrefixesCallerInfo() {
        RuntimeException cause = new RuntimeException("root cause");
        TraceableException ex = TraceableException.of("Login failed for user %s", cause, "Login failed", "bob");

        assertThat(ex.getMessage())
                .contains("Error in")
                .contains("traceablePrefixesCallerInfo")
                .contains("root cause")
                .contains("bob");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getResponse().getMessage()).isEqualTo("Login failed");
        assertThat(ex.getResponse().getStatus()).isEqualTo(OperationStatus.ERROR);
    }

    @Test
    @DisplayName("caller info degrades gracefully with an empty stack or null cause")
    void callerInfoIsFailSafe() {
        assertThat(TraceableException.getCallerInfo(new StackTraceElement[0], null))
                .contains("Unknown");
        assertThat(TraceableException.of("msg", new RuntimeException(), "resp").getMessage())
                .contains("Error in");
    }
}
