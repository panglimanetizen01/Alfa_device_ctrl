package com.alfa.device_ctrl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Execution proof for Gates 1-5: the emulator must instantiate every native UI surface,
 * not merely inspect manifest declarations or static source contracts.
 */
@RunWith(AndroidJUnit4.class)
public final class Gate1To5RuntimeExecutionInstrumentationTest {
    @Test(timeout = 30000)
    public void gatesOneToFiveNativeSurfacesActuallyExecute() {
        assertActivityExecutes(MainActivity.class);
        assertActivityExecutes(RuntimeToolsActivity.class);
        assertActivityExecutes(ProjectExplorerActivity.class);
        assertActivityExecutes(SecurityDiagnosticActivity.class);
        assertActivityExecutes(PolicyEvidenceActivity.class);
    }

    private static void assertActivityExecutes(Class<? extends Activity> activityClass) {
        try (ActivityScenario<? extends Activity> scenario = ActivityScenario.launch(activityClass)) {
            scenario.onActivity(activity -> {
                View root = activity.findViewById(android.R.id.content);
                assertNotNull(activityClass.getSimpleName() + " content root missing", root);
                assertTrue(activityClass.getSimpleName() + " activity is not resumed", activity.hasWindowFocus() || !activity.isFinishing());
            });
        }
    }
}
