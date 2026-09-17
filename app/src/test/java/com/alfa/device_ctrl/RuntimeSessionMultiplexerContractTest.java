package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/**
 * Red contract for the Stitch session-multiplexer gap.
 * This test is intentionally source-level until the production multiplexer exists;
 * it must never be weakened to accept the current single-PTY implementation.
 */
public final class RuntimeSessionMultiplexerContractTest {
    @Test public void multiplexerOwnsMultipleIndependentlyAddressableSessions() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/RuntimeSessionMultiplexer.java");
        assertTrue("real multi-session orchestrator must exist", source.isFile());
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("Map<String, RuntimeSessionManager>"));
        assertTrue(text.contains("createSession"));
        assertTrue(text.contains("removeSession"));
        assertTrue(text.contains("attachSession"));
        assertTrue(text.contains("stopAll"));
        assertTrue(text.contains("sessionId"));
        assertTrue(text.contains("RuntimeSessionManager"));
        assertTrue("session orchestration must not become a second runtime registry", !text.contains("Map<String, RuntimeProfile>"));
    }
}
