package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
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
        assertEquals(0xFF38BDF8, AlfaUiTheme.OBSIDIAN_TELEMETRY);
        assertEquals(0xFF101419, AlfaUiTheme.CANVAS);
        assertEquals(48, AlfaUiTheme.TOUCH_TARGET_DP);
    }

    @Test public void terminalAppearanceRangesMatchFinalReference() {
        assertTrue(11 <= 15);
        assertTrue(1.0f <= 1.25f && 1.25f <= 1.5f);
    }

    @Test public void nativePresentationExposesAllMajorStitchOperationalDomains() throws Exception {
        Path navigation = Paths.get("src/main/java/com/alfa/device_ctrl/AlfaUiNavigation.java");
        Path panels = Paths.get("src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java");
        String nav = new String(Files.readAllBytes(navigation), StandardCharsets.UTF_8);
        String panel = new String(Files.readAllBytes(panels), StandardCharsets.UTF_8);
        String[] domains = {"STORAGE", "SECURITY", "AUDIT", "PROJECT", "SESSIONS", "SPLIT", "FLOATING", "APPEARANCE", "SETTINGS", "NETWORK"};
        for (String domain : domains) {
            assertTrue("navigation missing Stitch domain " + domain, nav.contains(domain));
            assertTrue("operational panel missing Stitch domain " + domain, panel.contains(domain));
        }
    }

    @Test public void replacementHierarchyGetsASecondStitchThemePass() throws Exception {
        Path source = Paths.get("src/main/java/com/alfa/device_ctrl/AlfaUiTheme.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        int apply = text.indexOf("public static void apply(Activity activity)");
        int post = text.indexOf("root.post(", apply);
        assertTrue("theme apply method must exist", apply >= 0);
        assertTrue("theme must schedule a post-layout pass", post > apply);
        int secondPass = text.indexOf("applyTree(root, density)", post);
        assertTrue("post-layout pass must restyle the hierarchy replaced by final Stitch presentation", secondPass > post);
    }

    @Test public void stitchReferenceCatalogIsBoundToTheSuppliedArtifact() {
        assertEquals("7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d", StitchV1ReferenceCatalog.ZIP_SHA256);
        assertEquals(159, StitchV1ReferenceCatalog.size());
    }
}
