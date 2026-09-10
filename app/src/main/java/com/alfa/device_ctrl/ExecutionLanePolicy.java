package com.alfa.device_ctrl;

/**
 * Semantics for the boundary between Alfa's UI/runtime state and external
 * phone-control lanes. External lanes can provide device-control evidence, but
 * they do not become a hidden prerequisite for the Linux runtime UI.
 */
public final class ExecutionLanePolicy {
    private ExecutionLanePolicy() {
    }

    /** Runtime readiness is established by the canonical runtime evidence chain. */
    public static boolean runtimeReadinessDependsOnlyOnRuntimeEvidence() {
        return true;
    }

    /** External lane outages must not make an otherwise READY runtime look unavailable. */
    public static boolean externalLaneFailureBlocksRuntimeUi() {
        return false;
    }
}
