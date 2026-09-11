package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class RuntimeKeepAliveOwnershipContractTest {
    @Test public void serviceSourceUsesMultiOwnerLifecycle() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/RuntimeKeepAliveService.java");
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("Set<RuntimeSessionManager> owners"));
        assertTrue(text.contains("owners.contains(manager)"));
        assertTrue(!text.contains("RuntimeSessionManager owner;"));
        assertTrue(text.contains("empty = owners.isEmpty()"));
        assertTrue(text.contains("if (empty) context.stopService"));
        assertTrue(text.contains("current.size() == 1"));
    }
}
