package com.alfa.device_ctrl;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public final class StitchOperationalSessionUiStateContractTest {
    @Test public void stitchOperationalActivityConsumesSemanticSessionState() throws Exception {
        Path source = Paths.get("src/main/java/com/alfa/device_ctrl/StitchOperationalActivity.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        assertTrue("StitchOperationalActivity must consume the semantic listener state", text.contains("onState(String "));
        assertTrue("RUNNING semantic state must be projected to the system status", text.contains("\"RUNNING\".equals(state)"));
        assertTrue("READY semantic state must be projected to the system status", text.contains("\"READY\".equals(state)"));
        assertTrue("Session state must be routed through the canonical handler", text.contains("handleSessionState(sessionId,"));
    }

    @Test public void canonicalSemanticProjectionRemainsFailClosed() {
        assertTrue(SessionUiState.resolve("READY") == SessionUiState.Status.RUNNING);
        assertTrue(SessionUiState.resolve("RUNNING") == SessionUiState.Status.RUNNING);
        assertTrue(SessionUiState.resolve("PTY_CREATED") == SessionUiState.Status.STARTING);
        assertTrue(SessionUiState.resolve("unknown-event") == SessionUiState.Status.NOT_READY);
    }
}
