package com.alfa.device_ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;

/** Android-side representation of interactive-session.v1 with runtime-bound Gate 7 provenance. */
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
    private final String[] directoryOverrideBinds;

    public InteractiveSessionContract(
            String sessionId, String requestId, String pipelineRunId, String runtimeId,
            File runtimeReadyEvidence, File prootExecutable, File runtimeRoot, File hostCwd,
            String[] environment) {
        this(sessionId, requestId, pipelineRunId, runtimeId,
                "0000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000",
                runtimeReadyEvidence, prootExecutable, runtimeRoot, hostCwd, environment, new String[0]);
    }

    public InteractiveSessionContract(
            String sessionId, String requestId, String pipelineRunId, String runtimeId,
            File runtimeReadyEvidence, File prootExecutable, File runtimeRoot, File hostCwd,
            String[] environment, String[] directoryOverrideBinds) {
        this(sessionId, requestId, pipelineRunId, runtimeId,
                "0000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000",
                runtimeReadyEvidence, prootExecutable, runtimeRoot, hostCwd, environment, directoryOverrideBinds);
    }

    public InteractiveSessionContract(
            String sessionId, String requestId, String pipelineRunId, String runtimeId,
            String sourceCommit, String gate4ContractSha256, String profileSha256, String implementationCommit,
            File runtimeReadyEvidence, File prootExecutable, File runtimeRoot, File hostCwd, String[] environment) {
        this(sessionId, requestId, pipelineRunId, runtimeId, sourceCommit, gate4ContractSha256, profileSha256, implementationCommit,
                runtimeReadyEvidence, prootExecutable, runtimeRoot, hostCwd, environment, new String[0]);
    }

    public InteractiveSessionContract(
            String sessionId, String requestId, String pipelineRunId, String runtimeId,
            String sourceCommit, String gate4ContractSha256, String profileSha256, String implementationCommit,
            File runtimeReadyEvidence, File prootExecutable, File runtimeRoot, File hostCwd, String[] environment,
            String[] directoryOverrideBinds) {
        this.sessionId = requireToken(sessionId, "sessionId");
        this.requestId = requireToken(requestId, "requestId");
        this.runtimeId = requireToken(runtimeId, "runtimeId");
        if (RuntimeRegistry.get(this.runtimeId) == null) throw new IllegalArgumentException("unsupported-runtime-id");
        Properties launch = readLaunchContract(runtimeReadyEvidence, this.runtimeId);
        this.pipelineRunId = requireToken(launch.getProperty("pipeline_run_id", pipelineRunId), "pipelineRunId");
        String attestedRuntimeId = requireToken(launch.getProperty("runtime_id"), "attestedRuntimeId");
        if (!this.runtimeId.equals(attestedRuntimeId)) throw new IllegalArgumentException("attested-runtime-mismatch");
        this.sourceCommit = requireHex(launch.getProperty("source_commit", sourceCommit), 40, "sourceCommit");
        this.gate4ContractSha256 = requireHex(launch.getProperty("gate4_contract_sha256", gate4ContractSha256), 64, "gate4ContractSha256");
        this.profileSha256 = requireHex(launch.getProperty("profile_sha256", profileSha256), 64, "profileSha256");
        this.implementationCommit = requireHex(launch.getProperty("implementation_commit", implementationCommit), 40, "implementationCommit");
        this.runtimeReadyEvidence = requireFile(runtimeReadyEvidence, "runtimeReadyEvidence");
        this.prootExecutable = requireFile(prootExecutable, "prootExecutable");
        this.runtimeRoot = requireFile(runtimeRoot, "runtimeRoot");
        this.hostCwd = requireFile(hostCwd, "hostCwd");
        this.environment = withLoader(environment, this.prootExecutable);
        this.directoryOverrideBinds = validateBinds(directoryOverrideBinds);
    }

    private static Properties readLaunchContract(File runtimeReadyEvidence, String runtimeId) {
        if (runtimeReadyEvidence == null || runtimeReadyEvidence.getParentFile() == null) throw new IllegalArgumentException("runtimeReadyEvidence is invalid");
        File runtimeVault = runtimeReadyEvidence.getParentFile().getParentFile().getParentFile();
        if (runtimeVault == null) throw new IllegalArgumentException("runtime vault is invalid");
        File launchFile = new File(runtimeVault, "gate7-launch.properties");
        if (!Gate6LaunchContract.verify(launchFile, runtimeId)) throw new IllegalStateException("Gate 7 launch contract is missing, stale, unauthorized, or runtime-bound to another profile");
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(launchFile)) { p.load(in); return p; }
        catch (Exception error) { throw new IllegalStateException("Gate 7 launch contract is unreadable", error); }
    }

    private static String[] withLoader(String[] supplied, File prootExecutable) {
        ArrayList<String> values = new ArrayList<>();
        if (supplied != null) for (String entry : supplied) if (entry != null && !entry.startsWith("PROOT_LOADER=")) values.add(entry);
        File parent = prootExecutable == null ? null : prootExecutable.getParentFile();
        File loader = parent == null ? null : new File(parent, "libproot-loader.so");
        if (loader == null || !loader.isFile() || !loader.canExecute()) values.add("PROOT_LOADER=");
        else try { values.add("PROOT_LOADER=" + loader.getCanonicalPath()); } catch (Exception error) { values.add("PROOT_LOADER="); }
        return values.toArray(new String[0]);
    }

    private static String[] validateBinds(String[] supplied) {
        if (supplied == null || supplied.length == 0) return new String[0];
        String[] result = supplied.clone();
        for (String bind : result) {
            if (bind == null || bind.indexOf('\0') >= 0 || bind.indexOf('|') >= 0 || bind.indexOf(':') <= 0 || bind.indexOf(':') == bind.length() - 1) throw new IllegalArgumentException("directory-override-bind-invalid");
            int separator = bind.indexOf(':');
            String host = bind.substring(0, separator);
            String guest = bind.substring(separator + 1);
            RuntimeDirectoryOverride.canonicalDirectory(host);
            if (!(guest.startsWith("/mnt/") || guest.startsWith("/workspace/"))) throw new IllegalArgumentException("directory-override-bind-invalid");
        }
        return result;
    }

    public boolean isAuthorizedForInteractiveRuntime() {
        return POLICY_ID.equals("interactive-runtime.v1") && POLICY_VERSION == 1
                && POLICY_SCOPE.equals("full-user-access-inside-selected-rootless-runtime")
                && RuntimeRegistry.get(runtimeId) != null
                && RuntimeEvidence.verify(runtimeReadyEvidence, runtimeId, prootExecutable, runtimeRoot)
                && prootExecutable.isFile() && prootExecutable.canExecute() && prootLoaderIsValid()
                && runtimeRoot.isDirectory() && hostCwd.isDirectory()
                && !runtimeRoot.getAbsolutePath().startsWith("/home/userland") && !hostCwd.getAbsolutePath().startsWith("/home/userland")
                && hasValidProotTmpDir() && !hasUnsafeEnvironmentPath();
    }

    private boolean prootLoaderIsValid() {
        for (String entry : environment) if (entry != null && entry.startsWith("PROOT_LOADER=")) {
            String value = entry.substring("PROOT_LOADER=".length());
            return isCanonicalProotLoaderValue(value, prootExecutable);
        }
        return false;
    }

    static boolean isCanonicalProotLoaderValue(String supplied, File prootExecutable) {
        if (supplied == null || supplied.isEmpty() || prootExecutable == null) return false;
        try {
            File executableParent = prootExecutable.getCanonicalFile().getParentFile();
            if (executableParent == null) return false;
            File expectedLoader = new File(executableParent, "libproot-loader.so").getCanonicalFile();
            return supplied.equals(expectedLoader.getAbsolutePath()) && expectedLoader.isFile() && expectedLoader.canExecute();
        } catch (Exception error) { return false; }
    }

    private boolean hasValidProotTmpDir() {
        String value = null; for (String entry : environment) if (entry != null && entry.startsWith("PROOT_TMP_DIR=")) { value = entry.substring("PROOT_TMP_DIR=".length()); break; }
        if (value == null || value.isEmpty()) return false;
        try {
            Path vaultPath = runtimeRoot.getCanonicalFile().getParentFile().getParentFile().getCanonicalFile().toPath().normalize();
            Path candidatePath = Paths.get(value.trim()).toAbsolutePath().normalize();
            if (!candidatePath.startsWith(vaultPath)) return false;
            File tmp = candidatePath.toFile().getCanonicalFile();
            return tmp.isDirectory() && tmp.canWrite() && tmp.canExecute()
                    && tmp.toPath().normalize().startsWith(vaultPath)
                    && !tmp.toPath().normalize().startsWith(runtimeRoot.getCanonicalFile().toPath().normalize().resolve("rootfs"));
        } catch (Exception error) { return false; }
    }

    private boolean hasUnsafeEnvironmentPath() {
        for (String entry : environment) { if (entry == null || entry.indexOf('\0') >= 0) return true; if (entry.startsWith("HOME=/home/userland") || entry.startsWith("TMPDIR=/home/userland")) return true; }
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
    public String[] directoryOverrideBinds() { return directoryOverrideBinds.clone(); }

    public String[] prootArguments() {
        RuntimeProfile profile = RuntimeRegistry.get(runtimeId);
        if (profile == null) throw new IllegalStateException("unsupported-runtime-id");
        return resolveProotArguments(profile, runtimeRoot, directoryOverrideBinds);
    }

    static String[] resolveProotArguments(RuntimeProfile profile, File runtimeRoot, String[] directoryOverrideBinds) {
        if (profile == null || runtimeRoot == null) throw new IllegalArgumentException("proot-arguments-invalid");
        String prompt = profile.promptContract() + "\\w\\$ ";
        String[] template = profile.prootArguments();
        ArrayList<String> resolved = new ArrayList<>();
        int commandStart = template.length;
        for (int i = 0; i < template.length; i++) {
            String value = template[i];
            if (isCommandToken(value, profile)) { commandStart = i; break; }
            resolved.add(value.replace("{RUNTIME_ROOT}", runtimeRoot.getAbsolutePath()).replace("{SHELL}", profile.shell()).replace("{PROMPT}", prompt));
        }
        if (directoryOverrideBinds != null) for (String bind : directoryOverrideBinds) { resolved.add("-b"); resolved.add(bind); }
        for (int i = commandStart; i < template.length; i++) {
            String value = template[i];
            resolved.add(value.replace("{RUNTIME_ROOT}", runtimeRoot.getAbsolutePath()).replace("{SHELL}", profile.shell()).replace("{PROMPT}", prompt));
        }
        return resolved.toArray(new String[0]);
    }

    private static boolean isCommandToken(String value, RuntimeProfile profile) {
        return profile.shell().equals(value) || "/usr/bin/env".equals(value);
    }

    private static String requireToken(String value, String name) { if (value == null || value.trim().isEmpty() || value.indexOf('\0') >= 0) throw new IllegalArgumentException(name + " is invalid"); return value; }
    private static String requireHex(String value, int length, String name) { if (value == null || !value.matches("[0-9a-fA-F]{" + length + "}")) throw new IllegalArgumentException(name + " is invalid"); return value.toLowerCase(); }
    private static File requireFile(File value, String name) { if (value == null || value.getAbsolutePath().startsWith("/home/userland")) throw new IllegalArgumentException(name + " is outside Alfa boundary"); return value; }
}
