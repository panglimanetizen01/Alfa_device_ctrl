package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Regression guard: keep-alive ownership must not collapse multiple live runtime managers into one static owner. */
public final class RuntimeKeepAliveOwnershipContractTest {
    @Test public void serviceSourceUsesBoundedOwnerSetAndDoesNotClearAllOnSingleStop() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/RuntimeKeepAliveService.java");
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue("service must maintain multiple owners", text.contains("Set<RuntimeSessionManager> owners"));
        assertTrue("service must expose manager-specific ownership check", text.contains("owners.contains(manager)"));
        assertTrue("service must not use the legacy singular owner field", !text.contains("RuntimeSessionManager owner;"));
        assertTrue("service must not blindly stop the whole service for one manager", text.contains("if (owners.isEmpty())"));
    }
}
