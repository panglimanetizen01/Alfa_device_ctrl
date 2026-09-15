package com.alfa.device_ctrl;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StitchOperationalSessionUiStateContractTest {
    @Test public void stitchOperationalActivityConsumesSemanticSessionStateNotRawRuntimeEvents() throws Exception {
        Path source = Paths.get("src/main/java/com/alfa/device_ctrl/StitchOperationalActivity.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        assertTrue("StitchOperationalActivity must consume the semantic listener state", text.contains("onState(String state)"));
        assertTrue("RUNNING semantic state must be projected to the system status", text.contains("\"RUNNING\".equals(state)"));
        assertTrue("READY semantic state must be projected to the system status", text.contains("\"READY\".equals(state)"));
        assertFalse("Activity must not parse raw PTY lifecycle events", text.contains("PTY_CREATED"));
        assertFalse("Activity must not parse raw prompt events", text.contains("PTY_WAITING_FOR_PROMPT"));
        assertFalse("Activity must not parse raw process events", text.contains("BACKGROUND_SESSION_PRESERVED"));
        assertFalse("Activity must not retain the obsolete SESSION_STATUS implementation detail", text.contains("SESSION_STATUS=\" + state"));
    }

    @Test public void canonicalSemanticProjectionRemainsFailClosed() {
        assertTrue(SessionUiState.resolve("READY") == SessionUiState.Status.RUNNING);
        assertTrue(SessionUiState.resolve("RUNNING") == SessionUiState.Status.RUNNING);
        assertTrue(SessionUiState.resolve("PTY_CREATED") == SessionUiState.Status.STARTING);
        assertTrue(SessionUiState.resolve("unknown-event") == SessionUiState.Status.NOT_READY);
    }
}
