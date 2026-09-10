package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class MainActivityAdaptiveInsetsSemanticContractTest {
    private static String source() throws Exception {
        File file = new File("src/main/java/com/alfa/device_ctrl/MainActivity.java");
        if (!file.isFile()) file = new File("app/src/main/java/com/alfa/device_ctrl/MainActivity.java");
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void lifecycleRebindsForegroundOwnedSession() throws Exception {
        String text = source();
        assertTrue(text.contains("RuntimeKeepAliveService.owner()"));
        assertTrue(text.contains("rebindListener(this)"));
        assertFalse(text.contains("sessionManager.stop();\n        super.onStop();"));
        assertFalse(text.contains("sessionManager.stop();\n        super.onDestroy();"));
    }

    @Test public void activityConsumesOnlySemanticSessionStates() throws Exception {
        String text = source();
        assertTrue(text.contains("SessionUiState.Status uiState = SessionUiState.resolve(state)"));
        assertFalse(text.contains("\"READY\".equals(state)"));
        assertFalse(text.contains("\"RUNNING\".equals(state)"));
    }

    @Test public void layoutUsesCurrentWindowMetricsAndInsets() throws Exception {
        String text = source();
        assertTrue(text.contains("getCurrentWindowMetrics()"));
        assertTrue(text.contains("WindowCompat.enableEdgeToEdge"));
        assertTrue(text.contains("setOnApplyWindowInsetsListener"));
        assertFalse(text.contains("new LinearLayout.LayoutParams(-1, dp(248))"));
        assertFalse(text.contains("new LinearLayout.LayoutParams(-1, dp(158))"));
    }
}
