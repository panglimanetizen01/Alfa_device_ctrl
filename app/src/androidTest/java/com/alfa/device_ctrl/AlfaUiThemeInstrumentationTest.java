package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

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
}
