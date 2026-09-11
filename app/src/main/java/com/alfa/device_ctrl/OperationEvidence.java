package com.alfa.device_ctrl;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Atomic operation-evidence.v1 writer for audit and G7 provenance verification. */
public final class OperationEvidence {
    public static final String SCHEMA_VERSION = "operation-evidence.v1";

    private OperationEvidence() { }

    public static File write(InteractiveSessionContract contract, String state, String result) {
        return write(contract, state, result, -1);
    }

    public static File write(InteractiveSessionContract contract, String state, String result, int processPid) {
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
            p.setProperty("source_commit", contract.sourceCommit());
            p.setProperty("gate4_contract_sha256", contract.gate4ContractSha256());
            p.setProperty("profile_sha256", contract.profileSha256());
            p.setProperty("implementation_commit", contract.implementationCommit());
            p.setProperty("policy_id", InteractiveSessionContract.POLICY_ID);
            p.setProperty("policy_version", Integer.toString(InteractiveSessionContract.POLICY_VERSION));
            p.setProperty("policy_scope", InteractiveSessionContract.POLICY_SCOPE);
            p.setProperty("state", state);
            p.setProperty("result", result);
            p.setProperty("process_pid", Integer.toString(processPid));
            PttyProof proof = probePty(processPid);
            p.setProperty("pty_status", proof.status);
            p.setProperty("pty_status_basis", proof.basis);
            p.setProperty("prompt_observed", "READY".equals(state) ? "PASS" : "PENDING");
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

    private static PttyProof probePty(int pid) {
        if (pid <= 0) return new PttyProof("NOT_MEASURED", "NO_VALID_PROCESS_PID");
        String stdin = readFdTarget(pid, 0);
        String stdout = readFdTarget(pid, 1);
        String stderr = readFdTarget(pid, 2);
        if (isPts(stdin) && isPts(stdout) && isPts(stderr)) {
            return new PttyProof("PASS", "PROC_FD_0_1_2_POINT_TO_DEV_PTS");
        }
        if (stdin == null && stdout == null && stderr == null) {
            return new PttyProof("UNKNOWN", "PROC_FD_PROBE_UNAVAILABLE");
        }
        return new PttyProof("FAIL", "PROC_FD_0_1_2_NOT_ALL_DEV_PTS");
    }

    private static String readFdTarget(int pid, int fd) {
        try {
            Path path = Path.of("/proc", Integer.toString(pid), "fd", Integer.toString(fd));
            return Files.readSymbolicLink(path).toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isPts(String target) {
        return target != null && (target.startsWith("/dev/pts/") || target.startsWith("/dev/pts/"));
    }

    private static final class PttyProof {
        final String status;
        final String basis;

        PttyProof(String status, String basis) {
            this.status = status;
            this.basis = basis;
        }
    }
}
