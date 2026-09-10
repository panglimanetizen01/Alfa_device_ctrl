package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;

import org.junit.Test;

import java.lang.reflect.Field;

/** Native-Views regression proof for runtime dashboard density and accessibility sizing. */
public final class RuntimeDashboardUiTest {
    @Test public void runtimeDashboardFitsAllRegisteredRuntimeCards() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    Field field = MainActivity.class.getDeclaredField("runtimeDashboard");
                    field.setAccessible(true);
                    LinearLayout dashboard = (LinearLayout) field.get(activity);
                    assertEquals("runtime dashboard must expose every canonical runtime", RuntimeRegistry.all().size(), runtimeCardCount(dashboard));
                    int required = dashboard.getPaddingTop() + dashboard.getPaddingBottom();
                    for (int i = 0; i < dashboard.getChildCount(); i++) {
                        View child = dashboard.getChildAt(i);
                        ViewGroup.LayoutParams lp = child.getLayoutParams();
                        if (child.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                            LinearLayout.LayoutParams linear = (LinearLayout.LayoutParams) lp;
                            required += child.getMeasuredHeight() + linear.topMargin + linear.bottomMargin;
                        } else {
                            required += child.getMeasuredHeight();
                        }
                        if (child instanceof LinearLayout) {
                            assertTrue("runtime card touch surface must be at least 48dp", child.getMeasuredHeight() >= dp(activity, 48));
                        }
                    }
                    assertTrue("runtime dashboard content overflows its allocated height", required <= dashboard.getMeasuredHeight());
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError(error);
                }
            });
        }
    }

    private static int runtimeCardCount(LinearLayout dashboard) {
        int count = 0;
        for (int i = 0; i < dashboard.getChildCount(); i++) {
            if (dashboard.getChildAt(i) instanceof LinearLayout) count++;
        }
        return count;
    }

    private static int dp(MainActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
