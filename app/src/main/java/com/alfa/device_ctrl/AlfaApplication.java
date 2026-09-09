package com.alfa.device_ctrl;

import android.app.Application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

/** Copies the explicit build-time Gate 7 launch attestation into the private runtime vault. */
public final class AlfaApplication extends Application {
    private static final String ASSET = "gate7-launch.properties";

    @Override public void onCreate() {
        super.onCreate();
        installLaunchContract();
    }

    private void installLaunchContract() {
        File vault = new File(getFilesDir(), "runtime-vault");
        if (!vault.exists() && !vault.mkdirs()) return;
        File destination = new File(vault, ASSET);
        File temporary = new File(vault, ASSET + ".part");
        try (InputStream input = getAssets().open(ASSET);
             FileOutputStream output = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.getFD().sync();
            if (!temporary.renameTo(destination)) {
                if (destination.exists()) destination.delete();
                if (!temporary.renameTo(destination)) throw new IllegalStateException("launch-contract-publish-failed");
            }
            validate(destination);
        } catch (Exception ignored) {
            temporary.delete();
        }
    }

    private static void validate(File file) throws Exception {
        Properties p = new Properties();
        try (FileInputStream input = new FileInputStream(file)) { p.load(input); }
        require(p, "pipeline_run_id");
        require(p, "runtime_id");
        require(p, "source_commit");
        require(p, "gate4_contract_sha256");
        require(p, "profile_sha256");
        require(p, "implementation_commit");
        if (!RuntimeProfile.ID.equals(p.getProperty("runtime_id"))) throw new IllegalStateException("unsupported-runtime-id");
        if (!p.getProperty("source_commit").matches("[0-9a-fA-F]{40}")) throw new IllegalStateException("invalid-source-commit");
        if (!p.getProperty("implementation_commit").matches("[0-9a-fA-F]{40}")) throw new IllegalStateException("invalid-implementation-commit");
        if (!p.getProperty("gate4_contract_sha256").matches("[0-9a-fA-F]{64}")) throw new IllegalStateException("invalid-gate4-hash");
        if (!p.getProperty("profile_sha256").matches("[0-9a-fA-F]{64}")) throw new IllegalStateException("invalid-profile-hash");
    }

    private static void require(Properties p, String key) {
        if (p.getProperty(key) == null || p.getProperty(key).trim().isEmpty()) throw new IllegalStateException("missing-" + key);
    }
}
