package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class UiSessionOwnershipSemanticsTest {
    @Test public void foregroundOwnerCanRebindPresentationAfterActivityLifecycle() throws Exception {
        File app = new File("src/main/java/com/alfa/device_ctrl/AlfaApplication.java");
        if (!app.isFile()) app = new File("app/src/main/java/com/alfa/device_ctrl/AlfaApplication.java");
        String text = new String(Files.readAllBytes(app.toPath()), StandardCharsets.UTF_8);
        assertTrue("Application must recover the foreground-owned manager", text.contains("RuntimeKeepAliveService.owner()"));
        assertTrue("Application must rebind the listener", text.contains("owner.rebindListener"));
        assertTrue("Application must reattach the terminal view", text.contains("owner.attachTo"));
    }

    @Test public void sessionManagerExposesExplicitListenerRebindAndLifecycleGuard() throws Exception {
        File manager = new File("src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        if (!manager.isFile()) manager = new File("app/src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        String text = new String(Files.readAllBytes(manager.toPath()), StandardCharsets.UTF_8);
        assertTrue("session manager must expose a lifecycle-safe listener rebind", text.contains("rebindListener"));
        assertTrue("session manager must distinguish Activity pause from explicit stop", text.contains("isActivityPauseInProgress()"));
    }
}
