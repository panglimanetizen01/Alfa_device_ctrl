package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

/** Regression contract for the canonical runtime UI presentation. */
public final class MultiRuntimeDashboardContractTest {
    @Test public void canonicalRegistryContainsFourSupportedRuntimes() {
        assertEquals(4, RuntimeRegistry.all().size());
        assertTrue(RuntimeRegistry.get("debian") != null);
        assertTrue(RuntimeRegistry.get("ubuntu") != null);
        assertTrue(RuntimeRegistry.get("alpine") != null);
        assertTrue(RuntimeRegistry.get("kali") != null);
    }

    @Test public void runtimeDashboardUsesScrollableContainer() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/com/alfa/device_ctrl/AlfaFinalUiPresentation.java")), StandardCharsets.UTF_8);
        assertTrue(source.contains("ScrollView scroll"));
        assertTrue(source.contains("scroll.addView(list"));
        assertTrue(source.contains("LINUX RUNTIMES"));
    }
}
