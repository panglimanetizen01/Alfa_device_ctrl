package com.alfa.device_ctrl;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;

import static org.junit.Assert.assertEquals;

public final class RuntimeUiStateTest {
    @Test public void missingRuntimeIsNotInstalled() {
        File root = new File(System.getProperty("java.io.tmpdir"), "alfa-ui-missing-" + System.nanoTime());
        assertEquals(RuntimeUiState.Status.NOT_INSTALLED,
                RuntimeUiState.resolve("debian", root, new File(root, "READY.evidence"), new File(root, "libproot.so"), new File(root, "rootfs")));
    }

    @Test public void rootfsWithoutEvidenceIsInstalled() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "alfa-ui-installed-" + System.nanoTime());
        assertEquals(true, root.mkdirs());
        File rootfs = new File(root, "rootfs");
        assertEquals(true, rootfs.mkdirs());
        assertEquals(RuntimeUiState.Status.INSTALLED,
                RuntimeUiState.resolve("debian", root, new File(root, "READY.evidence"), new File(root, "libproot.so"), rootfs));
    }

    @Test public void invalidEvidenceFailsClosed() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "alfa-ui-invalid-" + System.nanoTime());
        assertEquals(true, root.mkdirs());
        File evidence = new File(root, "READY.evidence");
        try (FileWriter writer = new FileWriter(evidence)) { writer.write("pending=true\n"); }
        assertEquals(RuntimeUiState.Status.FAILED,
                RuntimeUiState.resolve("debian", root, evidence, new File(root, "libproot.so"), new File(root, "rootfs")));
    }
}
