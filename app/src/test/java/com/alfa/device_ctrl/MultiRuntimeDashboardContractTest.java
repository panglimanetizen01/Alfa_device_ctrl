package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Regression contract for the DUT evidence: all canonical runtimes must remain reachable from the UI. */
public final class MultiRuntimeDashboardContractTest {
    @Test public void canonicalRegistryContainsFourSupportedRuntimes() {
        assertEquals(4, RuntimeRegistry.all().size());
        assertTrue(RuntimeRegistry.get("debian") != null);
        assertTrue(RuntimeRegistry.get("ubuntu") != null);
        assertTrue(RuntimeRegistry.get("alpine") != null);
        assertTrue(RuntimeRegistry.get("kali") != null);
    }

    @Test public void runtimeDashboardUsesScrollableContainer() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/com/alfa/device_ctrl/MainActivity.java")), StandardCharsets.UTF_8);
        assertTrue(source.contains("android.widget.ScrollView runtimeScroll"));
        assertTrue(source.contains("runtimeScroll.addView(runtimeDashboard"));
        assertTrue(source.contains("ALFA DEVICE CTRL"));
    }
}
