package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AlfaFinalUiPresentationContractTest {
    @Test public void finalPresentationUsesDominantStitchObsidianTokensAnd48dpTouchFloor() {
        assertEquals(0xFF101419, AlfaUiTheme.OBSIDIAN_CANVAS);
        assertEquals(0xFF161B22, AlfaUiTheme.OBSIDIAN_SURFACE);
        assertEquals(0xFF1C2229, AlfaUiTheme.OBSIDIAN_SURFACE_ACTIVE);
        assertEquals(0xFF10B981, AlfaUiTheme.OBSIDIAN_READY);
        assertEquals(0xFFF59E0B, AlfaUiTheme.OBSIDIAN_VERIFYING);
        assertEquals(0xFFEF4444, AlfaUiTheme.OBSIDIAN_ERROR);
        assertEquals(0xFF38BDF8, AlfaFinalUiPresentation.CYAN);
        assertEquals(0xFF0D1117, AlfaFinalUiPresentation.TERMINAL);
        assertEquals(48, AlfaUiTheme.TOUCH_TARGET_DP);
    }

    @Test public void terminalAppearanceRangesMatchFinalReference() {
        assertTrue(AlfaFinalUiPresentation.FONT_SIZES_SP.contains(11));
        assertTrue(AlfaFinalUiPresentation.FONT_SIZES_SP.contains(13));
        assertTrue(AlfaFinalUiPresentation.FONT_SIZES_SP.contains(15));
        assertTrue(AlfaFinalUiPresentation.LINE_HEIGHTS.contains(1.0f));
        assertTrue(AlfaFinalUiPresentation.LINE_HEIGHTS.contains(1.25f));
        assertTrue(AlfaFinalUiPresentation.LINE_HEIGHTS.contains(1.5f));
    }
}
