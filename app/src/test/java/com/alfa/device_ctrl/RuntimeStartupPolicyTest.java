package com.alfa.device_ctrl;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression contract: first visible startup may auto-start only after every local execution boundary is proven. */
public final class RuntimeStartupPolicyTest {
    @Test public void autoStartRequiresVisibleAuthorizedReadyRuntime() {
        assertTrue(RuntimeStartupPolicy.shouldAutoStart(true, false, true, true));
        assertFalse(RuntimeStartupPolicy.shouldAutoStart(false, false, true, true));
        assertFalse(RuntimeStartupPolicy.shouldAutoStart(true, true, true, true));
        assertFalse(RuntimeStartupPolicy.shouldAutoStart(true, false, false, true));
        assertFalse(RuntimeStartupPolicy.shouldAutoStart(true, false, true, false));
    }
}
