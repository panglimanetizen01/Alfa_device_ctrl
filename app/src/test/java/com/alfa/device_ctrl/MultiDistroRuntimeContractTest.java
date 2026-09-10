package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

public final class MultiDistroRuntimeContractTest {
    @Test public void everyBuiltInProfileCarriesCompleteRuntimeMetadata() {
        assertEquals(4, RuntimeRegistry.all().size());
        for (RuntimeProfile profile : RuntimeRegistry.all()) {
            assertTrue(profile.version().length() > 0);
            assertEquals("aarch64", profile.architecture());
            assertTrue(profile.archiveFormat().equals("tar.gz") || profile.archiveFormat().equals("tar.xz"));
            assertTrue(profile.shell().startsWith("/"));
            assertTrue(profile.packageManager().length() > 0);
            assertTrue(profile.promptContract().contains(profile.id()));
            assertTrue(profile.environment().length > 0);
            assertTrue(profile.requiredPaths().length >= 5);
            assertTrue(profile.capabilities().length >= 5);
        }
    }

    @Test public void packageManagerMatchesKnownDistroFamilies() {
        assertEquals("apt", RuntimeRegistry.get("debian").packageManager());
        assertEquals("apt", RuntimeRegistry.get("ubuntu").packageManager());
        assertEquals("apk", RuntimeRegistry.get("alpine").packageManager());
        assertEquals("apt", RuntimeRegistry.get("kali").packageManager());
    }

    @Test public void KaliDeclaresRootlessKernelLimitation() {
        boolean found = false;
        for (String capability : RuntimeRegistry.get("kali").capabilities()) if ("kernel-features-limited".equals(capability)) found = true;
        assertTrue(found);
    }

    @Test public void canonicalRuntimeJsonMatchesRegistryIdentityAndArtifactMetadata() throws Exception {
        File file = new File("../runtime/runtimes.v1.json");
        if (!file.isFile()) file = new File("runtime/runtimes.v1.json");
        assertTrue("canonical runtime registry JSON is missing", file.isFile());
        String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        for (RuntimeProfile profile : RuntimeRegistry.all()) {
            assertTrue(json.contains("\"runtime_id\": \"" + profile.id() + "\""));
            assertTrue(json.contains("\"family\": \"" + profile.displayName() + "\""));
            assertTrue(json.contains("\"version\": \"" + profile.version() + "\""));
            assertTrue(json.contains("\"architecture\": \"" + profile.architecture() + "\""));
            assertTrue(json.contains("\"archive_format\": \"" + profile.archiveFormat() + "\""));
            assertTrue(json.contains("\"rootfs_uri\": \"" + profile.rootfsUrl() + "\""));
            assertTrue(json.contains("\"rootfs_sha256\": \"" + profile.rootfsSha256() + "\""));
            assertTrue(json.contains("\"shell_path\": \"" + profile.shell() + "\""));
        }
    }
}
