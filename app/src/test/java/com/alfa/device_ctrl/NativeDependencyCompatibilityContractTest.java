package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class NativeDependencyCompatibilityContractTest {
    private static String source(String path) throws Exception {
        File file = new File(path);
        if (!file.isFile()) file = new File("app/" + path);
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void canonicalAndroid36BuildUsesCompatibleCoreLine() throws Exception {
        String app = source("build.gradle");
        String root = source("../build.gradle");
        assertTrue(app.contains("androidx.core:core:1.18.0"));
        assertFalse(app.contains("androidx.core:core:1.19.0"));
        assertTrue(app.contains("compileSdk 36"));
        assertTrue(root.contains("com.android.application") && root.contains("8.10.0"));
    }
}
