package com.alfa.device_ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
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
            String sessionId, String requestId, String pipelineRunId, String runtimeId,
            String sourceCommit, String gate4ContractSha256, String profileSha256, String implementationCommit,
            File runtimeReadyEvidence, File prootExecutable, File runtimeRoot, File hostCwd, String[] environment) {
        Properties launch = readLaunchContract(runtimeReadyEvidence);
        this.sessionId = requireToken(sessionId, "sessionId");
        this.requestId = requireToken(requestId, "requestId");
        this.pipelineRunId = requireToken(launch.getProperty("pipeline_run_id", pipelineRunId), "pipelineRunId");
        String attestedRuntimeId = requireToken(launch.getProperty("runtime_id"), "attestedRuntimeId");
        if (RuntimeRegistry.get(attestedRuntimeId) == null) throw new IllegalArgumentException("unsupported-attested-runtime-id");
        this.runtimeId = requireToken(runtimeId, "runtimeId");
        if (RuntimeRegistry.get(this.runtimeId) == null) throw new IllegalArgumentException("unsupported-runtime-id");
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
            requireProperty(p, "pipeline_run_id"); requireProperty(p, "runtime_id"); requireProperty(p, "source_commit");
            requireProperty(p, "gate4_contract_sha256"); requireProperty(p, "profile_sha256"); requireProperty(p, "implementation_commit");
            return p;
        } catch (Exception error) { throw new IllegalStateException("Gate 7 launch contract is unreadable", error); }
    }

    private static void requireProperty(Properties p, String key) { if (p.getProperty(key) == null || p.getProperty(key).trim().isEmpty()) throw new IllegalStateException("Gate 7 launch contract missing " + key); }

    private static String[] withLoader(String[] supplied, File prootExecutable) {
        ArrayList<String> values = new ArrayList<>();
        if (supplied != null) for (String entry : supplied) if (entry != null && !entry.startsWith("PROOT_LOADER=")) values.add(entry);
        File parent = prootExecutable == null ? null : prootExecutable.getParentFile();
        File loader = parent == null ? null : new File(parent, "libproot-loader.so");
        if (loader == null || !loader.isFile() || !loader.canExecute()) values.add("PROOT_LOADER=");
        else try { values.add("PROOT_LOADER=" + loader.getCanonicalPath()); } catch (Exception error) { values.add("PROOT_LOADER="); }
        return values.toArray(new String[0]);
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
            String value = entry.substring("PROOT_LOADER=".length()); File loader = new File(value);
            try { return !value.isEmpty() && loader.isFile() && loader.canExecute() && loader.getCanonicalFile().getParentFile().equals(prootExecutable.getCanonicalFile().getParentFile()); }
            catch (Exception error) { return false; }
        }
        return false;
    }

    private boolean hasValidProotTmpDir() {
        String value = null; for (String entry : environment) if (entry != null && entry.startsWith("PROOT_TMP_DIR=")) { value = entry.substring("PROOT_TMP_DIR=".length()); break; }
        if (value == null || value.isEmpty()) return false;
        try {
            File tmp = new File(value).getCanonicalFile(), runtime = runtimeRoot.getCanonicalFile(), vault = runtime.getParentFile().getParentFile().getCanonicalFile();
            return tmp.isDirectory() && tmp.canWrite() && tmp.canExecute() && !tmp.getAbsolutePath().startsWith("/home/userland")
                    && tmp.toPath().startsWith(vault.toPath()) && !tmp.toPath().startsWith(runtime.toPath().resolve("rootfs"));
        } catch (Exception error) { return false; }
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
        RuntimeProfile profile = RuntimeRegistry.get(runtimeId);
        if (profile == null) throw new IllegalStateException("unsupported-runtime-id");
        String prompt = profile.promptContract() + "\\w\\$ ";
        String[] template = profile.prootArguments(); String[] resolved = new String[template.length];
        for (int i = 0; i < template.length; i++) resolved[i] = template[i].replace("{RUNTIME_ROOT}", runtimeRoot.getAbsolutePath()).replace("{SHELL}", profile.shell()).replace("{PROMPT}", prompt);
        return resolved;
    }

    private static String requireToken(String value, String name) { if (value == null || value.trim().isEmpty() || value.indexOf('\0') >= 0) throw new IllegalArgumentException(name + " is invalid"); return value; }
    private static String requireHex(String value, int length, String name) { if (value == null || !value.matches("[0-9a-fA-F]{" + length + "}")) throw new IllegalArgumentException(name + " is invalid"); return value.toLowerCase(); }
    private static File requireFile(File value, String name) { if (value == null || value.getAbsolutePath().startsWith("/home/userland")) throw new IllegalArgumentException(name + " is outside Alfa boundary"); return value; }
}
