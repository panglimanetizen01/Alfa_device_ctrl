package com.alfa.device_ctrl;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class Gate6LaunchContractTest {
    private static final String RUN = "run_20260908_120000_12345";
    private static final String SOURCE = "0123456789abcdef0123456789abcdef01234567";
    private static final String CONTRACT = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String PROFILE = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
    private static final String IMPLEMENTATION = "89abcdef0123456789abcdef0123456789abcdef";

    @Test public void passBootstrapProducesVerifiedGate7Contract() throws Exception {
        File temp = Files.createTempDirectory("alfa-g6-launch-").toFile();
        File bootstrap = new File(temp, "bootstrap.txt");
        File launch = new File(temp, "gate7-launch.properties");
        Files.write(bootstrap.toPath(), validBootstrap().getBytes(StandardCharsets.UTF_8));

        Gate6LaunchContract.importBootstrap(bootstrap, launch, "ubuntu");

        assertTrue(launch.isFile());
        assertTrue(Gate6LaunchContract.verify(launch, "ubuntu"));
        String text = new String(Files.readAllBytes(launch.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("schema_version=gate7-launch.v1"));
        assertTrue(text.contains("pipeline_run_id=" + RUN));
        assertTrue(text.contains("runtime_id=ubuntu"));
    }

    @Test public void nonPassBootstrapIsRejectedWithoutLaunchContract() throws Exception {
        File temp = Files.createTempDirectory("alfa-g6-launch-deny-").toFile();
        File bootstrap = new File(temp, "bootstrap.txt");
        File launch = new File(temp, "gate7-launch.properties");
        Files.write(bootstrap.toPath(), validBootstrap().replace("gate_status=PASS", "gate_status=BLOCKED").getBytes(StandardCharsets.UTF_8));

        try {
            Gate6LaunchContract.importBootstrap(bootstrap, launch, "ubuntu");
        } catch (IllegalStateException expected) {
            assertFalse(launch.exists());
            return;
        }
        throw new AssertionError("blocked Gate 6 bootstrap was accepted");
    }

    @Test public void malformedProvenanceIsRejected() throws Exception {
        File temp = Files.createTempDirectory("alfa-g6-launch-malformed-").toFile();
        File bootstrap = new File(temp, "bootstrap.txt");
        File launch = new File(temp, "gate7-launch.properties");
        Files.write(bootstrap.toPath(), validBootstrap().replace("profile_sha256=" + PROFILE, "profile_sha256=bad").getBytes(StandardCharsets.UTF_8));

        try {
            Gate6LaunchContract.importBootstrap(bootstrap, launch, "ubuntu");
        } catch (IllegalStateException expected) {
            assertFalse(launch.exists());
            return;
        }
        throw new AssertionError("malformed provenance was accepted");
    }

    @Test public void wrongRuntimeIdFailsVerification() throws Exception {
        File temp = Files.createTempDirectory("alfa-g6-launch-runtime-").toFile();
        File bootstrap = new File(temp, "bootstrap.txt");
        File launch = new File(temp, "gate7-launch.properties");
        Files.write(bootstrap.toPath(), validBootstrap().getBytes(StandardCharsets.UTF_8));
        Gate6LaunchContract.importBootstrap(bootstrap, launch, "ubuntu");
        assertFalse(Gate6LaunchContract.verify(launch, "debian"));
    }

    private static String validBootstrap() {
        return "schema_version=gate6-bootstrap.v1\n"
                + "gate=gate6\n"
                + "gate_status=PASS\n"
                + "pipeline_run_id=" + RUN + "\n"
                + "source_commit=" + SOURCE + "\n"
                + "implementation_commit=" + IMPLEMENTATION + "\n"
                + "gate4_contract_sha256=" + CONTRACT + "\n"
                + "profile_sha256=" + PROFILE + "\n"
                + "decision_id=decision-123\n"
                + "request_id=request-123\n"
                + "authorization_status=AUTHORIZED\n"
                + "bootstrap_status=PASS\n"
                + "bootstrap_probe=PASS\n";
    }
}
