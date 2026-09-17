package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;

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
            int envIndex = find(args, "/usr/bin/env");
            assertEquals("/usr/bin/env", args[envIndex]);
            assertEquals("-i", args[envIndex + 1]);
            assertEquals("PS1={PROMPT}", args[args.length - 3]);
            assertEquals("{SHELL}", args[args.length - 2]);
            assertEquals("-i", args[args.length - 1]);
        }
    }

    @Test public void runtimeRegistryHashIsDerivedFromCanonicalJson() throws Exception {
        File file = new File("../runtime/runtimes.v1.json");
        if (!file.isFile()) file = new File("runtime/runtimes.v1.json");
        assertTrue("canonical runtime registry JSON is missing", file.isFile());
        byte[] bytes = Files.readAllBytes(file.toPath());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        assertEquals(hex(digest.digest(bytes)), RuntimeRegistry.canonicalRegistrySha256());
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    private static int find(String[] values, String expected) {
        for (int i = 0; i < values.length; i++) {
            if (expected.equals(values[i])) return i;
        }
        throw new AssertionError("missing argument: " + expected);
    }
}
