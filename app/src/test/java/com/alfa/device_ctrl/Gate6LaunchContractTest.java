package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.Test;

public final class Gate6LaunchContractTest {
    @Test public void importBindsRuntimeFromGate6Bootstrap() throws Exception {
        File dir = Files.createTempDirectory("alfa-g6-").toFile();
        try {
            File bootstrap = new File(dir, "bootstrap.txt");
            File launch = new File(dir, "gate7.properties");
            writeBootstrap(bootstrap, "alpine", RuntimeRegistry.CANONICAL_REGISTRY_SHA256);

            Gate6LaunchContract.importBootstrap(bootstrap, launch);

            assertTrue(Gate6LaunchContract.verify(launch, "alpine"));
            assertFalse(Gate6LaunchContract.verify(launch, "ubuntu"));
            String text = new String(Files.readAllBytes(launch.toPath()), StandardCharsets.UTF_8);
            assertTrue(text.contains("runtime_id=alpine"));
            assertTrue(text.contains("runtime_registry_sha256=" + RuntimeRegistry.CANONICAL_REGISTRY_SHA256));
        } finally {
            delete(dir);
        }
    }

    @Test public void importRejectsUnknownRuntimeAndNeverOverridesBootstrapIdentity() throws Exception {
        File dir = Files.createTempDirectory("alfa-g6-").toFile();
        try {
            File bootstrap = new File(dir, "bootstrap.txt");
            File launch = new File(dir, "gate7.properties");
            writeBootstrap(bootstrap, "not-a-runtime", RuntimeRegistry.CANONICAL_REGISTRY_SHA256);

            try {
                Gate6LaunchContract.importBootstrap(bootstrap, launch);
                org.junit.Assert.fail("unknown runtime must be rejected");
            } catch (IllegalArgumentException expected) {
                assertEquals("unsupported-runtime-id", expected.getMessage());
            }
            assertFalse(launch.exists());
        } finally {
            delete(dir);
        }
    }

    @Test public void verifyRequiresGate7AuthorizationContract() throws Exception {
        File dir = Files.createTempDirectory("alfa-g6-").toFile();
        try {
            File launch = new File(dir, "gate7.properties");
            Files.write(launch.toPath(), "runtime_id=debian\n".getBytes(StandardCharsets.UTF_8));
            assertFalse(Gate6LaunchContract.verify(launch, "debian"));
        } finally {
            delete(dir);
        }
    }

    private static void writeBootstrap(File file, String runtimeId, String registryHash) throws Exception {
        String run = "run-" + UUID.randomUUID();
        String source = "0123456789012345678901234567890123456789";
        String hash = "0123456789012345678901234567890123456789012345678901234567890123";
        String text = "schema_version=gate6-bootstrap.v1\n"
                + "gate=gate6\n"
                + "gate_status=PASS\n"
                + "pipeline_run_id=" + run + "\n"
                + "runtime_id=" + runtimeId + "\n"
                + "runtime_registry_sha256=" + registryHash + "\n"
                + "source_commit=" + source + "\n"
                + "implementation_commit=" + source + "\n"
                + "gate4_contract_sha256=" + hash + "\n"
                + "profile_sha256=" + hash + "\n"
                + "decision_id=decision-test\n"
                + "request_id=request-test\n"
                + "authorization_status=AUTHORIZED\n"
                + "bootstrap_status=PASS\n"
                + "bootstrap_probe=PASS\n";
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) delete(child);
        }
        file.delete();
    }
}
