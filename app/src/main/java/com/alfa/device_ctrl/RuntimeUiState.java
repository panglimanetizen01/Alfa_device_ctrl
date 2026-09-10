package com.alfa.device_ctrl;

import java.io.File;

/** Pure UI-facing projection of canonical runtime installation/evidence state. */
public final class RuntimeUiState {
    public enum Status { NOT_INSTALLED, INSTALLED, VERIFYING, READY, FAILED }

    private RuntimeUiState() { }

    public static Status resolve(String runtimeId, File runtimeRoot, File readyEvidence, File engine, File rootfs) {
        if (runtimeId == null || runtimeRoot == null || !runtimeRoot.exists()) return Status.NOT_INSTALLED;
        if (readyEvidence == null || !readyEvidence.isFile()) {
            return rootfs != null && rootfs.isDirectory() ? Status.INSTALLED : Status.VERIFYING;
        }
        if (engine != null && rootfs != null && RuntimeEvidence.verify(readyEvidence, runtimeId, engine, rootfs)) {
            return Status.READY;
        }
        return Status.FAILED;
    }

    public static String label(Status status) {
        switch (status) {
            case READY: return "READY";
            case VERIFYING: return "VERIFYING";
            case INSTALLED: return "INSTALLED";
            case FAILED: return "VERIFY FAILED";
            default: return "NOT INSTALLED";
        }
    }

    public static boolean canOpen(Status status) { return status == Status.READY; }
}
