package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class ForegroundSessionOwnershipContractTest {
    @Test public void serviceAndSessionManagerContainExplicitBackgroundOwnershipContract() throws Exception {
        File service = new File("src/main/java/com/alfa/device_ctrl/RuntimeKeepAliveService.java");
        if (!service.isFile()) service = new File("app/src/main/java/com/alfa/device_ctrl/RuntimeKeepAliveService.java");
        File manager = new File("src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        if (!manager.isFile()) manager = new File("app/src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        String serviceText = new String(Files.readAllBytes(service.toPath()), StandardCharsets.UTF_8);
        String managerText = new String(Files.readAllBytes(manager.toPath()), StandardCharsets.UTF_8);
        assertTrue(serviceText.contains("FOREGROUND_SERVICE_TYPE_SPECIAL_USE"));
        assertTrue(serviceText.contains("startForegroundService"));
        assertTrue(serviceText.contains("STOP_RUNTIME_KEEPALIVE"));
        assertTrue(managerText.contains("RuntimeKeepAliveService.start"));
        assertTrue(managerText.contains("BACKGROUND_SESSION_PRESERVED"));
        assertTrue(managerText.contains("RuntimeKeepAliveService.stop"));
    }
}
