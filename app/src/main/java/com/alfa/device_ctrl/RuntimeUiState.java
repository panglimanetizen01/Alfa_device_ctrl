package com.alfa.device_ctrl;

import java.io.File;

/** Pure UI-facing projection of canonical runtime installation/evidence state. */
public final class RuntimeUiState {
    public enum Status { NOT_INSTALLED, INSTALLED, VERIFYING, READY, PARTIAL, FAILED }

    private RuntimeUiState() { }

    public static Status resolve(String runtimeId, File runtimeRoot, File readyEvidence, File engine, File rootfs) {
        if (runtimeId == null || runtimeId.trim().isEmpty() || runtimeRoot == null || !runtimeRoot.exists()) {
            return Status.NOT_INSTALLED;
        }
        boolean hasRootfs = rootfs != null && rootfs.isDirectory();
        boolean hasEvidence = readyEvidence != null && readyEvidence.isFile();
        boolean hasEngine = engine != null && engine.isFile() && engine.canExecute();
        if (!hasEvidence) {
            return hasRootfs ? Status.INSTALLED : Status.PARTIAL;
        }
        if (!hasEngine || !hasRootfs) return Status.FAILED;
        return RuntimeEvidence.verify(readyEvidence, runtimeId, engine, rootfs)
                ? Status.READY : Status.FAILED;
    }

    public static String label(Status status) {
        switch (status) {
            case READY: return "READY";
            case VERIFYING: return "VERIFYING";
            case INSTALLED: return "INSTALLED";
            case PARTIAL: return "PARTIAL";
            case FAILED: return "VERIFY FAILED";
            default: return "NOT INSTALLED";
        }
    }

    public static boolean canOpen(Status status) { return status == Status.READY; }
}
