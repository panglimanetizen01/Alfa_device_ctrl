package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** RED/GREEN contract tests for milestones 6-10; success must come from real Activity execution. */
@RunWith(AndroidJUnit4.class)
public final class Milestone6To10ContractInstrumentationTest {
    @Test(timeout = 30000)
    public void splitTerminalExposesDualPtyControls() {
        try (ActivityScenario<SplitTerminalActivity> scenario = ActivityScenario.launch(SplitTerminalActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue("split terminal must expose two pane roots", countText(activity.getWindow().getDecorView(), "LEFT") == 1);
                assertTrue("split terminal must expose the right pane", countText(activity.getWindow().getDecorView(), "RIGHT") == 1);
                assertTrue("split terminal must expose synchronized session state", countText(activity.getWindow().getDecorView(), "SESSIONS=") >= 1);
            });
        }
    }

    @Test(timeout = 30000)
    public void floatingTerminalExposesOverlayControls() {
        try (ActivityScenario<FloatingTerminalActivity> scenario = ActivityScenario.launch(FloatingTerminalActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue("floating terminal must expose resize control", countText(activity.getWindow().getDecorView(), "RESIZE") >= 1);
                assertTrue("floating terminal must expose opacity control", countText(activity.getWindow().getDecorView(), "OPACITY") >= 1);
                assertTrue("floating terminal must expose bubble control", countText(activity.getWindow().getDecorView(), "BUBBLE") >= 1);
            });
        }
    }

    @Test(timeout = 30000)
    public void settingsAndNotificationSurfacesExposePersistentState() {
        try (ActivityScenario<AppearanceLanguageSettingsActivity> scenario = ActivityScenario.launch(AppearanceLanguageSettingsActivity.class)) {
            scenario.onActivity(activity -> assertTrue(countText(activity.getWindow().getDecorView(), "DATASTORE=PERSISTENT") == 1));
        }
        try (ActivityScenario<NotificationStateActivity> scenario = ActivityScenario.launch(NotificationStateActivity.class)) {
            scenario.onActivity(activity -> assertTrue(countText(activity.getWindow().getDecorView(), "FGS_SERVICE_ACTIVE=") == 1));
        }
    }

    @Test(timeout = 30000)
    public void vfsPolicySurfaceExposesRuntimeBoundPolicy() {
        try (ActivityScenario<RuntimeVfsPolicyActivity> scenario = ActivityScenario.launch(RuntimeVfsPolicyActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(countText(activity.getWindow().getDecorView(), "RUNTIME_ID=") == 1);
                assertTrue(countText(activity.getWindow().getDecorView(), "POLICY_ID=") == 1);
                assertTrue(countText(activity.getWindow().getDecorView(), "RUNTIME_ROOT=") == 1);
                assertTrue(countText(activity.getWindow().getDecorView(), "HOST_CWD=") == 1);
            });
        }
    }

    @Test(timeout = 30000)
    public void securitySurfaceExposesEvidenceBackedExecutionLanes() {
        try (ActivityScenario<SecurityDiagnosticActivity> scenario = ActivityScenario.launch(SecurityDiagnosticActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(countText(activity.getWindow().getDecorView(), "SECCOMP_MODE=") == 1);
                assertTrue(countText(activity.getWindow().getDecorView(), "SHIZUKU_PACKAGE=") == 1);
                assertTrue(countText(activity.getWindow().getDecorView(), "RISH=") == 1);
                assertTrue(countText(activity.getWindow().getDecorView(), "SYSCALL=") == 1);
            });
        }
    }

    private static int countText(View root, String token) {
        int count = 0;
        if (root instanceof TextView) {
            CharSequence value = ((TextView) root).getText();
            if (value != null && value.toString().contains(token)) count++;
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) count += countText(group.getChildAt(i), token);
        }
        return count;
    }
}
