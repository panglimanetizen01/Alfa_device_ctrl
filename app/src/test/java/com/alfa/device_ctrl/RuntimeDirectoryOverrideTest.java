package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RuntimeDirectoryOverrideTest {
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

    @Test public void guestTargetRejectsTraversal() {
        assertTrue(rejects("/mnt/../proc"));
        assertTrue(rejects("relative/path"));
    }

    @Test public void hostPathLexicalBoundaryRejectsTraversalAndLookalikes() {
        assertTrue(RuntimeDirectoryOverride.isLexicallySharedStoragePath("/sdcard/project"));
        assertTrue(RuntimeDirectoryOverride.isLexicallySharedStoragePath("/storage/emulated/0/project"));
        assertTrue(!RuntimeDirectoryOverride.isLexicallySharedStoragePath("/sdcard/../data"));
        assertTrue(!RuntimeDirectoryOverride.isLexicallySharedStoragePath("/sdcardish/project"));
        assertTrue(!RuntimeDirectoryOverride.isLexicallySharedStoragePath("/storage/emulated/00/project"));
        assertTrue(!RuntimeDirectoryOverride.isLexicallySharedStoragePath("/data/local/tmp"));
    }

    private static boolean rejects(String path) {
        try { RuntimeDirectoryOverride.canonicalGuestPath(path); return false; }
        catch (IllegalArgumentException expected) { return true; }
    }
}
