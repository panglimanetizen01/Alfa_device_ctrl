package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** Gate 7 provenance must be bound to the registry compiled into the APK. */
public final class Gate6LaunchContractRegistryTest {
    @Test public void launchContractRejectsStaleRuntimeRegistryHash() throws Exception {
        File dir = Files.createTempDirectory("alfa-gate6-registry-").toFile();
        File bootstrap = new File(dir, "bootstrap.txt");
        File launch = new File(dir, "gate7-launch.properties");
        String text = "schema_version=gate6-bootstrap.v1\n"
                + "gate=gate6\n"
                + "gate_status=PASS\n"
                + "runtime_id=debian\n"
                + "pipeline_run_id=run-1\n"
                + "source_commit=0123456789abcdef0123456789abcdef01234567\n"
                + "gate4_contract_sha256=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\n"
                + "profile_sha256=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789\n"
                + "runtime_registry_sha256=ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff\n"
                + "implementation_commit=abcdef0123456789abcdef0123456789abcdef01\n"
                + "decision_id=decision-1\n"
                + "request_id=request-1\n"
                + "authorization_status=AUTHORIZED\n"
                + "bootstrap_status=PASS\n"
                + "bootstrap_probe=PASS\n";
        Files.write(bootstrap.toPath(), text.getBytes(StandardCharsets.UTF_8));

        try {
            Gate6LaunchContract.importBootstrap(bootstrap, launch);
            fail("stale registry provenance must be rejected during import");
        } catch (IllegalStateException expected) {
            assertFalse(launch.exists());
        }

        String current = text.replace(
                "runtime_registry_sha256=ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "runtime_registry_sha256=" + RuntimeRegistry.CANONICAL_REGISTRY_SHA256);
        Files.write(bootstrap.toPath(), current.getBytes(StandardCharsets.UTF_8));
        Gate6LaunchContract.importBootstrap(bootstrap, launch);
        assertTrue("current registry provenance must be accepted", Gate6LaunchContract.verify(launch, "debian"));
    }
}
