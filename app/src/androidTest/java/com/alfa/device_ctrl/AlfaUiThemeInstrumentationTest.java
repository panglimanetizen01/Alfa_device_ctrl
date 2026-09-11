package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

@RunWith(AndroidJUnit4.class)
public final class AlfaUiThemeInstrumentationTest {
    @Test
    public void interactiveControlsHaveMinimumTouchTarget() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                int min = Math.round(48f * activity.getResources().getDisplayMetrics().density);
                int checked = assertInteractiveTargets(activity.findViewById(android.R.id.content), min);
                assertTrue("expected at least one interactive control", checked > 0);
            });
        }
    }

    @Test
    public void terminalObsidianTokensReplaceLegacyVisualPalette() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View root = activity.findViewById(android.R.id.content);
                assertTrue("root background must be a ColorDrawable", root.getBackground() instanceof ColorDrawable);
                assertEquals("root background must use Terminal Obsidian canvas", AlfaUiTheme.CANVAS, ((ColorDrawable) root.getBackground()).getColor());
                assertEquals("status bar must use Terminal Obsidian canvas", AlfaUiTheme.CANVAS, activity.getWindow().getStatusBarColor());
                assertEquals("navigation bar must use Terminal Obsidian canvas", AlfaUiTheme.CANVAS, activity.getWindow().getNavigationBarColor());
                assertNoLegacyTextColors(root);
            });
        }
    }

    @Test
    public void runtimeCardsRemainAtLeast48dpAfterMeasuredAdaptation() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    Field field = MainActivity.class.getDeclaredField("runtimeDashboard");
                    field.setAccessible(true);
                    LinearLayout dashboard = (LinearLayout) field.get(activity);
                    int min = Math.round(48f * activity.getResources().getDisplayMetrics().density);
                    for (int i = 0; i < dashboard.getChildCount(); i++) {
                        View child = dashboard.getChildAt(i);
                        if (child instanceof LinearLayout) assertTrue("runtime card must retain 48dp minimum", child.getMeasuredHeight() >= min);
                    }
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError(error);
                }
            });
        }
    }

    @Test
    public void finalShellContainsCanonicalExecutionLanePresentation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View lanesView = activity.findViewById(android.R.id.content).findViewWithTag("alfa.execution.lanes");
                assertNotNull("final shell must expose the execution-lane panel", lanesView);
                assertTrue(lanesView instanceof LinearLayout);
                LinearLayout lanes = (LinearLayout) lanesView;
                assertEquals("four canonical execution lanes must be represented", ExecutionLane.values().length, lanes.getChildCount());
                assertTrue("runtime lane must be represented", textContains(lanes, "RUNTIME"));
                assertTrue("Termux lane must be represented", textContains(lanes, "TERMUX"));
                assertTrue("Termux API lane must be represented", textContains(lanes, "TERMUX API"));
                assertTrue("Shizuku/Rish lane must be represented", textContains(lanes, "SHIZUKU / RISH"));
            });
        }
    }

    private static int assertInteractiveTargets(View view, int min) {
        int checked = 0;
        if (view instanceof Button || view.isClickable()) {
            assertTrue("interactive view is below 48dp height: " + view.getClass().getName(), view.getMinimumHeight() >= min);
            checked++;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) checked += assertInteractiveTargets(group.getChildAt(i), min);
        }
        return checked;
    }

    private static boolean textContains(View view, String expected) {
        if (view instanceof TextView && String.valueOf(((TextView) view).getText()).contains(expected)) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (textContains(group.getChildAt(i), expected)) return true;
        }
        return false;
    }

    private static void assertNoLegacyTextColors(View view) {
        if (view instanceof TextView) {
            int color = ((TextView) view).getCurrentTextColor();
            assertTrue("legacy primary palette leaked into UI", color != Color.rgb(171, 199, 255));
            assertTrue("legacy muted palette leaked into UI", color != Color.rgb(193, 198, 213));
            assertTrue("legacy error palette leaked into UI", color != Color.rgb(255, 180, 171));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) assertNoLegacyTextColors(group.getChildAt(i));
        }
    }
}
