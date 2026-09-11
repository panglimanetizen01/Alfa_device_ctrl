package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public final class UiSurfaces06To10ContractTest {
    private static final String MAIN = "src/main/java/com/alfa/device_ctrl/MainActivity.java";
    private static final String SHELL = "src/main/java/com/alfa/device_ctrl/AlfaUiShell.java";
    private static final String APP = "src/main/java/com/alfa/device_ctrl/AlfaApplication.java";
    private static final String STORAGE = "src/main/java/com/alfa/device_ctrl/StorageUiBridge.java";

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test public void securityAndDiagnosticSurfaceUsesRealExistingActions() throws Exception {
        String main = read(MAIN);
        String shell = read(SHELL);
        assertTrue(main.contains("showHtop()"));
        assertTrue(main.contains("showNetwork()"));
        assertTrue(main.contains("showPolicy()"));
        assertTrue(shell.contains("invoke(\"showHtop\")"));
        assertTrue(shell.contains("invoke(\"showNetwork\")"));
        assertTrue(shell.contains("invoke(\"showPolicy\")"));
        assertTrue(shell.contains("SPLIT — same real PTY, no fake second session"));
    }

    @Test public void storageSurfaceIsRealSafAndLifecycleBound() throws Exception {
        String app = read(APP);
        String storage = read(STORAGE);
        assertTrue(app.contains("StorageUiBridge.bind(a)"));
        assertTrue(app.contains("StorageUiBridge.refresh(a)"));
        assertTrue(storage.contains("StoragePickerActivity.class"));
        assertTrue(storage.contains("DocumentFile.fromTreeUri"));
        assertTrue(storage.contains("getSharedPreferences(\"alfa_storage\""));
    }

    @Test public void policyAndBackupRestoreRemainDelegatedNotMocked() throws Exception {
        String main = read(MAIN);
        assertTrue(main.contains("showPolicy()"));
        assertTrue(main.contains("installRuntime()"));
        assertTrue(main.contains("startSession()"));
        assertTrue(main.contains("stopSession()"));
        assertTrue(!main.contains("CPU: 42%"));
        assertTrue(!main.contains("MEM: 37%"));
    }
}
