package com.alfa.device_ctrl;

/** Strict runtime action semantics used by the canonical Stitch DUT renderer. */
public final class StitchV1RuntimeStateMachine {
    public enum State { NOT_INSTALLED, VERIFYING, READY, FAILED }
    public enum Action { CHECK, INSTALL, OPEN }

    private StitchV1RuntimeStateMachine() {}

    public static State from(RuntimeUiState.Status status) {
        if (status == RuntimeUiState.Status.READY) return State.READY;
        if (status == RuntimeUiState.Status.VERIFYING) return State.VERIFYING;
        if (status == RuntimeUiState.Status.FAILED) return State.FAILED;
        return State.NOT_INSTALLED;
    }

    public static Action primaryAction(State state) {
        return state == State.READY ? Action.OPEN : Action.INSTALL;
    }

    public static boolean showInstall(State state) { return primaryAction(state) == Action.INSTALL; }
    public static boolean showOpen(State state) { return primaryAction(state) == Action.OPEN; }
}
