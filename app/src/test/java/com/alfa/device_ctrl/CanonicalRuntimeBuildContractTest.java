package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public final class CanonicalRuntimeBuildContractTest {
    @Test
    public void appBuildMustOwnProotLoaderPreparation() throws Exception {
        Path buildFile = Paths.get("build.gradle");
        String text = new String(Files.readAllBytes(buildFile), StandardCharsets.UTF_8);
        assertTrue("canonical build must expose prepareProotLoader", text.contains("prepareProotLoader"));
        assertTrue("canonical build must wire preBuild to prepareProotLoader", text.contains("preBuild.dependsOn prepareProotLoader"));
    }
}
