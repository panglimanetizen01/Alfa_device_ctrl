package com.alfa.device_ctrl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RuntimeSessionRegistryTest {
    @Test public void acceptsExactlyTenReservedIndependentSlots() {
        RuntimeSessionRegistry registry = new RuntimeSessionRegistry(null);
        for (int i = 1; i <= RuntimeSessionRegistry.MAX_SESSIONS; i++) assertTrue(registry.reserve("session-" + i));
        assertEquals(10, registry.size());
        assertEquals(0, registry.boundCount());
        assertFalse(registry.reserve("session-11"));
    }

    @Test public void rejectsDuplicateAndUnsafeIds() {
        RuntimeSessionRegistry registry = new RuntimeSessionRegistry(null);
        assertTrue(registry.reserve("session-1"));
        assertFalse(registry.reserve("session-1"));
        assertFalse(registry.reserve("../escape"));
        assertFalse(registry.reserve(""));
    }

    @Test public void removalReopensCapacity() {
        RuntimeSessionRegistry registry = new RuntimeSessionRegistry(null);
        for (int i = 1; i <= RuntimeSessionRegistry.MAX_SESSIONS; i++) assertTrue(registry.reserve("session-" + i));
        assertTrue(registry.remove("session-10"));
        assertTrue(registry.reserve("session-11"));
        assertEquals(10, registry.size());
    }
}
