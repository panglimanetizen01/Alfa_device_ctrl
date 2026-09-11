package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class RuntimeSessionLifecycleContractTest {
    @Test public void foregroundServiceOwnsPausePreservationAndReattachment() throws Exception {
        File service = new File("app/src/main/java/com/alfa/device_ctrl/RuntimeKeepAliveService.java");
        String serviceText = new String(Files.readAllBytes(service.toPath()), StandardCharsets.UTF_8);
        assertTrue(serviceText.contains("isActivityPauseInProgress"));
        assertTrue(serviceText.contains("RuntimeSessionReattachment.attach"));
        assertTrue(serviceText.contains("registerActivityLifecycleCallbacks"));
    }

    @Test public void sessionManagerExposesListenerRebindAndPauseGuard() throws Exception {
        File manager = new File("app/src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        String managerText = new String(Files.readAllBytes(manager.toPath()), StandardCharsets.UTF_8);
        assertTrue(managerText.contains("rebindListener"));
        assertTrue(managerText.contains("RuntimeKeepAliveService.isActivityPauseInProgress()"));
    }

    @Test public void reattachmentBridgeFindsTerminalAndRestoresManager() throws Exception {
        File bridge = new File("app/src/main/java/com/alfa/device_ctrl/RuntimeSessionReattachment.java");
        assertTrue(bridge.isFile());
        String text = new String(Files.readAllBytes(bridge.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("findTerminalView"));
        assertTrue(text.contains("manager.rebindListener"));
        assertTrue(text.contains("manager.attachTo"));
    }
}
