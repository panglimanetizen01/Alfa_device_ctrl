package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/**
 * UI-13: real Android instrumentation proof for the current AlfaUiShell and its navigation.
 * This test must run on an Android device/emulator; local JVM tests cannot satisfy UI-13.
 */
public final class Ui13DutVisualInteractionTest {
    private static final int MIN_TOUCH_DP = 48;
    private static final int CANONICAL_EMERALD = Color.rgb(16, 185, 129);

    @Test
    public void renderedShellAndNavigationAreInteractiveOnAndroid() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            sleepForShellInstall();

            scenario.onActivity(activity -> {
                View root = activity.getWindow().getDecorView();
                TextView title = findText(root, "ALFA DEVICE CTRL");
                assertNotNull("Alfa shell root must be rendered", title);
                assertNotNull("Runtime navigation must be rendered", findText(root, "RUNTIME"));
                assertNotNull("Terminal navigation must be rendered", findText(root, "TERMINAL"));
                assertNotNull("Project navigation must be rendered", findText(root, "PROJECT"));
                assertNotNull("Diagnostic navigation must be rendered", findText(root, "DIAGNOSTIC"));
                assertNotNull("Network navigation must be rendered", findText(root, "NETWORK"));
                assertEquals("shell title must use canonical emerald", CANONICAL_EMERALD, title.getCurrentTextColor());
                assertTrue("shell title must be visible", title.isShown());

                for (String label : new String[]{"RUNTIME", "TERMINAL", "PROJECT", "DIAGNOSTIC", "NETWORK"}) {
                    Button button = findButton(root, label);
                    assertNotNull("missing navigation button: " + label, button);
                    assertTrue("navigation target below 48dp: " + label, button.getMinimumHeight() >= dp(activity, MIN_TOUCH_DP));
                }

                click(findButton(root, "TERMINAL"));
                assertNotNull("terminal workspace must remain rendered after TERMINAL interaction", findText(root, "PTY SESSION MULTIPLEXER"));

                click(findButton(root, "PROJECT"));
                assertNotNull("project explorer must become visible after PROJECT interaction", findText(root, "PROJECT EXPLORER"));

                click(findButton(root, "RUNTIME"));
                assertNotNull("runtime dashboard must become visible after RUNTIME interaction", findText(root, "RUNTIME DASHBOARD"));
            });
        }
        System.out.println("UI13_DUT_STATUS=PASS");
        System.out.println("UI13_DUT_RESULT=REAL_ANDROID_INSTRUMENTATION");
    }

    private static void sleepForShellInstall() throws InterruptedException {
        Thread.sleep(250L);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void click(Button button) {
        assertNotNull("required interactive button missing", button);
        assertTrue("button interaction was not accepted", button.performClick());
    }

    private static Button findButton(View root, String text) {
        if (root instanceof Button && text.contentEquals(((Button) root).getText())) return (Button) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button result = findButton(group.getChildAt(i), text);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static TextView findText(View root, String text) {
        if (root instanceof TextView && text.contentEquals(((TextView) root).getText())) return (TextView) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView result = findText(group.getChildAt(i), text);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static int dp(MainActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
