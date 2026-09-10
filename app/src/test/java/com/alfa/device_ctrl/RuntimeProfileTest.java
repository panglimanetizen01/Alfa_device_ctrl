package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RuntimeProfileTest {
    @Test public void profileIsImmutableMetadata() {
        RuntimeProfile profile = RuntimeRegistry.get("debian");
        assertNotNull(profile);
        assertEquals("debian", profile.id());
        assertEquals("Debian", profile.displayName());
        assertEquals("aarch64", profile.architecture());
        assertTrue(profile.rootfsUrl().startsWith("https://"));
        assertTrue(profile.rootfsSha256().matches("[0-9a-f]{64}"));
        assertTrue(profile.rootfsGzip());
    }

    @Test public void KaliUsesXzArchiveMetadata() {
        RuntimeProfile profile = RuntimeRegistry.get("kali");
        assertNotNull(profile);
        assertEquals("Kali Linux", profile.displayName());
        assertFalse(profile.rootfsGzip());
        assertTrue(profile.rootfsUrl().endsWith(".tar.xz"));
    }
}