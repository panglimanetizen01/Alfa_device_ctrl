package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

/** Build contract: the canonical app must target and compile against Android 16/API 36. */
public final class AndroidApi36BuildContractTest {
    @Test
    public void appBuildTargetsAndroid16() throws Exception {
        File source = new File("app/build.gradle");
        assertTrue("app/build.gradle must exist", source.isFile());
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertTrue("compileSdk must be API 36", text.contains("compileSdk 36"));
        assertTrue("targetSdk must be API 36", text.contains("targetSdk 36"));
    }
}
