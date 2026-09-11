package com.alfa.device_ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Converts one current Gate 6 bootstrap into a runtime-bound Gate 7 launch contract. */
public final class Gate6LaunchContract {
    public static final String INPUT_SCHEMA = "gate6-bootstrap.v1";
    public static final String OUTPUT_SCHEMA = "gate7-launch.v1";

    private Gate6LaunchContract() { }

    public static void importBootstrap(File bootstrap, File launchFile) throws Exception {
        if (bootstrap == null || !bootstrap.isFile()) throw new IllegalArgumentException("Gate 6 bootstrap is missing");
        if (launchFile == null || launchFile.getParentFile() == null) throw new IllegalArgumentException("launch target is invalid");

        Properties input = new Properties();
        try (InputStream in = new FileInputStream(bootstrap)) { input.load(in); }
        require(input, "schema_version", INPUT_SCHEMA);
        require(input, "gate", "gate6");
        require(input, "gate_status", "PASS");
        String runtimeId = token(input, "runtime_id");
        if (RuntimeRegistry.get(runtimeId) == null) throw new IllegalArgumentException("unsupported-runtime-id");
        String run = token(input, "pipeline_run_id");
        String source = hex(input, "source_commit", 40);
        String contract = hex(input, "gate4_contract_sha256", 64);
        String profile = hex(input, "profile_sha256", 64);
        String runtimeRegistry = hex(input, "runtime_registry_sha256", 64);
        String implementation = hex(input, "implementation_commit", 40);
        String decision = token(input, "decision_id");
        String request = token(input, "request_id");
        require(input, "authorization_status", "AUTHORIZED");
        require(input, "bootstrap_status", "PASS");
        require(input, "bootstrap_probe", "PASS");

        File parent = launchFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) throw new IllegalStateException("launch parent cannot be created");
        File temp = new File(parent, launchFile.getName() + ".partial");
        try (OutputStream out = new FileOutputStream(temp)) {
            StringBuilder text = new StringBuilder();
            text.append("schema_version=").append(OUTPUT_SCHEMA).append('\n');
            text.append("gate=gate7\n");
            text.append("gate_status=READY\n");
            text.append("launch_status=AUTHORIZED\n");
            text.append("launch_source=gate6-bootstrap\n");
            text.append("pipeline_run_id=").append(run).append('\n');
            text.append("runtime_id=").append(runtimeId).append('\n');
            text.append("runtime_registry_sha256=").append(runtimeRegistry).append('\n');
            text.append("source_commit=").append(source).append('\n');
            text.append("gate4_contract_sha256=").append(contract).append('\n');
            text.append("profile_sha256=").append(profile).append('\n');
            text.append("implementation_commit=").append(implementation).append('\n');
            text.append("decision_id=").append(decision).append('\n');
            text.append("request_id=").append(request).append('\n');
            text.append("authorization_status=AUTHORIZED\n");
            out.write(text.toString().getBytes(StandardCharsets.UTF_8));
        }
        if (!temp.renameTo(launchFile)) {
            temp.delete();
            throw new IllegalStateException("launch contract publish failed");
        }
        if (!verify(launchFile, runtimeId)) {
            launchFile.delete();
            throw new IllegalStateException("launch contract verification failed");
        }
    }

    public static boolean verify(File launchFile, String runtimeId) {
        if (launchFile == null || runtimeId == null || !launchFile.isFile()) return false;
        try {
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(launchFile)) { p.load(in); }
            require(p, "schema_version", OUTPUT_SCHEMA);
            require(p, "gate", "gate7");
            require(p, "gate_status", "READY");
            require(p, "launch_status", "AUTHORIZED");
            require(p, "launch_source", "gate6-bootstrap");
            require(p, "authorization_status", "AUTHORIZED");
            if (!runtimeId.equals(p.getProperty("runtime_id"))) return false;
            if (RuntimeRegistry.get(runtimeId) == null) return false;
            token(p, "pipeline_run_id");
            String registryHash = hex(p, "runtime_registry_sha256", 64);
            if (!RuntimeRegistry.CANONICAL_REGISTRY_SHA256.equalsIgnoreCase(registryHash)) return false;
            hex(p, "source_commit", 40);
            hex(p, "gate4_contract_sha256", 64);
            hex(p, "profile_sha256", 64);
            hex(p, "implementation_commit", 40);
            token(p, "decision_id");
            token(p, "request_id");
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    private static void require(Properties p, String key, String expected) {
        if (!expected.equals(p.getProperty(key))) throw new IllegalStateException("invalid " + key);
    }

    private static String token(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.trim().isEmpty() || value.indexOf('\0') >= 0) throw new IllegalStateException("missing " + key);
        return value.trim();
    }

    private static String hex(Properties p, String key, int length) {
        String value = token(p, key);
        if (!value.matches("[0-9a-fA-F]{" + length + "}")) throw new IllegalStateException("invalid " + key);
        return value.toLowerCase();
    }
}
