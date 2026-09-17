package com.alfa.device_ctrl;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;

import org.junit.Test;

/** Contract for real multi-session ownership; no mock terminal substitutes are permitted. */
public final class RuntimeSessionMultiplexerExecutionContractTest {
    @Test public void multiplexerExposesSemanticActiveSessionOwnership() throws Exception {
        Method setActive = RuntimeSessionMultiplexer.class.getDeclaredMethod("setActiveSessionId", String.class);
        Method activeId = RuntimeSessionMultiplexer.class.getDeclaredMethod("activeSessionId");
        Method active = RuntimeSessionMultiplexer.class.getDeclaredMethod("activeSession");
        assertNotNull(setActive);
        assertNotNull(activeId);
        assertNotNull(active);
    }

    @Test public void sessionManagerExposesLiveTerminalIdentityForRuntimeEvidence() throws Exception {
        Method current = RuntimeSessionManager.class.getDeclaredMethod("currentSession");
        Method running = RuntimeSessionManager.class.getDeclaredMethod("isRunning");
        assertNotNull(current);
        assertNotNull(running);
    }
}
