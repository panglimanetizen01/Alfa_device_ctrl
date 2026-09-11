package com.alfa.device_ctrl;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MainActivitySessionUiStateContractTest {
    @Test public void mainActivityConsumesSemanticSessionStateNotRawRuntimeEvents() throws Exception {
        Path source = locateMainActivity();
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        assertTrue("MainActivity must expose the semantic state to its status view", text.contains("SESSION_STATUS=\" + state"));
        assertTrue("RUNNING semantic state must hide transient status", text.contains("\"RUNNING\".equals(state)"));
        assertTrue("READY semantic state must hide transient status", text.contains("\"READY\".equals(state)"));
        assertFalse("MainActivity must not parse raw PTY lifecycle events", text.contains("PTY_CREATED"));
        assertFalse("MainActivity must not parse raw prompt events", text.contains("PTY_WAITING_FOR_PROMPT"));
        assertFalse("MainActivity must not parse raw process events", text.contains("BACKGROUND_SESSION_PRESERVED"));
    }

    @Test public void canonicalSemanticProjectionRemainsFailClosed() {
        assertTrue(SessionUiState.resolve("READY") == SessionUiState.Status.RUNNING);
        assertTrue(SessionUiState.resolve("RUNNING") == SessionUiState.Status.RUNNING);
        assertTrue(SessionUiState.resolve("PTY_CREATED") == SessionUiState.Status.STARTING);
        assertTrue(SessionUiState.resolve("unknown-event") == SessionUiState.Status.NOT_READY);
    }

    private static Path locateMainActivity() {
        Path[] candidates = {
                Paths.get("app/src/main/java/com/alfa/device_ctrl/MainActivity.java"),
                Paths.get("src/main/java/com/alfa/device_ctrl/MainActivity.java")
        };
        for (Path candidate : candidates) if (Files.isRegularFile(candidate)) return candidate;
        throw new AssertionError("MainActivity.java not found");
    }
}
