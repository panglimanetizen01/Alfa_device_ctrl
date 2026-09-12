package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** APK builds must not silently omit current-run runtime provenance or accept an untrusted loader. */
public final class ReleaseProvenanceContractTest {
    @Test public void apkBuildRequiresCurrentRunGate7Asset() throws Exception {
        File source = new File("build.gradle");
        assertTrue(source.isFile());
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("ALFA_GATE7_ASSET"));
        assertTrue(text.contains("APK readiness is blocked: current-run Gate 7 launch provenance is missing"));
        assertTrue(text.contains("ALFA_GATE7_RUNTIME_ID"));
        assertTrue(text.contains("value = { key ->"));
        assertTrue(text.contains("key.endsWith('=')"));
        assertTrue(!text.contains("gradle.startParameter.taskNames.any"));
    }

    @Test public void loaderBuildFailsOnTrustedShaMismatch() throws Exception {
        File source = new File("../tools/build_proot_loader.sh");
        assertTrue(source.isFile());
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("TRUSTED_LOADER_SHA256_ARM64=\"" + RuntimeEvidence.TRUSTED_PROOT_LOADER_ARM64_SHA256 + "\""));
        assertTrue(text.contains("TRUSTED_LOADER_SHA256_X86_64=\"" + RuntimeEvidence.TRUSTED_PROOT_LOADER_CI_X86_64_SHA256 + "\""));
        assertTrue(text.contains("TRUSTED_SHA256_MISMATCH"));
        assertTrue(text.contains("exit 21"));
    }

    @Test public void launchContractCannotSurviveAnUpgradeWithoutFreshPackagedProvenance() throws Exception {
        File source = new File("src/main/java/com/alfa/device_ctrl/AlfaApplication.java");
        assertTrue(source.isFile());
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("destination.exists()"));
        assertTrue(text.contains("destination.delete()"));
        assertTrue(text.contains("validate(temporary)"));
        assertTrue(text.contains("temporary.renameTo(destination)"));
    }
}
