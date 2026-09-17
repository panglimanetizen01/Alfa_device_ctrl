package com.alfa.device_ctrl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public final class MultiDistroRuntimeContractTest {
    @Test public void everyBuiltInProfileCarriesCompleteRuntimeMetadata() {
        assertEquals(4, RuntimeRegistry.all().size());
        for (RuntimeProfile profile : RuntimeRegistry.all()) {
            assertTrue(profile.version().length() > 0);
            assertEquals("aarch64", profile.architecture());
            assertTrue(profile.archiveFormat().equals("tar.gz") || profile.archiveFormat().equals("tar.xz"));
            assertTrue(profile.shell().startsWith("/"));
            assertTrue(profile.packageManager().length() > 0);
            assertTrue(profile.promptContract().contains(profile.id()));
            assertTrue(profile.environment().length > 0);
            assertTrue(profile.requiredPaths().length >= 5);
            assertTrue(profile.capabilities().length >= 5);
            assertTrue(profile.prootArguments().length > 0);
        }
    }

    @Test public void packageManagerMatchesKnownDistroFamilies() {
        assertEquals("apt", RuntimeRegistry.get("debian").packageManager());
        assertEquals("apt", RuntimeRegistry.get("ubuntu").packageManager());
        assertEquals("apk", RuntimeRegistry.get("alpine").packageManager());
        assertEquals("apt", RuntimeRegistry.get("kali").packageManager());
    }

    @Test public void KaliDeclaresRootlessKernelLimitation() {
        boolean found = false;
        for (String capability : RuntimeRegistry.get("kali").capabilities()) if ("kernel-features-limited".equals(capability)) found = true;
        assertTrue(found);
    }

    @Test public void canonicalRuntimeJsonIndependentlyProjectsEachRuntimeProfile() throws Exception {
        File file = new File("../runtime/runtimes.v1.json");
        if (!file.isFile()) file = new File("runtime/runtimes.v1.json");
        assertTrue("canonical runtime registry JSON is missing", file.isFile());

        JSONObject registry = new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        assertEquals("runtime-registry.v1", registry.getString("schema_version"));
        assertEquals("multi-distro-linux-runtime.v1", registry.getString("engine_contract"));
        assertEquals("arm64-v8a", registry.getString("target_architecture"));

        JSONArray runtimes = registry.getJSONArray("runtimes");
        assertEquals(RuntimeRegistry.all().size(), runtimes.length());
        Set<String> runtimeIds = new HashSet<>();
        Set<Integer> acceptanceOrders = new HashSet<>();

        for (int i = 0; i < runtimes.length(); i++) {
            JSONObject expected = runtimes.getJSONObject(i);
            String runtimeId = expected.getString("runtime_id");
            assertTrue("duplicate runtime_id: " + runtimeId, runtimeIds.add(runtimeId));

            int acceptanceOrder = expected.getInt("acceptance_order");
            assertTrue("duplicate acceptance_order: " + acceptanceOrder, acceptanceOrders.add(acceptanceOrder));
            assertEquals("SUPPORTED", expected.getString("acceptance_status"));
            assertEquals(i + 1, acceptanceOrder);

            RuntimeProfile actual = RuntimeRegistry.get(runtimeId);
            assertTrue("missing RuntimeProfile for " + runtimeId, actual != null);

            assertEquals(expected.getString("runtime_id"), actual.id());
            assertEquals(expected.getString("family"), actual.displayName());
            assertEquals(expected.getString("version"), actual.version());
            assertEquals(expected.getString("architecture"), actual.architecture());
            assertEquals(expected.getString("rootfs_uri"), actual.rootfsUrl());
            assertEquals(expected.getString("rootfs_sha256"), actual.rootfsSha256());
            assertEquals(expected.getBoolean("rootfs_gzip"), actual.rootfsGzip());
            assertEquals(expected.getString("archive_format"), actual.archiveFormat());
            assertEquals(expected.getString("shell_path"), actual.shell());
            assertEquals(expected.getString("package_manager"), actual.packageManager());
            assertEquals(expected.getString("prompt_contract"), actual.promptContract());
            assertArrayEquals(jsonArrayToStrings(expected.getJSONArray("environment")), actual.environment());
            assertArrayEquals(jsonArrayToStrings(expected.getJSONArray("required_paths")), actual.requiredPaths());
            assertArrayEquals(jsonArrayToStrings(expected.getJSONArray("capabilities")), actual.capabilities());
            assertArrayEquals(jsonArrayToStrings(expected.getJSONArray("proot_arguments")), actual.prootArguments());
        }

        assertEquals(runtimes.length(), runtimeIds.size());
        assertEquals(runtimes.length(), acceptanceOrders.size());
    }

    private static String[] jsonArrayToStrings(JSONArray array) {
        String[] values = new String[array.length()];
        for (int i = 0; i < array.length(); i++) values[i] = array.getString(i);
        return values;
    }
}
