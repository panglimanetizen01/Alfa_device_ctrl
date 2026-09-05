package com.alfa.device_ctrl;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Atomic operation-evidence.v1 writer for audit and UI consumption. */
public final class OperationEvidence {
    public static final String SCHEMA_VERSION = "operation-evidence.v1";

    private OperationEvidence() { }

    public static File write(InteractiveSessionContract contract, String state, String result) {
        if (contract == null || state == null || result == null) return null;
        try {
            File parent = new File(contract.runtimeReadyEvidence().getParentFile(), "evidence/sessions");
            if (!parent.exists() && !parent.mkdirs()) return null;
            File finalFile = new File(parent, contract.sessionId() + ".properties");
            File temp = new File(parent, finalFile.getName() + ".part");
            Properties p = new Properties();
            p.setProperty("schema_version", SCHEMA_VERSION);
            p.setProperty("session_id", contract.sessionId());
            p.setProperty("request_id", contract.requestId());
            p.setProperty("pipeline_run_id", contract.pipelineRunId());
            p.setProperty("runtime_id", contract.runtimeId());
            p.setProperty("policy_id", InteractiveSessionContract.POLICY_ID);
            p.setProperty("policy_version", Integer.toString(InteractiveSessionContract.POLICY_VERSION));
            p.setProperty("policy_scope", InteractiveSessionContract.POLICY_SCOPE);
            p.setProperty("state", state);
            p.setProperty("result", result);
            p.setProperty("environment_profile", String.join(";", contract.environment()));
            p.setProperty("runtime_evidence", contract.runtimeReadyEvidence().getCanonicalPath());
            p.setProperty("engine_path", contract.prootExecutable().getCanonicalPath());
            p.setProperty("rootfs_path", contract.runtimeRoot().getCanonicalPath());
            p.setProperty("android_boundary", "rootless-selected-runtime-only; no-android-root; no-other-app-private-data");
            p.setProperty("created_at_epoch_ms", Long.toString(System.currentTimeMillis()));
            try (FileOutputStream out = new FileOutputStream(temp)) { p.store(out, "Alfa Device Ctrl operation evidence"); }
            Files.move(temp.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return finalFile;
        } catch (Exception ignored) {
            return null;
        }
    }
}
