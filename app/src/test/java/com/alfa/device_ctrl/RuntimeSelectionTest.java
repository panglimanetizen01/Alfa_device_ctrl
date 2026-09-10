package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public final class RuntimeSelectionTest {
    @Test public void defaultSelectionIsDebian() {
        assertEquals("debian", RuntimeSelection.DEFAULT_RUNTIME_ID);
        assertEquals("debian", RuntimeSelection.resolveId(null));
        assertEquals("debian", RuntimeSelection.resolveId(""));
    }

    @Test public void supportedSelectionResolvesToRegistryProfile() {
        assertEquals("ubuntu", RuntimeSelection.resolveId("ubuntu"));
        assertEquals("alpine", RuntimeSelection.resolveId("alpine"));
        assertEquals("kali", RuntimeSelection.resolveId("kali"));
        assertNotNull(RuntimeSelection.profile("ubuntu"));
        assertEquals("Kali Linux", RuntimeSelection.profile("kali").displayName());
    }

    @Test public void unknownSelectionFallsBackToDefault() {
        assertEquals("debian", RuntimeSelection.resolveId("not-a-runtime"));
        assertEquals("debian", RuntimeSelection.resolveId("../ubuntu"));
    }
}
