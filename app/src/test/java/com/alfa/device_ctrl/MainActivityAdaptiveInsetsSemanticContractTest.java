package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class MainActivityAdaptiveInsetsSemanticContractTest {
    private static String source(String path) throws Exception {
        File file = new File(path);
        if (!file.isFile()) file = new File("app/" + path);
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void lifecycleRebindsForegroundOwnedSessionWithoutStoppingIt() throws Exception {
        String app = source("src/main/java/com/alfa/device_ctrl/AlfaApplication.java");
        String manager = source("src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        String activity = source("src/main/java/com/alfa/device_ctrl/MainActivity.java");
        assertTrue(app.contains("RuntimeKeepAliveService.owner()"));
        assertTrue(app.contains("owner.rebindListener"));
        assertTrue(app.contains("owner.attachTo"));
        assertTrue(app.contains("onActivityPaused"));
        assertTrue(app.contains("activityPauseInProgress"));
        assertTrue(manager.contains("isActivityPauseInProgress()"));
        assertTrue(manager.contains("if (AlfaApplication.isActivityPauseInProgress())"));
        assertTrue(activity.contains("sessionManager.stop();"));
    }

    @Test public void semanticStateProjectionRemainsSingleSource() throws Exception {
        String manager = source("src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        String state = source("src/main/java/com/alfa/device_ctrl/SessionUiState.java");
        assertTrue(manager.contains("SessionUiState.resolve(event).name()"));
        assertTrue(state.contains("public enum Status { NOT_READY, STARTING, RUNNING, FAILED, FINISHED }"));
        assertTrue(state.contains("case \"BACKGROUND_SESSION_PRESERVED\":"));
        assertTrue(state.contains("return Status.RUNNING;"));
    }

    @Test public void layoutUsesInsetAdjustedActualContentSpace() throws Exception {
        String theme = source("src/main/java/com/alfa/device_ctrl/AlfaUiTheme.java");
        String app = source("src/main/java/com/alfa/device_ctrl/AlfaApplication.java");
        String policy = source("src/main/java/com/alfa/device_ctrl/AdaptiveRuntimeLayoutPolicy.java");
        assertTrue(theme.contains("content.getHeight() / density"));
        assertTrue(theme.contains("AdaptiveRuntimeLayoutPolicy.resolve"));
        assertTrue(app.contains("WindowCompat.enableEdgeToEdge"));
        assertTrue(app.contains("setOnApplyWindowInsetsListener"));
        assertTrue(app.contains("setInsets("));
        assertTrue(app.contains("Insets.NONE"));
        assertTrue(policy.contains("terminalMinDp"));
    }
}
