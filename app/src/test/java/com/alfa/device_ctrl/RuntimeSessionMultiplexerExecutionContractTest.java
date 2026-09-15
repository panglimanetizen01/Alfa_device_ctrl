package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/** Contract for real multi-session ownership; no mock terminal substitutes are permitted. */
public final class RuntimeSessionMultiplexerExecutionContractTest {
    @Test public void sessionsAreIndependentlyAddressableAndActiveSelectionIsSemantic() {
        RuntimeSessionMultiplexer multiplexer = new RuntimeSessionMultiplexer();
        InteractiveSessionContract contractA = null;
        InteractiveSessionContract contractB = null;

        RuntimeSessionManager managerA = multiplexer.createSession("SESSION_A", contractA, null);
        RuntimeSessionManager managerB = multiplexer.createSession("SESSION_B", contractB, null);

        assertNotSame(managerA, managerB);
        assertSame(managerA, multiplexer.getSession("SESSION_A"));
        assertSame(managerB, multiplexer.getSession("SESSION_B"));

        multiplexer.setActiveSessionId("SESSION_A");
        assertEquals("SESSION_A", multiplexer.activeSessionId());
        assertSame(managerA, multiplexer.activeSession());

        multiplexer.setActiveSessionId("SESSION_B");
        assertEquals("SESSION_B", multiplexer.activeSessionId());
        assertSame(managerB, multiplexer.activeSession());
    }
}
