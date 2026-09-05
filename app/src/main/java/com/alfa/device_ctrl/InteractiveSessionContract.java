package com.alfa.device_ctrl;

import java.io.File;

/**
 * Small Android-side representation of interactive-session.v1.
 * It deliberately rejects the Gate 5 pwd self-test as a session authorization.
 */
public final class InteractiveSessionContract {
    public static final String SCHEMA_VERSION = "interactive-session.v1";
    public static final String POLICY_ID = "interactive-runtime.v1";
    public static final int POLICY_VERSION = 1;
    public static final String POLICY_SCOPE = "full-user-access-inside-selected-rootless-runtime";

    private final String sessionId;
    private final String requestId;
    private final String pipelineRunId;
    private final String runtimeId;
    private final File runtimeReadyEvidence;
    private final File prootExecutable;
    private final File runtimeRoot;
    private final File hostCwd;
    private final String[] environment;

    public InteractiveSessionContract(
            String sessionId,
            String requestId,
            String pipelineRunId,
            String runtimeId,
            File runtimeReadyEvidence,
            File prootExecutable,
            File runtimeRoot,
            File hostCwd,
            String[] environment) {
        this.sessionId = requireToken(sessionId, "sessionId");
        this.requestId = requireToken(requestId, "requestId");
        this.pipelineRunId = requireToken(pipelineRunId, "pipelineRunId");
        this.runtimeId = requireToken(runtimeId, "runtimeId");
        this.runtimeReadyEvidence = requireFile(runtimeReadyEvidence, "runtimeReadyEvidence");
        this.prootExecutable = requireFile(prootExecutable, "prootExecutable");
        this.runtimeRoot = requireFile(runtimeRoot, "runtimeRoot");
        this.hostCwd = requireFile(hostCwd, "hostCwd");
        this.environment = environment == null ? new String[0] : environment.clone();
    }

    public boolean isAuthorizedForInteractiveRuntime() {
        // Gate 5 V1 pwd self-test is intentionally not accepted here.
        return POLICY_ID.equals("interactive-runtime.v1")
                && POLICY_VERSION == 1
                && POLICY_SCOPE.equals("full-user-access-inside-selected-rootless-runtime")
                && RuntimeEvidence.verify(runtimeReadyEvidence, runtimeId, prootExecutable, runtimeRoot)
                && prootExecutable.isFile()
                && prootExecutable.canExecute()
                && runtimeRoot.isDirectory()
                && hostCwd.isDirectory()
                && !runtimeRoot.getAbsolutePath().startsWith("/home/userland")
                && !hostCwd.getAbsolutePath().startsWith("/home/userland")
                && hasValidProotTmpDir()
                && !hasUnsafeEnvironmentPath();
    }

    private boolean hasValidProotTmpDir() {
        String value = null;
        for (String entry : environment) {
            if (entry != null && entry.startsWith("PROOT_TMP_DIR=")) {
                value = entry.substring("PROOT_TMP_DIR=".length());
                break;
            }
        }
        if (value == null || value.isEmpty()) return false;

        try {
            File tmp = new File(value).getCanonicalFile();
            File runtime = runtimeRoot.getCanonicalFile();
            return tmp.isDirectory()
                    && tmp.canWrite()
                    && tmp.canExecute()
                    && !tmp.getAbsolutePath().startsWith("/home/userland")
                    && tmp.toPath().startsWith(runtime.toPath());
        } catch (Exception error) {
            return false;
        }
    }

    private boolean hasUnsafeEnvironmentPath() {
        for (String entry : environment) {
            if (entry == null || entry.indexOf('\0') >= 0) return true;
            if (entry.startsWith("HOME=/home/userland") || entry.startsWith("TMPDIR=/home/userland")) return true;
        }
        return false;
    }

    public String sessionId() { return sessionId; }
    public String requestId() { return requestId; }
    public String pipelineRunId() { return pipelineRunId; }
    public String runtimeId() { return runtimeId; }
    public File runtimeReadyEvidence() { return runtimeReadyEvidence; }
    public File prootExecutable() { return prootExecutable; }
    public File runtimeRoot() { return runtimeRoot; }
    public File hostCwd() { return hostCwd; }
    public String[] environment() { return environment.clone(); }

    public String[] prootArguments() {
        return new String[] {
                "-0", "-r", runtimeRoot.getAbsolutePath(),
                "-b", "/dev", "-b", "/proc", "-b", "/sys",
                "-w", "/root",
                "/usr/bin/env", "-i",
                "HOME=/root",
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "TERM=xterm-256color",
                "/bin/sh", "-i"
        };
    }

    private static String requireToken(String value, String name) {
        if (value == null || value.trim().isEmpty() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(name + " is invalid");
        }
        return value;
    }

    private static File requireFile(File value, String name) {
        if (value == null || value.getAbsolutePath().startsWith("/home/userland")) {
            throw new IllegalArgumentException(name + " is outside Alfa boundary");
        }
        return value;
    }
}
