package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class ProductionTransportContractTest {
    private static File repoFile(String path) {
        File root = new File(System.getProperty("user.dir"));
        while (root != null && !new File(root, "settings.gradle").isFile()) {
            root = root.getParentFile();
        }
        if (root == null) {
            throw new IllegalStateException("repository root not found");
        }
        return new File(root, path);
    }

    private static String read(String path) throws Exception {
        File file = repoFile(path);
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void shizukuProductionEdgeIsPresent() throws Exception {
        String gradle = read("app/build.gradle");
        String manifest = read("app/src/main/AndroidManifest.xml");
        assertTrue("Shizuku API dependency missing", gradle.contains("dev.rikka.shizuku:api:13.1.5"));
        assertTrue("Shizuku provider dependency missing", gradle.contains("dev.rikka.shizuku:provider:13.1.5"));
        assertTrue("Shizuku provider missing", manifest.contains("rikka.shizuku.ShizukuProvider"));
        assertTrue("production bridge missing", repoFile("app/src/main/java/com/alfa/device_ctrl/ShizukuExecutionBridge.java").isFile());
        assertTrue("UserService missing", repoFile("app/src/main/java/com/alfa/device_ctrl/AlfaShizukuUserService.java").isFile());
        assertTrue("AIDL missing", repoFile("app/src/main/aidl/com/alfa/device_ctrl/IAlfaShizukuService.aidl").isFile());
    }

    @Test public void termuxProductionEdgeIsPresent() throws Exception {
        String gradle = read("app/build.gradle");
        String manifest = read("app/src/main/AndroidManifest.xml");
        assertTrue("Termux app dependency missing", gradle.contains("com.github.Termux:Termux-app:v0.118.0"));
        assertTrue("RUN_COMMAND permission missing", manifest.contains("com.termux.permission.RUN_COMMAND"));
        assertTrue("result receiver missing", repoFile("app/src/main/java/com/alfa/device_ctrl/TermuxRunCommandResultReceiver.java").isFile());
        assertTrue("production bridge missing", repoFile("app/src/main/java/com/alfa/device_ctrl/TermuxRunCommandBridge.java").isFile());
    }

    @Test public void termuxApiIsSeparateCapabilityLane() throws Exception {
        assertTrue(repoFile("app/src/main/java/com/alfa/device_ctrl/TermuxApiCapabilityBridge.java").isFile());
        String source = read("app/src/main/java/com/alfa/device_ctrl/TermuxApiCapabilityBridge.java");
        assertTrue(source.contains("termux-battery-status"));
        assertTrue(source.contains("TermuxRunCommandBridge"));
    }

    @Test public void activeUiCallsCoordinator() throws Exception {
        String source = read("app/src/main/java/com/alfa/device_ctrl/StitchOperationalActivityV2.java");
        assertTrue(source.contains("ExternalExecutionCoordinator"));
        assertTrue(source.contains("executeShizuku"));
        assertTrue(source.contains("executeTermux"));
        assertTrue(source.contains("probeTermuxApi"));
    }

    @Test public void evidencePersistenceIsExplicit() throws Exception {
        String source = read("app/src/main/java/com/alfa/device_ctrl/ExternalExecutionEvidence.java");
        assertTrue(source.contains("PID"));
        assertTrue(source.contains("stdout"));
        assertTrue(source.contains("stderr"));
        assertTrue(source.contains("exit"));
        assertTrue(source.contains("persist"));
    }
}
