package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    @Test public void terminalHeaderContainsNoLegacyActionButtons() throws Exception {
        Path source = Paths.get("src/main/java/com/alfa/device_ctrl/AlfaFinalUiPresentation.java");
        String text = Files.readString(source, StandardCharsets.UTF_8);
        int start = text.indexOf("private View terminalHeader() {");
        int end = text.indexOf("\n    private View accessoryBar()", start);
        assertTrue("terminalHeader method must exist", start >= 0);
        assertTrue("terminalHeader method boundary must exist", end > start);
        String header = text.substring(start, end);
        assertFalse("terminal header must not render minimize action", header.contains("addHeaderAction(bar, \"—\""));
        assertFalse("terminal header must not render fullscreen action", header.contains("addHeaderAction(bar, \"↗\""));
        assertFalse("terminal header must not render split action", header.contains("addHeaderAction(bar, \"□\""));
        assertFalse("terminal header must not render kill action", header.contains("addHeaderAction(bar, \"×\""));
    }
}
