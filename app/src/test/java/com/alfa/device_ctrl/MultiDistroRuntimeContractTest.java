package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
}
