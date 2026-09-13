package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public final class AlfaUiRuntimeWiringContractTest {
    @Test public void applicationLifecycleUsesCanonicalStitchShellNotRetiredPresenter() throws Exception {
        Path source = Paths.get("src/main/java/com/alfa/device_ctrl/AlfaApplication.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        assertFalse(text.contains("AlfaFinalUiPresentation.apply(activity)"));
        assertTrue(text.contains("AlfaFinalUiHooks.apply(activity)"));
        assertFalse(text.contains("StitchV1ReferenceConsole.installEntry(activity)"));
    }
}
