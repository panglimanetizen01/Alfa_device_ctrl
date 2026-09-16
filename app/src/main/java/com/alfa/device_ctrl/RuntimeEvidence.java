package com.alfa.device_ctrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Properties;

/** Fail-closed verifier for runtime-ready.v1 evidence. */
public final class RuntimeEvidence {
    public static final String TRUSTED_PROOT_ARM64_SHA256 = "c902f35b3bce4013d2e78e3bf360b606523d55ab7b907578938577b243bfca38";
    public static final String TRUSTED_PROOT_LOADER_ARM64_SHA256 = "1e0341759bb0776dbfe6afbad7dbd51b0eb3fc38d7ff1db321b8c036e1617e33";
    /** Empty until an x86_64 loader artifact is actually built and its SHA is reviewed. */
    public static final String TRUSTED_PROOT_LOADER_CI_X86_64_SHA256 = "";

    private RuntimeEvidence() { }

    public static boolean isTrustedProotSha256ForAbi(String abi, String sha256) {
        if (RuntimeAbi.ARM64_V8A.equals(abi)) {
            return TRUSTED_PROOT_ARM64_SHA256.equalsIgnoreCase(sha256);
        }
        // x86_64 remains fail-closed until the exact built artifact SHA is observed and reviewed.
        return false;
    }

    public static boolean isTrustedProotLoaderSha256ForAbi(String abi, String sha256) {
        if (RuntimeAbi.ARM64_V8A.equals(abi)) {
            return TRUSTED_PROOT_LOADER_ARM64_SHA256.equalsIgnoreCase(sha256);
        }
        if (RuntimeAbi.X86_64.equals(abi)) {
            return !TRUSTED_PROOT_LOADER_CI_X86_64_SHA256.isEmpty()
                    && TRUSTED_PROOT_LOADER_CI_X86_64_SHA256.equalsIgnoreCase(sha256);
        }
        return false;
    }

    public static boolean isTrustedProotLoaderSha256(String sha256) {
        return isTrustedProotLoaderSha256ForAbi(RuntimeAbi.ARM64_V8A, sha256)
                || isTrustedProotLoaderSha256ForAbi(RuntimeAbi.X86_64, sha256);
    }

    public static String expectedEngineMachine(String abi) {
        if (RuntimeAbi.ARM64_V8A.equals(abi)) return "AArch64";
        if (RuntimeAbi.X86_64.equals(abi)) return "Advanced Micro Devices X86-64";
        throw new IllegalArgumentException("unsupported-native-abi:" + abi);
    }

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
            String abi = p.getProperty("engine_abi", "").trim().toLowerCase(Locale.ROOT);
            if (!RuntimeAbi.ARM64_V8A.equals(abi) && !RuntimeAbi.X86_64.equals(abi)) return false;
            if (!"libproot.so".equals(engine.getName())) return false;
            if (!engine.isFile() || !engine.canExecute() || !rootfs.isDirectory()) return false;
            if (!isExpectedElf(engine, abi)) return false;
            File loader = new File(engine.getParentFile(), "libproot-loader.so").getCanonicalFile();
            if (!loader.isFile() || !loader.canExecute() || !isExpectedElf(loader, abi)) return false;
            String loaderSha = sha256(loader);
            if (!isTrustedProotLoaderSha256ForAbi(abi, loaderSha)) return false;
            String actualEngineSha = sha256(engine);
            if (!isTrustedProotSha256ForAbi(abi, actualEngineSha)) return false;
            String expected = p.getProperty("engine_sha256", "");
            return expected.length() == 64 && expected.equalsIgnoreCase(actualEngineSha);
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean isExpectedElf(File file, String abi) throws IOException {
        byte[] header = new byte[20];
        try (FileInputStream input = new FileInputStream(file)) {
            int offset = 0;
            while (offset < header.length) {
                int count = input.read(header, offset, header.length - offset);
                if (count < 0) break;
                offset += count;
            }
            if (offset < 20
                    || (header[0] & 0xff) != 0x7f
                    || header[1] != 'E' || header[2] != 'L' || header[3] != 'F'
                    || (header[4] & 0xff) != 2
                    || (header[5] & 0xff) != 1) return false;
            int machine = (header[18] & 0xff) | ((header[19] & 0xff) << 8);
            if (RuntimeAbi.ARM64_V8A.equals(abi)) return machine == 183;
            if (RuntimeAbi.X86_64.equals(abi)) return machine == 62;
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