package com.alfa.device_ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;

/** Android-side representation of interactive-session.v1 with Gate 6 provenance binding. */
public final class InteractiveSessionContract {
    public static final String SCHEMA_VERSION = "interactive-session.v1";
    public static final String POLICY_ID = "interactive-runtime.v1";
    public static final int POLICY_VERSION = 1;
    public static final String POLICY_SCOPE = "full-user-access-inside-selected-rootless-runtime";

    private final String sessionId;
    private final String requestId;
    private final String pipelineRunId;
    private final String runtimeId;
    private final String sourceCommit;
    private final String gate4ContractSha256;
    private final String profileSha256;
    private final String implementationCommit;
    private final File runtimeReadyEvidence;
    private final File prootExecutable;
    private final File runtimeRoot;
    private final File hostCwd;
    private final String[] environment;

    public InteractiveSessionContract(
            String sessionId, String requestId, String pipelineRunId, String runtimeId,
            File runtimeReadyEvidence, File prootExecutable, File runtimeRoot, File hostCwd,
            String[] environment) {
        this(sessionId, requestId, pipelineRunId, runtimeId,
                "0000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000",
                runtimeReadyEvidence, prootExecutable, runtimeRoot, hostCwd, environment);
    }

    public InteractiveSessionContract(
            String sessionId,
            String requestId,
            String pipelineRunId,
            String runtimeId,
            String sourceCommit,
            String gate4ContractSha256,
            String profileSha256,
            String implementationCommit,
            File runtimeReadyEvidence,
            File prootExecutable,
            File runtimeRoot,
            File hostCwd,
            String[] environment) {
        Properties launch = readLaunchContract(runtimeReadyEvidence);
        this.sessionId = requireToken(sessionId, "sessionId");
        this.requestId = requireToken(requestId, "requestId");
        this.pipelineRunId = requireToken(launch.getProperty("pipeline_run_id", pipelineRunId), "pipelineRunId");
        this.runtimeId = requireToken(launch.getProperty("runtime_id", runtimeId), "runtimeId");
        this.sourceCommit = requireHex(launch.getProperty("source_commit", sourceCommit), 40, "sourceCommit");
        this.gate4ContractSha256 = requireHex(launch.getProperty("gate4_contract_sha256", gate4ContractSha256), 64, "gate4ContractSha256");
        this.profileSha256 = requireHex(launch.getProperty("profile_sha256", profileSha256), 64, "profileSha256");
        this.implementationCommit = requireHex(launch.getProperty("implementation_commit", implementationCommit), 40, "implementationCommit");
        this.runtimeReadyEvidence = requireFile(runtimeReadyEvidence, "runtimeReadyEvidence");
        this.prootExecutable = requireFile(prootExecutable, "prootExecutable");
        this.runtimeRoot = requireFile(runtimeRoot, "runtimeRoot");
        this.hostCwd = requireFile(hostCwd, "hostCwd");
        this.environment = withDerivedLoader(environment, prootExecutable);
    }

