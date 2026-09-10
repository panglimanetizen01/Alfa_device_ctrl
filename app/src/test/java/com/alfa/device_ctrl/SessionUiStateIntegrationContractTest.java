package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** Source contract proving the UI listener receives semantic state, not raw lifecycle events. */
public final class SessionUiStateIntegrationContractTest {
    @Test
    public void runtimeSessionManagerProjectsEventsThroughSessionUiState() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        assertTrue("RuntimeSessionManager source must exist", source.isFile());
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue("RuntimeSessionManager must use the canonical semantic projection", text.contains("SessionUiState.resolve(event).name()"));
        assertTrue("RuntimeSessionManager must not forward raw READY directly", !text.contains("listener.onState(\"READY\")"));
        assertTrue("RuntimeSessionManager must not forward raw PTY_CREATED directly", !text.contains("listener.onState(\"PTY_CREATED\")"));
        assertTrue("RuntimeSessionManager must not forward raw STOPPING directly", !text.contains("listener.onState(\"STOPPING\")"));
    }
}
