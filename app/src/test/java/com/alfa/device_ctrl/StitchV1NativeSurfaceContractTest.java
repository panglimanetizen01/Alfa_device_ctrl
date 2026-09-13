package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public final class StitchV1NativeSurfaceContractTest {
    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test public void operationalPanelsContainStructuredStitchControls() throws Exception {
        String source = read("src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java");
        String[] required = {
                "card(", "chip(", "rowButton(", "sectionTitle(",
                "SESSION MULTIPLEXER", "SPAWN +", "STOP SESSION", "SWITCH SESSION",
                "APPEARANCE PRESETS", "TYPOGRAPHY", "FONT SIZE", "LINE HEIGHT",
                "SPLIT VIEW", "HORIZONTAL", "VERTICAL", "SYNC INPUT",
                "FLOATING TERMINAL", "OVERLAY PERMISSION", "STORAGE POLICY",
                "PROJECT EXPLORER", "DIAGNOSTIC ENGINE"
        };
        for (String token : required) assertTrue("missing native Stitch control: " + token, source.contains(token));
    }

    @Test public void stitchSurfaceKeepsCapabilityGatesExplicit() throws Exception {
        String source = read("src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java");
        assertTrue(source.contains("NOT_EXPOSED_BY_CANONICAL_MANAGER"));
        assertTrue(source.contains("interactive_overlay=NOT_STARTED_BY_THIS_PANEL"));
        assertTrue(source.contains("SAF_REQUIRED"));
        assertTrue(source.contains("NOT_CLAIMED_FROM_APP_SANDBOX"));
    }

    @Test public void stitchSurfaceUsesLockedGeometryAndSemanticTokens() throws Exception {
        String source = read("src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java");
        assertTrue(source.contains("AlfaUiTheme.TOUCH_TARGET_DP"));
        assertTrue(source.contains("AlfaUiTheme.SURFACE_1"));
        assertTrue(source.contains("AlfaUiTheme.SURFACE_2"));
        assertTrue(source.contains("AlfaUiTheme.READY"));
        assertTrue(source.contains("AlfaUiTheme.WARNING"));
        assertTrue(source.contains("AlfaUiTheme.ERROR"));
    }
}
