package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RuntimeDirectoryOverrideTest {
    @Test public void serializationRoundTripPreservesBindRule() {
        RuntimeDirectoryOverride value = new RuntimeDirectoryOverride("debian", "/sdcard/Alfa_device_ctrl_HOST", "/mnt/alfa-shared");
        RuntimeDirectoryOverride parsed = RuntimeDirectoryOverride.parse(value.serialize());
        assertEquals("debian", parsed.runtimeId());
        assertEquals("/sdcard/Alfa_device_ctrl_HOST", parsed.hostPath());
        assertEquals("/mnt/alfa-shared", parsed.guestPath());
        assertEquals("/sdcard/Alfa_device_ctrl_HOST:/mnt/alfa-shared", parsed.prootBindArgument());
    }

    @Test public void guestTargetRejectsRootAndSensitiveNamespaces() {
        assertTrue(rejects("/"));
        assertTrue(rejects("/proc"));
        assertTrue(rejects("/sys"));
        assertTrue(rejects("/dev"));
    }

    @Test public void guestTargetAllowsExplicitMountNamespaces() {
        assertEquals("/mnt/work", RuntimeDirectoryOverride.canonicalGuestPath("/mnt/work/"));
        assertEquals("/workspace/project", RuntimeDirectoryOverride.canonicalGuestPath("/workspace/project"));
    }

    private static boolean rejects(String path) {
        try { RuntimeDirectoryOverride.canonicalGuestPath(path); return false; }
        catch (IllegalArgumentException expected) { return true; }
    }
}
