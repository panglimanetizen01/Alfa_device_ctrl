package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/**
 * Locks the boundary between the Android UI/session presentation and the external
 * phone-control execution lanes used by the Alfa workflow.
 */
public final class ExecutionLaneIsolationContractTest {
    @Test
    public void externalLanesAreExplicitAndIndependent() {
        assertTrue(ExecutionLane.values().length >= 4);
        assertTrue(ExecutionLane.TERMUX.isExternalToApp());
        assertTrue(ExecutionLane.TERMUX_API.isExternalToApp());
        assertTrue(ExecutionLane.SHIZUKU_RISH.isExternalToApp());
        assertFalse(ExecutionLane.RUNTIME.isExternalToApp());
    }

    @Test
    public void mainActivityDoesNotOwnExternalBridgeExecution() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/MainActivity.java");
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertFalse(text.contains("com.termux.api"));
        assertFalse(text.contains("rikka.shizuku"));
        assertFalse(text.contains("Shizuku"));
        assertFalse(text.contains("rish"));
    }

    @Test
    public void externalLaneFailureCannotDefineRuntimeReadiness() {
        assertTrue(ExecutionLanePolicy.runtimeReadinessDependsOnlyOnRuntimeEvidence());
        assertFalse(ExecutionLanePolicy.externalLaneFailureBlocksRuntimeUi());
    }
}
