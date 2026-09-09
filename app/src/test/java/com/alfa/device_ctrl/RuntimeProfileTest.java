package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RuntimeProfileTest {
    @Test public void firstAcceptanceRuntimeIsPinnedDebianArtifact() {
        assertEquals("debian", RuntimeProfile.ID);
        assertEquals("Debian", RuntimeProfile.DISPLAY_NAME);
        assertTrue(RuntimeProfile.ROOTFS_URL.contains("debuerreotype/docker-debian-artifacts"));
        assertTrue(RuntimeProfile.ROOTFS_URL.contains("fb7215b47dab72bdbdd59204a7b7914311431d90"));
        assertTrue(RuntimeProfile.ROOTFS_URL.endsWith("/bookworm/rootfs.tar.xz"));
        assertEquals("202ecca447dbf1b3ac1b1e983d9363381ac6a34f8e22d7d786125d06754ebb76", RuntimeProfile.ROOTFS_SHA256);
        assertFalse(RuntimeProfile.ROOTFS_GZIP);
    }
}
