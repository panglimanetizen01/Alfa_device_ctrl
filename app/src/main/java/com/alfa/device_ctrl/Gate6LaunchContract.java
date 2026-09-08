package com.alfa.device_ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Converts one externally produced Gate 6 bootstrap artifact into the Android
 * launch contract consumed by InteractiveSessionContract. The input is only
 * accepted when Gate 6 is PASS and all provenance fields are present.
 */
public final class Gate6LaunchContract {
    public static final String INPUT_SCHEMA = "gate6-bootstrap.v1";
    public static final String OUTPUT_SCHEMA = "gate7-launch.v1";

    private Gate6LaunchContract() { }

    public static void importBootstrap(File bootstrap, File launchFile, String runtimeId) throws Exception {
        if (bootstrap == null || !bootstrap.isFile()) throw new IllegalArgumentException("Gate 6 bootstrap is missing");
        if (launchFile == null || launchFile.getParentFile() == null) throw new IllegalArgumentException("launch target is invalid");
        if (runtimeId == null || runtimeId.trim().isEmpty() || runtimeId.indexOf('\0') >= 0) throw new IllegalArgumentException("runtimeId is invalid");

        Properties input = new Properties();
        try (InputStream in = new FileInputStream(bootstrap)) { input.load(in); }
        require(input, "schema_version", INPUT_SCHEMA);
        require(input, "gate", "gate6");
        require(input, "gate_status", "PASS");
        String run = token(input, "pipeline_run_id");
        String source = hex(input, "source_commit", 40);
        String contract = hex(input, "gate4_contract_sha256", 64);
        String profile = hex(input, "profile_sha256", 64);
        String implementation = hex(input, "implementation_commit", 40);
        String decision = token(input, "decision_id");
        String request = token(input, "request_id");
        require(input, "authorization_status", "AUTHORIZED");
        token(input, "bootstrap_status");
        token(input, "bootstrap_probe");

        Properties output = new Properties();
        output.setProperty("schema_version", OUTPUT_SCHEMA);
        output.setProperty("gate", "gate7");
        output.setProperty("gate_status", "READY");
        output.setProperty("launch_status", "AUTHORIZED");
        output.setProperty("launch_source", "gate6-bootstrap");
        output.setProperty("pipeline_run_id", run);
        output.setProperty("runtime_id", runtimeId);
        output.setProperty("source_commit", source);
        output.setProperty("gate4_contract_sha256", contract);
        output.setProperty("profile_sha256", profile);
        output.setProperty("implementation_commit", implementation);
        output.setProperty("decision_id", decision);
        output.setProperty("request_id", request);
        output.setProperty("authorization_status", "AUTHORIZED");

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
            if (launchFile.exists() && !launchFile.delete()) throw new IllegalStateException("existing launch contract cannot be replaced");
            if (!temp.renameTo(launchFile)) throw new IllegalStateException("launch contract publish failed");
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
            token(p, "pipeline_run_id");
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
