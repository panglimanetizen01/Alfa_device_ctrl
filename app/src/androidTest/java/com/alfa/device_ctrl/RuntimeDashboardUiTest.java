package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.test.core.app.ActivityScenario;

import org.junit.Test;

import java.lang.reflect.Field;

/** Native-Views regression proof for the canonical Stitch runtime dashboard density and accessibility sizing. */
public final class RuntimeDashboardUiTest {
    @Test public void runtimeDashboardExposesAllRegisteredRuntimeCardsWithAccessibleHeight() throws Exception {
        try (ActivityScenario<StitchOperationalActivity> scenario = ActivityScenario.launch(StitchOperationalActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    Field pageField = StitchOperationalActivity.class.getDeclaredField("nodePage");
                    pageField.setAccessible(true);
                    LinearLayout nodePage = (LinearLayout) pageField.get(activity);
                    LinearLayout window = (LinearLayout) nodePage.getChildAt(0);
                    ScrollView scroll = (ScrollView) window.getChildAt(1);
                    LinearLayout dashboard = (LinearLayout) scroll.getChildAt(0);
                    assertEquals("canonical runtime dashboard must expose every registered runtime", RuntimeRegistry.all().size(), dashboard.getChildCount());
                    for (int i = 0; i < dashboard.getChildCount(); i++) {
                        View child = dashboard.getChildAt(i);
                        assertTrue("runtime card must be a native layout", child instanceof LinearLayout);
                        assertTrue("runtime card touch surface must be at least 48dp", child.getMeasuredHeight() >= dp(activity, 48));
                    }
                    assertTrue("runtime dashboard must remain scrollable", scroll.getChildCount() == 1 && scroll.getChildAt(0) == dashboard);
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError(error);
                }
            });
        }
    }

    private static int dp(StitchOperationalActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
