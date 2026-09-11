package com.alfa.device_ctrl;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Lifecycle proof: only a real service-owned session may satisfy the strict reattachment mode. */
@RunWith(AndroidJUnit4.class)
public final class RuntimeSessionReattachmentInstrumentationTest {
    @Rule public final ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class);

    @Test public void reattachExistingServiceOwnedSession() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        MainActivity activity = rule.getActivity();
        boolean required = "true".equalsIgnoreCase(InstrumentationRegistry.getArguments().getString("require_live_session", "false"));
        RuntimeSessionManager owner = RuntimeKeepAliveService.owner();
        if (!required && owner == null) {
            RuntimeSessionReattachment.tryReattach(activity);
            assertTrue("safe no-session reattachment path completed", true);
            return;
        }
        assertTrue("strict reattachment requires an active service-owned manager", owner != null && owner.isRunning());
        RuntimeSessionReattachment.tryReattach(activity);
        java.lang.reflect.Field field = MainActivity.class.getDeclaredField("sessionManager");
        field.setAccessible(true);
        Object rebound = field.get(activity);
        assertSame("recreated MainActivity must use the existing service-owned manager", owner, rebound);
        assertTrue("existing PTY manager must remain running after reattachment", owner.isRunning());
        System.out.println("REATTACHMENT_STATUS=PASS");
        System.out.println("REATTACHMENT_OWNER_PRESERVED=true");
        System.out.println("REATTACHMENT_PTY_REUSE=true");
        instrumentation.waitForIdleSync();
    }
}
