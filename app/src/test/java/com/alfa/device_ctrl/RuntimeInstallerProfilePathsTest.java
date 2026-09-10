package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;

import org.junit.Test;

public final class RuntimeInstallerProfilePathsTest {
    @Test public void rootfsValidationUsesSelectedProfileRequiredPaths() throws Exception {
        File root = Files.createTempDirectory("alfa-profile-rootfs-").toFile();
        File custom = new File(root, "opt/alfa/runtime-marker");
        assertTrue(custom.getParentFile().mkdirs());
        assertTrue(custom.createNewFile());

        RuntimeProfile profile = new RuntimeProfile(
                "test-runtime", "Test Runtime", "1", "aarch64", "https://example.invalid/rootfs.tar.gz",
                "0123456789012345678901234567890123456789012345678901234567890123", true,
                "tar.gz", "/bin/sh", "test", "alfa:test:",
                new String[]{"HOME=/root"},
                new String[]{"opt/alfa/runtime-marker"},
                new String[]{"rootless-proot"});

        RuntimeInstaller installer = new RuntimeInstaller(root, null);
        Method method = RuntimeInstaller.class.getDeclaredMethod("validateRootfs", File.class, RuntimeProfile.class);
        method.setAccessible(true);
        try {
            method.invoke(installer, root, profile);
        } catch (InvocationTargetException error) {
            throw new AssertionError(error.getCause());
        }
    }
}
