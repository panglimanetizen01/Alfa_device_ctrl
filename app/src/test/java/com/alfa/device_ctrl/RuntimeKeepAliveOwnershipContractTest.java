package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

public final class RuntimeKeepAliveOwnershipContractTest {
    @Test public void serviceSourceUsesMultiOwnerLifecycle() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/RuntimeKeepAliveService.java");
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("Set<RuntimeSessionManager> owners"));
        assertTrue(text.contains("owners.contains(manager)"));
        assertTrue(!text.contains("RuntimeSessionManager owner;"));
        assertTrue(Pattern.compile("empty\\s*=\\s*owners\\.isEmpty\\(\\)").matcher(text).find());
        assertTrue(Pattern.compile("if\\s*\\(\\s*empty\\s*\\)\\s*context\\.stopService").matcher(text).find());
        assertTrue(Pattern.compile("current\\.size\\(\\)\\s*==\\s*1").matcher(text).find());
    }
}
