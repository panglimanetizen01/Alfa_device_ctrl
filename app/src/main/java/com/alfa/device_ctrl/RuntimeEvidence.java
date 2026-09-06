package com.alfa.device_ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Properties;

/** Fail-closed verifier for runtime-ready.v1 evidence. */
public final class RuntimeEvidence {
    private RuntimeEvidence() { }

    public static boolean verify(File evidence, String runtimeId, File engine, File rootfs) {
        if (evidence == null || runtimeId == null || engine == null || rootfs == null || !evidence.isFile()) return false;
        try {
            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream(evidence)) { p.load(in); }
            if (!"runtime-ready.v1".equals(p.getProperty("schema_version"))) return false;
            if (!"READY".equals(p.getProperty("status"))) return false;
            if (!runtimeId.equals(p.getProperty("runtime_id"))) return false;
            if (!engine.getCanonicalPath().equals(p.getProperty("engine_path"))) return false;
            if (!rootfs.getCanonicalPath().equals(p.getProperty("rootfs_path"))) return false;
            if (!"ApplicationInfo.nativeLibraryDir".equals(p.getProperty("engine_source"))) return false;
            if (!"arm64-v8a".equals(p.getProperty("engine_abi"))) return false;
            if (!"libproot.so".equals(engine.getName())) return false;
            if (!engine.isFile() || !engine.canExecute() || !rootfs.isDirectory()) return false;
            String expected = p.getProperty("engine_sha256", "");
            return expected.length() == 64 && expected.equalsIgnoreCase(sha256(engine));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[65536];
        int count;
        try (FileInputStream in = new FileInputStream(file)) {
            while ((count = in.read(buffer)) > 0) digest.update(buffer, 0, count);
        }
        StringBuilder out = new StringBuilder(64);
        for (byte value : digest.digest()) out.append(String.format("%02x", value & 0xff));
        return out.toString();
    }
}