    private static Properties readLaunchContract(File runtimeReadyEvidence) {
        if (runtimeReadyEvidence == null || runtimeReadyEvidence.getParentFile() == null) throw new IllegalArgumentException("runtimeReadyEvidence is invalid");
        File runtimeVault = runtimeReadyEvidence.getParentFile().getParentFile().getParentFile();
        if (runtimeVault == null) throw new IllegalArgumentException("runtime vault is invalid");
        File launchFile = new File(runtimeVault, "gate7-launch.properties");
        if (!launchFile.isFile()) throw new IllegalStateException("Gate 7 launch contract is missing");
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(launchFile)) {
            p.load(in);
            requireProperty(p, "pipeline_run_id");
            requireProperty(p, "runtime_id");
            requireProperty(p, "source_commit");
            requireProperty(p, "gate4_contract_sha256");
            requireProperty(p, "profile_sha256");
            requireProperty(p, "implementation_commit");
            return p;
        } catch (Exception error) {
            throw new IllegalStateException("Gate 7 launch contract is unreadable", error);
        }
    }

    private static String[] withDerivedLoader(String[] input, File prootExecutable) {
        String[] base = input == null ? new String[0] : input.clone();
        for (String entry : base) if (entry != null && entry.startsWith("PROOT_LOADER=")) return base;
        try {
            File loader = new File(prootExecutable.getCanonicalFile().getParentFile(), "libproot-loader.so").getCanonicalFile();
            return Arrays.copyOf(base, base.length + 1);
        } catch (Exception error) {
            return base;
        }
    }

    private static void requireProperty(Properties p, String key) {
        if (p.getProperty(key) == null || p.getProperty(key).trim().isEmpty()) throw new IllegalStateException("Gate 7 launch contract missing " + key);
    }

    public boolean isAuthorizedForInteractiveRuntime() {
        return POLICY_ID.equals("interactive-runtime.v1") && POLICY_VERSION == 1
                && POLICY_SCOPE.equals("full-user-access-inside-selected-rootless-runtime")
                && RuntimeEvidence.verify(runtimeReadyEvidence, runtimeId, prootExecutable, runtimeRoot)
                && prootExecutable.isFile() && prootExecutable.canExecute() && runtimeRoot.isDirectory() && hostCwd.isDirectory()
                && !runtimeRoot.getAbsolutePath().startsWith("/home/userland") && !hostCwd.getAbsolutePath().startsWith("/home/userland")
                && hasValidProotTmpDir() && hasValidProotLoader() && !hasUnsafeEnvironmentPath();
    }

    private boolean hasValidProotTmpDir() {
        String value = null;
        for (String entry : environment) if (entry != null && entry.startsWith("PROOT_TMP_DIR=")) { value = entry.substring("PROOT_TMP_DIR=".length()); break; }
        if (value == null || value.isEmpty()) return false;
        try {
            File tmp = new File(value).getCanonicalFile();
            File runtime = runtimeRoot.getCanonicalFile();
            return tmp.isDirectory() && tmp.canWrite() && tmp.canExecute() && !tmp.getAbsolutePath().startsWith("/home/userland") && tmp.toPath().startsWith(runtime.toPath());
        } catch (Exception error) { return false; }
    }

    private boolean hasValidProotLoader() {
        for (String entry : environment) {
            if (entry != null && entry.startsWith("PROOT_LOADER=")) {
                try {
                    File loader = new File(entry.substring("PROOT_LOADER=".length())).getCanonicalFile();
                    File nativeDir = prootExecutable.getCanonicalFile().getParentFile();
                    return loader.isFile() && loader.canExecute() && nativeDir != null && loader.getParentFile().equals(nativeDir);
                } catch (Exception error) { return false; }
            }
        }
        return false;
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
    public String sourceCommit() { return sourceCommit; }
    public String gate4ContractSha256() { return gate4ContractSha256; }
    public String profileSha256() { return profileSha256; }
    public String implementationCommit() { return implementationCommit; }
    public File runtimeReadyEvidence() { return runtimeReadyEvidence; }
    public File prootExecutable() { return prootExecutable; }
    public File runtimeRoot() { return runtimeRoot; }
    public File hostCwd() { return hostCwd; }
    public String[] environment() { return environment.clone(); }

    public String[] prootArguments() {
        return new String[] { "-0", "-r", runtimeRoot.getAbsolutePath(), "-b", "/dev", "-b", "/proc", "-b", "/sys", "-w", "/root", "/usr/bin/env", "-i", "HOME=/root", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "TERM=xterm-256color", "/bin/sh", "-i" };
    }

    private static String requireToken(String value, String name) {
        if (value == null || value.trim().isEmpty() || value.indexOf('\0') >= 0) throw new IllegalArgumentException(name + " is invalid");
        return value;
    }

    private static String requireHex(String value, int length, String name) {
        if (value == null || !value.matches("[0-9a-fA-F]{" + length + "}")) throw new IllegalArgumentException(name + " is invalid");
        return value.toLowerCase();
    }

    private static File requireFile(File value, String name) {
        if (value == null || value.getAbsolutePath().startsWith("/home/userland")) throw new IllegalArgumentException(name + " is outside Alfa boundary");
        return value;
    }
}
