package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Execution contracts for milestones 11-12. Labels alone are insufficient; states must expose evidence semantics. */
@RunWith(AndroidJUnit4.class)
public final class Milestone11To12ContractInstrumentationTest {
    @Test(timeout = 30000)
    public void termuxExecutionSurfaceExposesRealExecutionAndEvidenceStates() {
        try (ActivityScenario<TermuxExecutionActivity> scenario = ActivityScenario.launch(TermuxExecutionActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(countText(activity.getWindow().getDecorView(), "TERMUX_EXECUTION=") == 1);
                assertTrue(countText(activity.getWindow().getDecorView(), "TERMUX_API=") == 1);
                assertTrue(countText(activity.getWindow().getDecorView(), "RESULT=") == 1);
                assertTrue(countText(activity.getWindow().getDecorView(), "EVIDENCE=") == 1);
            });
        }
    }

    @Test(timeout = 30000)
    public void projectEditorAndSocketDiagnosticsExposeRealBackends() {
        try (ActivityScenario<ProjectEditorSocketDiagnosticsActivity> scenario = ActivityScenario.launch(ProjectEditorSocketDiagnosticsActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(countText(activity.getWindow().getDecorView(), "EDITOR_BACKEND=SAF") == 1);
                assertTrue(countText(activity.getWindow().getDecorView(), "SOCKET_BACKEND=LOCAL_SOCKET") == 1);
                assertTrue(countText(activity.getWindow().getDecorView(), "SOCKET_STATE=") == 1);
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
