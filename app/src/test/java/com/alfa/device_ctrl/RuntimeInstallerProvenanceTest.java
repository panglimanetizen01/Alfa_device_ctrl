package com.alfa.device_ctrl;

import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RuntimeInstallerProvenanceTest {
    @Test
    public void stringInstallApiCannotOverrideCanonicalRuntimeArtifact() throws Exception {
        File temp = Files.createTempDirectory("alfa-runtime-provenance-").toFile();
        RuntimeInstaller installer = new RuntimeInstaller(temp, null);
        RuntimeProfile profile = RuntimeRegistry.get("debian");
        assertTrue(profile != null);

        RuntimeInstaller.Result result = installer.install(
                profile.id(),
                null,
                RuntimeInstaller.TRUSTED_PROOT_ARM64_SHA256,
                new URL("https://example.invalid/arbitrary-rootfs.tar.gz"),
                profile.rootfsSha256(),
                profile.rootfsGzip());

        assertFalse("unregistered archive URL must be rejected before download", result.success);
        assertTrue(result.message.contains("runtime-artifact-does-not-match-registry"));
    }
}
