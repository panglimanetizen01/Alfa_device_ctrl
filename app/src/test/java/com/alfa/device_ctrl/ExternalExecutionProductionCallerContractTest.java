package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** Locks that the active launcher has a real production caller for external lanes. */
public final class ExternalExecutionProductionCallerContractTest {
    @Test
    public void activeStitchActivityOwnsExternalCoordinator() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/StitchOperationalActivityV2.java");
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("ExternalExecutionCoordinator"));
        assertTrue(text.contains("executeShizukuProbe"));
        assertTrue(text.contains("executeTermuxProbe"));
        assertTrue(text.contains("executeTermuxApiProbe"));
    }
}
