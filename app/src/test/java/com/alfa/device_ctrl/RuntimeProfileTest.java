package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RuntimeProfileTest {
    @Test public void firstAcceptanceRuntimeIsPinnedDebianArtifact() {
        assertEquals("debian", RuntimeProfile.ID);
        assertEquals("Debian", RuntimeProfile.DISPLAY_NAME);
        assertTrue(RuntimeProfile.ROOTFS_URL.contains("debuerreotype/docker-debian-artifacts"));
        assertTrue(RuntimeProfile.ROOTFS_URL.contains("14d91d295c23da6cc04d4bfe8b3d74a8a6c54e5c"));
        assertTrue(RuntimeProfile.ROOTFS_URL.endsWith("/bookworm/oci/blobs/rootfs.tar.gz"));
        assertEquals("c6cbf97176c58c741329cd787e932a1e47931b35f5dc0f23db3e6e82924fef0f", RuntimeProfile.ROOTFS_SHA256);
        assertTrue(RuntimeProfile.ROOTFS_GZIP);
    }
}
