package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

public final class InteractiveSessionContractTest {
    @Test
    public void directoryOverrideBindsAreBeforeCommand() throws IOException {
        File runtimeRoot = Files.createTempDirectory("alfa-runtime-root").toFile();
        try {
            RuntimeProfile profile = RuntimeRegistry.get("debian");
            String[] args = InteractiveSessionContract.resolveProotArguments(profile, runtimeRoot, new String[]{"/sdcard/Alfa_device_ctrl_HOST:/mnt/alfa"});
            int bind = indexOf(args, "-b");
            int command = indexOf(args, "/usr/bin/env");
            assertTrue("PRoot bind option must exist", bind >= 0);
            assertTrue("PRoot command must exist", command >= 0);
            assertEquals("bind must precede the command", true, bind < command);
            assertEquals("/sdcard/Alfa_device_ctrl_HOST:/mnt/alfa", args[bind + 1]);
        } finally { Files.deleteIfExists(runtimeRoot.toPath()); }
    }

    @Test
    public void prootLoaderValidationDoesNotTrustEnvironmentPath() throws IOException {
        File runtimeDir = Files.createTempDirectory("alfa-loader-root").toFile();
        try {
            File proot = new File(runtimeDir, "proot");
            File loader = new File(runtimeDir, "libproot-loader.so");
            assertTrue(proot.createNewFile());
            assertTrue(loader.createNewFile());
            assertTrue(loader.setExecutable(true, false));
            assertFalse("environment-controlled loader path must be rejected", InteractiveSessionContract.isCanonicalProotLoaderValue("/tmp/attacker/libproot-loader.so", proot));
            assertTrue("canonical loader sibling must be accepted", InteractiveSessionContract.isCanonicalProotLoaderValue(loader.getCanonicalPath(), proot));
        } finally {
            Files.deleteIfExists(new File(runtimeDir, "libproot-loader.so").toPath());
            Files.deleteIfExists(new File(runtimeDir, "proot").toPath());
            Files.deleteIfExists(runtimeDir.toPath());
        }
    }

    @Test
    public void prootTmpDirectoryMustResolveInsideRuntimeVault() throws IOException {
        File vault = Files.createTempDirectory("alfa-proot-vault").toFile();
        File runtimeRoot = new File(vault, "runtime");
        File tmp = new File(vault, "tmp");
        File rootfs = new File(runtimeRoot, "rootfs");
        File attacker = Files.createTempDirectory("alfa-attacker").toFile();
        try {
            assertTrue(runtimeRoot.mkdirs());
            assertTrue(tmp.mkdirs());
            assertTrue(rootfs.mkdirs());
            assertTrue(tmp.setWritable(true, false));
            assertTrue(tmp.setExecutable(true, false));
            assertTrue("vault temp directory must be accepted", InteractiveSessionContract.isCanonicalProotTmpDirValue(tmp.getPath(), runtimeRoot));
            assertFalse("outside-vault temp directory must be rejected", InteractiveSessionContract.isCanonicalProotTmpDirValue(attacker.getPath(), runtimeRoot));
            assertFalse("runtime rootfs must be rejected", InteractiveSessionContract.isCanonicalProotTmpDirValue(rootfs.getPath(), runtimeRoot));
        } finally {
            Files.deleteIfExists(rootfs.toPath());
            Files.deleteIfExists(tmp.toPath());
            Files.deleteIfExists(runtimeRoot.toPath());
            Files.deleteIfExists(vault.toPath());
            Files.deleteIfExists(attacker.toPath());
        }
    }

    private static int indexOf(String[] values, String target) {
        for (int i = 0; i < values.length; i++) if (target.equals(values[i])) return i;
        return -1;
    }
}
