package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class AlfaUiThemeSemanticContractTest {
    @Test public void themeDoesNotOverwriteSemanticContainerBackgrounds() throws Exception {
        File theme = new File("src/main/java/com/alfa/device_ctrl/AlfaUiTheme.java");
        if (!theme.isFile()) theme = new File("app/src/main/java/com/alfa/device_ctrl/AlfaUiTheme.java");
        String text = new String(Files.readAllBytes(theme.toPath()), StandardCharsets.UTF_8);
        assertFalse("theme traversal must not overwrite direct child backgrounds", text.contains("child.setBackgroundColor(SURFACE_1)"));
        assertTrue("Terminal Obsidian surface tokens remain the presentation source", text.contains("public static final int SURFACE_1"));
    }
}
