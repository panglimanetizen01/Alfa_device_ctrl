package com.alfa.device_ctrl;

/**
 * Semantic UI projection of the exact RuntimeSessionManager event vocabulary.
 * Unknown events are fail-closed and never become RUNNING.
 */
public final class SessionUiState {
    public enum Status { NOT_READY, STARTING, RUNNING, STOPPING, FAILED, FINISHED }

    private SessionUiState() { }

    public static Status resolve(String event) {
        if (event == null) return Status.NOT_READY;
        switch (event) {
            case "PTY_CREATED":
            case "PTY_WAITING_FOR_PROMPT":
                return Status.STARTING;
            case "READY":
            case "RUNNING":
            case "BACKGROUND_SESSION_PRESERVED":
                return Status.RUNNING;
            case "STOPPING":
                return Status.STOPPING;
            case "FINISHED":
                return Status.FINISHED;
            case "BLOCKED":
            case "BLOCKED_FGS_START":
                return Status.FAILED;
            default:
                return Status.NOT_READY;
        }
    }

    public static String label(Status status) {
        switch (status) {
            case STARTING: return "STARTING";
            case RUNNING: return "RUNNING";
            case STOPPING: return "STOPPING";
            case FAILED: return "FAILED";
            case FINISHED: return "FINISHED";
            default: return "NOT READY";
        }
    }
}
