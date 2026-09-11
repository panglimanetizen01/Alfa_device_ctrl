package com.alfa.device_ctrl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class RuntimeSessionRegistryTest {
    @Test public void addRemoveAndFirstAreDeterministic() {
        RuntimeSessionRegistry registry = new RuntimeSessionRegistry();
        RuntimeSessionManager a = null;
        RuntimeSessionManager b = null;
        try {
            a = manager("a");
            b = manager("b");
            registry.add(a);
            registry.add(b);
            assertEquals(2, registry.size());
            assertSame(a, registry.first());
            assertTrue(registry.remove(a));
            assertSame(b, registry.first());
            assertFalse(registry.remove(a));
            assertEquals(1, registry.size());
        } finally {
            if (a != null) a.stop();
            if (b != null) b.stop();
        }
    }

    private static RuntimeSessionManager manager(String id) {
        RuntimeProfile profile = RuntimeRegistry.get(RuntimeSelection.DEFAULT_RUNTIME_ID);
        java.io.File root = new java.io.File(System.getProperty("java.io.tmpdir"), "alfa-registry-test-" + id);
        java.io.File ready = new java.io.File(root, "READY.evidence");
        java.io.File engine = new java.io.File(root, "libproot.so");
        java.io.File cwd = new java.io.File(root, "cwd");
        root.mkdirs(); cwd.mkdirs();
        try {
            return new RuntimeSessionManager(new InteractiveSessionContract(
                    "test-" + id, "request-" + id, "pipeline-" + id, profile.id(),
                    ready, engine, root, cwd, new String[]{"PROOT_TMP_DIR=" + root.getAbsolutePath()}), null);
        } catch (RuntimeException expected) {
            return null;
        }
    }
}
