package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** Locks probe markers, lane separation, and real Termux:API operation selection. */
public final class ExternalExecutionCoordinatorContractTest {
    @Test
    public void probesHaveUniqueMarkersAndConcreteCommands() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/external/ExternalExecutionCoordinator.java");
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("ALFA_SHIZUKU_PROBE_"));
        assertTrue(text.contains("ALFA_TERMUX_PROBE_"));
        assertTrue(text.contains("ALFA_TERMUX_API_PROBE_"));
        assertTrue(text.contains("termux-battery-status"));
        assertTrue(text.contains("ExternalExecutionResult"));
    }

    @Test
    public void apiProbeKeepsTransportAndCapabilityDistinct() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/external/ExternalExecutionCoordinator.java");
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("TERMUX_API, bridge"));
        assertTrue(text.contains("RUN_COMMAND transport"));
        assertTrue(text.contains("Termux:API device capability"));
    }
}
