package com.alfa.device_ctrl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Presentation contract for the Stitch v1 operational dashboard.
 * This test is intentionally written before the renderer implementation.
 */
public final class StitchUiContractTest {
    @Test
    public void operationalNavigationMatchesStitch() {
        assertArrayEquals(
                new String[]{"TERMINAL", "RUNTIMES", "MONITOR", "SECURITY", "SETTINGS"},
                StitchUiContract.NAV_LABELS);
    }

    @Test
    public void telemetryStripHasFourLiveSlots() {
        assertEquals(4, StitchUiContract.TELEMETRY_LABELS.length);
        assertArrayEquals(
                new String[]{"CPU", "MEM", "STORAGE", "NET"},
                StitchUiContract.TELEMETRY_LABELS);
    }

    @Test
    public void terminalSurfaceExposesOperationalActions() {
        assertTrue(StitchUiContract.TERMINAL_ACTIONS.contains("RESTART SESSION"));
        assertTrue(StitchUiContract.TERMINAL_ACTIONS.contains("CLEAR"));
        assertTrue(StitchUiContract.TERMINAL_ACTIONS.contains("COMMAND"));
    }
}
