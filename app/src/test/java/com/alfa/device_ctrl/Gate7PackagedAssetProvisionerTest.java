package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** RED/GREEN regression contract: packaged Gate 7 must be bound to the APK currently executing. */
public final class Gate7PackagedAssetProvisionerTest {
    private static final String VALID =
            "schema_version=gate7-launch.v1\n" +
            "gate=gate7\n" +
            "gate_status=READY\n" +
            "launch_status=AUTHORIZED\n" +
            "launch_source=gate6-bootstrap\n" +
            "pipeline_run_id=run_20260912_031629_2427\n" +
            "runtime_id=debian\n" +
            "runtime_registry_sha256=2e5622860db9973f9349999306604d5b01bd6d7915d4a69ff4bf89127a1ca405\n" +
            "source_commit=73fe72a01074a1858e982863fe4358241881f37a\n" +
            "gate4_contract_sha256=963db9d820615bdaca743de6a4afb6c070a39c54f0c5c9a67a0e3b0c0697663e\n" +
            "profile_sha256=be39c832cc87f3b3809f7589a7e870a635c4e0bb9a73a51e46fdd75712f3da38\n" +
            "implementation_commit=73fe72a01074a1858e982863fe4358241881f37a\n" +
            "decision_id=decision-apk-readiness-d40581854456c373\n" +
            "request_id=apk-readiness\n" +
            "authorization_status=AUTHORIZED\n";

    @Test public void packagedGate7IsMaterializedAndVerified() throws Exception {
        File dir = Files.createTempDirectory("gate7-provision").toFile();
        File launch = new File(dir, "gate7-launch.properties");
        assertFalse(launch.isFile());
        assertTrue(Gate7PackagedAssetProvisioner.provision(
                new ByteArrayInputStream(VALID.getBytes(StandardCharsets.UTF_8)), launch, "debian"));
        assertTrue(Gate6LaunchContract.verify(launch, "debian"));
        assertEquals(VALID, new String(Files.readAllBytes(launch.toPath()), StandardCharsets.UTF_8));
    }

    @Test public void invalidPackagedGate7IsRejected() throws Exception {
        File dir = Files.createTempDirectory("gate7-provision-invalid").toFile();
        File launch = new File(dir, "gate7-launch.properties");
        assertFalse(Gate7PackagedAssetProvisioner.provision(
                new ByteArrayInputStream("gate_status=READY\n".getBytes(StandardCharsets.UTF_8)), launch, "debian"));
        assertFalse(launch.exists());
    }

    @Test public void staleExistingGate7DoesNotMatchPackagedAsset() throws Exception {
        File dir = Files.createTempDirectory("gate7-provision-stale").toFile();
        File launch = new File(dir, "gate7-launch.properties");
        Files.write(launch.toPath(), VALID.getBytes(StandardCharsets.UTF_8));
        String current = VALID.replace("pipeline_run_id=run_20260912_031629_2427", "pipeline_run_id=run-current")
                .replace("source_commit=73fe72a01074a1858e982863fe4358241881f37a", "source_commit=ec5bdf5178ebe38032c4b865bfe472e207be6839")
                .replace("implementation_commit=73fe72a01074a1858e982863fe4358241881f37a", "implementation_commit=ec5bdf5178ebe38032c4b865bfe472e207be6839");
        assertFalse(Gate7PackagedAssetProvisioner.matchesPackaged(
                launch, new ByteArrayInputStream(current.getBytes(StandardCharsets.UTF_8)), "debian"));
    }

    @Test public void currentExistingGate7MatchesPackagedAsset() throws Exception {
        File dir = Files.createTempDirectory("gate7-provision-current").toFile();
        File launch = new File(dir, "gate7-launch.properties");
        Files.write(launch.toPath(), VALID.getBytes(StandardCharsets.UTF_8));
        assertTrue(Gate7PackagedAssetProvisioner.matchesPackaged(
                launch, new ByteArrayInputStream(VALID.getBytes(StandardCharsets.UTF_8)), "debian"));
    }
}
