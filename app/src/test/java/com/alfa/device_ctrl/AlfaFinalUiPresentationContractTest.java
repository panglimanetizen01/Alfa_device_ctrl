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
        assertEquals(0xFF21262D, AlfaUiTheme.OBSIDIAN_SURFACE_ACTIVE);
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
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        int start = text.indexOf("private View terminalHeader() {");
        int end = text.indexOf("private View accessoryBar()", start);
        assertTrue("terminalHeader method must exist", start >= 0);
        assertTrue("terminalHeader method boundary must exist", end > start);
        String header = text.substring(start, end);
        assertFalse("terminal header must not render minimize action", header.contains("addHeaderAction(bar, \"—\""));
        assertFalse("terminal header must not render fullscreen action", header.contains("addHeaderAction(bar, \"↗\""));
        assertFalse("terminal header must not render split action", header.contains("addHeaderAction(bar, \"□\""));
        assertFalse("terminal header must not render kill action", header.contains("addHeaderAction(bar, \"×\""));
    }

    @Test public void nativePresentationExposesAllMajorStitchOperationalDomains() throws Exception {
        Path navigation = Paths.get("src/main/java/com/alfa/device_ctrl/AlfaUiNavigation.java");
        Path presentation = Paths.get("src/main/java/com/alfa/device_ctrl/AlfaFinalUiPresentation.java");
        Path panels = Paths.get("src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java");
        String nav = new String(Files.readAllBytes(navigation), StandardCharsets.UTF_8);
        String ui = new String(Files.readAllBytes(presentation), StandardCharsets.UTF_8);
        String panel = new String(Files.readAllBytes(panels), StandardCharsets.UTF_8);
        String[] domains = {"STORAGE", "SECURITY", "AUDIT", "PROJECT", "SESSIONS", "SPLIT", "FLOATING", "APPEARANCE", "SETTINGS", "NETWORK"};
        for (String domain : domains) {
            assertTrue("navigation missing Stitch domain " + domain, nav.contains(domain));
            assertTrue("presentation missing Stitch domain " + domain, ui.contains(domain));
            assertTrue("operational panel missing Stitch domain " + domain, panel.contains(domain));
        }
        assertFalse("Stitch shell must not render a fake placeholder state panel", ui.contains("State panel:"));
    }

    @Test public void finalShellRestylesTheNewlyBuiltNativeHierarchy() throws Exception {
        Path source = Paths.get("src/main/java/com/alfa/device_ctrl/AlfaFinalUiPresentation.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        int build = text.indexOf("content.addView(ui.build(), new FrameLayout.LayoutParams(-1, -1));");
        int theme = text.indexOf("AlfaUiTheme.apply(activity);", build);
        assertTrue("final shell must be added before its new hierarchy is themed", build >= 0);
        assertTrue("newly created Stitch hierarchy must be styled after insertion", theme > build);
    }

    @Test public void finalShellButtonsUseExplicitStitchControlBackground() throws Exception {
        Path source = Paths.get("src/main/java/com/alfa/device_ctrl/AlfaFinalUiPresentation.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        int start = text.indexOf("private Button button(String label, int color, View.OnClickListener listener)");
        int end = text.indexOf("private void addNav", start);
        assertTrue("native button factory must exist", start >= 0);
        assertTrue("native button factory boundary must exist", end > start);
        String factory = text.substring(start, end);
        assertTrue("final shell buttons must use the explicit Stitch control drawable", factory.contains("setBackground(controlBackground"));
    }

    @Test public void stitchReferenceCatalogIsBoundToTheSuppliedArtifact() {
        assertEquals("7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d", StitchV1ReferenceCatalog.ZIP_SHA256);
        assertEquals(159, StitchV1ReferenceCatalog.size());
    }
}
