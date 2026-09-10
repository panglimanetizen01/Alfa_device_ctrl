package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class UiSessionOwnershipSemanticsTest {
    @Test public void activityDoesNotStopBackgroundSessionDuringLifecycleTeardown() throws Exception {
        File activity = new File("src/main/java/com/alfa/device_ctrl/MainActivity.java");
        if (!activity.isFile()) activity = new File("app/src/main/java/com/alfa/device_ctrl/MainActivity.java");
        String text = new String(Files.readAllBytes(activity.toPath()), StandardCharsets.UTF_8);
        assertFalse("onStop must not terminate the foreground-owned runtime session", text.contains("@Override protected void onStop() {\n        uiActive = false;\n        if (sessionManager != null && sessionManager.isRunning()) sessionManager.stop();"));
        assertFalse("onDestroy must not terminate the foreground-owned runtime session", text.contains("@Override protected void onDestroy() {\n        uiActive = false;\n        if (sessionManager != null && sessionManager.isRunning()) sessionManager.stop();"));
        assertTrue("Activity must support rebinding to the foreground-owned session", text.contains("RuntimeKeepAliveService.owner()"));
    }

    @Test public void sessionManagerExposesExplicitListenerRebind() throws Exception {
        File manager = new File("src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        if (!manager.isFile()) manager = new File("app/src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        String text = new String(Files.readAllBytes(manager.toPath()), StandardCharsets.UTF_8);
        assertTrue("session manager must expose a lifecycle-safe listener rebind", text.contains("rebindListener"));
    }
}
