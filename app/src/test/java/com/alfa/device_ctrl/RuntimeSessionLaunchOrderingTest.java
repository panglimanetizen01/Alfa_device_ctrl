package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Regression contract: the keep-alive owner must be established before the PTY is created. */
public final class RuntimeSessionLaunchOrderingTest {
    @Test public void foregroundServiceStartsBeforeTerminalSessionCreation() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        int service = text.indexOf("RuntimeKeepAliveService.start(AlfaApplication.getInstance(), this)");
        int terminal = text.indexOf("new TerminalSession(");
        assertTrue("RuntimeSessionManager source missing FGS start", service >= 0);
        assertTrue("RuntimeSessionManager source missing TerminalSession creation", terminal >= 0);
        assertTrue("FGS ownership must be established before PTY creation", service < terminal);
    }
}
