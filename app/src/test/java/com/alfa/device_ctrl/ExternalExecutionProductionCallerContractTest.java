package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** Locks the active production UI -> external capability -> transport path. */
public final class ExternalExecutionProductionCallerContractTest {
    @Test
    public void applicationInstallsExternalLaneUiHook() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/AlfaApplication.java");
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("ExternalExecutionUiHooks.apply(activity)"));
    }

    @Test
    public void uiHookSelectsConcreteExternalLanes() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/ExternalExecutionUiHooks.java");
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("executeShizukuProbe"));
        assertTrue(text.contains("executeTermuxProbe"));
        assertTrue(text.contains("executeTermuxApiProbe"));
        assertTrue(text.contains("ExternalExecutionCoordinator"));
    }
}
