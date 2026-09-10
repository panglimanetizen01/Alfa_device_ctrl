package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RuntimeProfileTest {
    @Test public void profileIsImmutableMetadata() {
        RuntimeProfile profile = RuntimeRegistry.get("debian");
        assertNotNull(profile);
        assertEquals("debian", profile.id());
        assertEquals("Debian", profile.displayName());
        assertEquals("aarch64", profile.architecture());
        assertTrue(profile.rootfsUrl().startsWith("https://"));
        assertTrue(profile.rootfsSha256().matches("[0-9a-f]{64}"));
        assertTrue(profile.rootfsGzip());
    }

    @Test public void KaliUsesXzArchiveMetadata() {
        RuntimeProfile profile = RuntimeRegistry.get("kali");
        assertNotNull(profile);
        assertEquals("Kali Linux", profile.displayName());
        assertFalse(profile.rootfsGzip());
        assertTrue(profile.rootfsUrl().endsWith(".tar.xz"));
    }

    @Test public void everyBuiltInProfileProvidesProfileDrivenProotArguments() {
        for (RuntimeProfile profile : RuntimeRegistry.all()) {
            String[] args = profile.prootArguments();
            assertTrue(args.length > 0);
            assertEquals("-0", args[0]);
            assertEquals("-r", args[1]);
            assertEquals("{RUNTIME_ROOT}", args[2]);
            assertEquals("/usr/bin/env", args[11]);
            assertEquals("-i", args[12]);
            assertEquals("{PROMPT}", args[args.length - 3]);
            assertEquals("{SHELL}", args[args.length - 2]);
            assertEquals("-i", args[args.length - 1]);
        }
    }
}
