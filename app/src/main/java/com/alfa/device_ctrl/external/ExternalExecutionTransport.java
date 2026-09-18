package com.alfa.device_ctrl.external;

import com.alfa.device_ctrl.ExecutionLane;

/** Common evidence-facing contract for external process lanes. */
public interface ExternalExecutionTransport {
    interface Callback {
        void onStarted(long pid, int uid);
        void onStdout(String chunk);
        void onStderr(String chunk);
        void onExit(int exitCode);
        void onError(String message);
    }

    ExecutionLane lane();
    boolean isAvailable();
    boolean hasPermission();
    boolean supportsStreamingStdin();
    void execute(String[] command, String workDir, String initialStdin, Callback callback);
    void writeStdin(String input);
    void terminate();
}
