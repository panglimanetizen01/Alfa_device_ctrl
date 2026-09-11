package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Contract gate for UI foundation stages 1-5: shell, routing, explorer, evidence/policy and session UI. */
@RunWith(AndroidJUnit4.class)
public final class UiFoundationContractInstrumentationTest {
    @Test
    public void mainShellExposesAllStageOneToFiveCapabilityAnchors() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View root = activity.findViewById(android.R.id.content);
                assertNotNull("main shell must exist", root);
                assertNotNull("runtime capability anchor missing", findDescription(root, "Buka alat runtime"));
                assertNotNull("project explorer capability anchor missing", findDescription(root, "Tampilkan file runtime"));
                assertNotNull("policy capability anchor missing", findDescription(root, "Tampilkan kebijakan runtime"));
                assertNotNull("runtime session creation control missing", findDescription(root, "Buat sesi runtime baru"));
                assertNotNull("terminal stop control missing", findDescription(root, "Hentikan sesi runtime"));
            });
        }
    }

    @Test
    public void stageOneToFiveActivitiesAreInternalOnly() {
        PackageManager pm = InstrumentationRegistryHolder.getPackageManager();
        assertActivity(pm, ProjectExplorerActivity.class);
        assertActivity(pm, SecurityDiagnosticActivity.class);
        assertActivity(pm, PolicyEvidenceActivity.class);
        assertActivity(pm, RuntimeToolsActivity.class);
        assertActivity(pm, MainActivity.class);
    }

    @Test
    public void sessionRegistryCapacityIsDerivedFromCanonicalRuntimeRegistry() {
        RuntimeSessionRegistry registry = RuntimeSessionRegistry.get(InstrumentationRegistryHolder.getContext());
        assertEquals("session capacity must be the canonical runtime count", RuntimeRegistry.all().size(), registry.capacity());
        assertTrue("session registry cannot report a negative size", registry.size() >= 0);
        assertTrue("session registry cannot exceed canonical capacity", registry.size() <= registry.capacity());
    }

    @Test
    public void sessionStateMappingIsFailClosed() {
        assertEquals(SessionUiState.Status.NOT_READY, SessionUiState.resolve(null));
        assertEquals(SessionUiState.Status.NOT_READY, SessionUiState.resolve("UNRECOGNIZED_EVENT"));
        assertEquals(SessionUiState.Status.STARTING, SessionUiState.resolve("PTY_CREATED"));
        assertEquals(SessionUiState.Status.RUNNING, SessionUiState.resolve("READY"));
        assertEquals(SessionUiState.Status.STOPPING, SessionUiState.resolve("STOPPING"));
        assertEquals(SessionUiState.Status.FAILED, SessionUiState.resolve("BLOCKED"));
        assertEquals(SessionUiState.Status.FINISHED, SessionUiState.resolve("FINISHED"));
    }

    private static void assertActivity(PackageManager pm, Class<?> activityClass) {
        ComponentName name = new ComponentName(InstrumentationRegistryHolder.getContext(), activityClass);
        try {
            android.content.pm.ActivityInfo info = pm.getActivityInfo(name, 0);
            assertTrue(activityClass.getSimpleName() + " must not be externally exported", !info.exported || activityClass == MainActivity.class);
        } catch (PackageManager.NameNotFoundException error) {
            throw new AssertionError("missing manifest activity: " + activityClass.getName(), error);
        }
    }

    private static View findDescription(View view, String description) {
        if (description.equals(view.getContentDescription())) return view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            View found = findDescription(group.getChildAt(i), description);
            if (found != null) return found;
        }
        return null;
    }

    private static final class InstrumentationRegistryHolder {
        private static android.content.Context getContext() {
            return androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
        }
        private static PackageManager getPackageManager() { return getContext().getPackageManager(); }
    }
}
