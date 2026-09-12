package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AlfaUiForensicDesignContractTest {
    @Test public void canonicalObsidianTokensMatchApprovedDesign() {
        assertEquals(0xFF0B0F14, AlfaUiTheme.OBSIDIAN_CANVAS);
        assertEquals(0xFF121820, AlfaUiTheme.OBSIDIAN_SURFACE);
        assertEquals(0xFF1A222D, AlfaUiTheme.OBSIDIAN_SURFACE_ACTIVE);
        assertEquals(0xFF1F2937, AlfaUiTheme.OBSIDIAN_BORDER);
        assertEquals(0xFF10B981, AlfaUiTheme.OBSIDIAN_READY);
        assertEquals(0xFFF59E0B, AlfaUiTheme.OBSIDIAN_VERIFYING);
        assertEquals(0xFFEF4444, AlfaUiTheme.OBSIDIAN_ERROR);
        assertEquals(0xFF38BDF8, AlfaUiTheme.OBSIDIAN_TELEMETRY);
        assertEquals(0xFF64748B, AlfaUiTheme.OBSIDIAN_UNKNOWN);
    }

    @Test public void runtimeStatusMappingIsSemanticNotDecorative() {
        assertEquals(AlfaUiTheme.OBSIDIAN_READY, AlfaUiTheme.statusColor("READY"));
        assertEquals(AlfaUiTheme.OBSIDIAN_VERIFYING, AlfaUiTheme.statusColor("VERIFYING"));
        assertEquals(AlfaUiTheme.OBSIDIAN_ERROR, AlfaUiTheme.statusColor("FAILED"));
        assertEquals(AlfaUiTheme.OBSIDIAN_TELEMETRY, AlfaUiTheme.statusColor("TELEMETRY"));
        assertEquals(AlfaUiTheme.OBSIDIAN_UNKNOWN, AlfaUiTheme.statusColor("UNKNOWN"));
        assertFalse(AlfaUiTheme.isLegacyPalette(AlfaUiTheme.OBSIDIAN_READY));
        assertTrue(AlfaUiTheme.isLegacyPalette(0xFFABC7FF));
    }

    @Test public void touchTargetFloorRemains48dp() {
        assertEquals(48, AlfaUiTheme.TOUCH_TARGET_DP);
    }
}
