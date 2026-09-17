package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FourSessionRuntimeContractTest {
    @Test public void canonicalMappingCoversExactlyFourIndependentRuntimeSessions() {
        FourSessionRuntimeContract contract = new FourSessionRuntimeContract();
        assertEquals(4, contract.sessionIds().size());
        assertEquals(4, contract.runtimeIds().size());
        assertEquals("debian", contract.runtimeFor("SESSION_A"));
        assertEquals("ubuntu", contract.runtimeFor("SESSION_B"));
        assertEquals("alpine", contract.runtimeFor("SESSION_C"));
        assertEquals("kali", contract.runtimeFor("SESSION_D"));
    }

    @Test public void canonicalPairingRejectsCrossRuntimeBinding() {
        FourSessionRuntimeContract contract = new FourSessionRuntimeContract();
        assertTrue(contract.isCanonicalPair("SESSION_A", "debian"));
        assertTrue(contract.isCanonicalPair("SESSION_D", "kali"));
        assertFalse(contract.isCanonicalPair("SESSION_A", "kali"));
        assertFalse(contract.isCanonicalPair("SESSION_B", "alpine"));
    }

    @Test public void sessionWorkingDirectoriesAreDistinct() {
        FourSessionRuntimeContract contract = new FourSessionRuntimeContract();
        assertEquals("session-a", contract.cwdNameFor("SESSION_A"));
        assertEquals("session-b", contract.cwdNameFor("SESSION_B"));
        assertEquals("session-c", contract.cwdNameFor("SESSION_C"));
        assertEquals("session-d", contract.cwdNameFor("SESSION_D"));
        assertTrue(contract.isIsolatedFrom("SESSION_A", "SESSION_B"));
        assertTrue(contract.isIsolatedFrom("SESSION_A", "SESSION_D"));
        assertFalse(contract.isIsolatedFrom("SESSION_A", "SESSION_A"));
    }

    @Test public void expectedWorkingDirectorySetIsStable() {
        FourSessionRuntimeContract contract = new FourSessionRuntimeContract();
        assertEquals("session-a", contract.expectedSessionCwds()[0]);
        assertEquals("session-d", contract.expectedSessionCwds()[3]);
    }
}
