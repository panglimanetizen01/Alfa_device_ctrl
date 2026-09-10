package com.alfa.device_ctrl;

/**
 * Semantic UI projection of the exact RuntimeSessionManager event vocabulary.
 * Unknown events are fail-closed and never become RUNNING.
 */
public final class SessionUiState {
    public enum Status { NOT_READY, STARTING, RUNNING, FAILED, FINISHED }

    private SessionUiState() { }

    public static Status resolve(String event) {
        if (event == null) return Status.NOT_READY;
        switch (event) {
            case "PTY_CREATED":
            case "PTY_WAITING_FOR_PROMPT":
                return Status.STARTING;
            case "READY":
            case "RUNNING":
                return Status.RUNNING;
            case "FINISHED":
                return Status.FINISHED;
            case "BLOCKED":
            case "BLOCKED_FGS_START":
            case "STOPPING":
                return Status.FAILED;
            case "BACKGROUND_SESSION_PRESERVED":
            default:
                return Status.NOT_READY;
        }
    }

    public static String label(Status status) {
        switch (status) {
            case STARTING: return "STARTING";
            case RUNNING: return "RUNNING";
            case FAILED: return "FAILED";
            case FINISHED: return "FINISHED";
            default: return "NOT READY";
        }
    }
}
