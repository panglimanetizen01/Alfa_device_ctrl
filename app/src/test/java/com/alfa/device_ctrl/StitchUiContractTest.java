package com.alfa.device_ctrl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

/** Deterministic presentation contract for the Stitch v1 operational dashboard. */
public final class StitchUiContractTest {
    @Test public void operationalIdentityAndNavigationMatchStitch() {
        assertEquals("ALFA DEVICE CTRL", StitchUiContract.BRAND);
        assertEquals("Linux Runtime Control", StitchUiContract.SUBTITLE);
        assertEquals("SYSTEM ONLINE", StitchUiContract.SYSTEM_ONLINE);
        assertArrayEquals(new String[]{"TERMINAL", "RUNTIMES", "MONITOR", "SECURITY", "SETTINGS"}, StitchUiContract.NAV_LABELS);
    }

    @Test public void telemetryStripHasFourLiveSlots() {
        assertArrayEquals(new String[]{"CPU", "MEM", "STORAGE", "NET"}, StitchUiContract.TELEMETRY_LABELS);
    }

    @Test public void multiRuntimeContractContainsAllFourDistros() {
        assertEquals(4, RuntimeRegistry.all().size());
        assertEquals(StitchUiContract.DISTRO_IDS, java.util.Arrays.asList("debian", "ubuntu", "alpine", "kali"));
        for (String id : StitchUiContract.DISTRO_IDS) assertTrue(RuntimeRegistry.get(id) != null);
    }

    @Test public void runtimeAndTerminalActionsAreComplete() {
        assertEquals(java.util.Arrays.asList("CHECK", "OPEN", "INSTALL"), StitchUiContract.RUNTIME_ACTIONS);
        assertTrue(StitchUiContract.TERMINAL_ACTIONS.contains("RESTART SESSION"));
        assertTrue(StitchUiContract.TERMINAL_ACTIONS.contains("CLEAR"));
        assertTrue(StitchUiContract.TERMINAL_ACTIONS.contains("COMMAND"));
    }

    @Test public void nativeRendererContainsEveryRequiredOperationalAction() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/com/alfa/device_ctrl/StitchOperationalActivity.java")), StandardCharsets.UTF_8);
        for (String label : StitchUiContract.RUNTIME_ACTIONS) assertTrue("missing runtime action " + label, source.contains("\"" + label + "\""));
        assertTrue(source.contains("RESTART"));
        assertTrue(source.contains("CLEAR"));
        assertTrue(source.contains("RUN ▶"));
        assertTrue(source.contains("RUN RUNTIME TOOL PROBE"));
        assertTrue(source.contains("scrollTo(terminalAnchor)"));
        assertTrue(source.contains("scrollTo(runtimeAnchor)"));
        assertTrue(source.contains("refreshTelemetry()"));
    }
}
