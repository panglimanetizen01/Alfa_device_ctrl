package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public final class RuntimeRegistryTest {
    @Test public void registryContainsRequiredInitialRuntimes() {
        List<RuntimeProfile> profiles = RuntimeRegistry.all();
        assertEquals(4, profiles.size());

        Set<String> ids = new HashSet<>();
        for (RuntimeProfile profile : profiles) {
            assertNotNull(profile);
            assertTrue(profile.id().length() > 0);
            assertTrue(profile.displayName().length() > 0);
            assertEquals("aarch64", profile.architecture());
            assertTrue(profile.rootfsUrl().startsWith("https://"));
            assertTrue(profile.rootfsSha256().matches("[0-9a-f]{64}"));
            assertTrue(ids.add(profile.id()));
        }

        assertNotNull(RuntimeRegistry.get("debian"));
        assertNotNull(RuntimeRegistry.get("ubuntu"));
        assertNotNull(RuntimeRegistry.get("alpine"));
        assertNotNull(RuntimeRegistry.get("kali"));
    }

    @Test public void requiredArtifactIdentitiesArePinned() {
        assertEquals(
                "c6cbf97176c58c741329cd787e932a1e47931b35f5dc0f23db3e6e82924fef0f",
                RuntimeRegistry.get("debian").rootfsSha256());
        assertEquals(
                "04207713ece899c3740823d33690441ad3a7f0ded1101aca744e2b0f37ac7ff2",
                RuntimeRegistry.get("ubuntu").rootfsSha256());
        assertEquals(
                "f55a90f69052c5bd6f92cb09a8f47065970830b194c917a006fb94028e721259",
                RuntimeRegistry.get("alpine").rootfsSha256());
        assertEquals(
                "d6403a5da175df325611d23af4b92330856059c45454eced7f4cdf3ca6df2e4e",
                RuntimeRegistry.get("kali").rootfsSha256());
    }

    @Test public void unknownRuntimeIsRejected() {
        assertEquals(null, RuntimeRegistry.get("not-a-runtime"));
    }
}
