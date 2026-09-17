package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

public final class RuntimeSessionMultiplexerBehaviorTest {
    @Test public void emptyMultiplexerHasNoSessionsAndCannotReportUnknownSession() {
        RuntimeSessionMultiplexer multiplexer = new RuntimeSessionMultiplexer();
        assertEquals(0, multiplexer.size());
        assertEquals(Collections.emptyList(), multiplexer.sessionIds());
        assertFalse(multiplexer.containsSession("session-1"));
        assertTrue(multiplexer.getSession("session-1") == null);
    }

    @Test public void stopAllAndRemoveAllAreIdempotentWhenEmpty() {
        RuntimeSessionMultiplexer multiplexer = new RuntimeSessionMultiplexer();
        multiplexer.stopAll();
        multiplexer.removeAll();
        multiplexer.removeAll();
        assertEquals(0, multiplexer.size());
    }
}
