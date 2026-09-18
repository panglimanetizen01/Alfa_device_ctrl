package com.alfa.device_ctrl.external;

import com.alfa.device_ctrl.ExecutionLane;

/** Immutable evidence snapshot for an external process execution. */
public final class ExternalExecutionResult {
    private final ExecutionLane lane;
    private final long pid;
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final String error;

    public ExternalExecutionResult(
            ExecutionLane lane,
            long pid,
            int exitCode,
            String stdout,
            String stderr,
            String error) {
        this.lane = lane;
        this.pid = pid;
        this.exitCode = exitCode;
        this.stdout = stdout == null ? "" : stdout;
        this.stderr = stderr == null ? "" : stderr;
        this.error = error;
    }

    public ExecutionLane getLane() {
        return lane;
    }

    public long getPid() {
        return pid;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public String getError() {
        return error;
    }

    public boolean isProcessStarted() {
        return pid > 0;
    }

    public boolean isOutputCaptured() {
        return !stdout.isEmpty() || !stderr.isEmpty();
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    public boolean isSuccessful() {
        return isProcessStarted() && !hasError() && exitCode == 0;
    }
}
