package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class ProductionTransportContractTest {
    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }

    @Test public void shizukuProductionEdgeIsPresent() throws Exception {
        String gradle = read("app/build.gradle");
        String manifest = read("app/src/main/AndroidManifest.xml");
        assertTrue("Shizuku API dependency missing", gradle.contains("dev.rikka.shizuku:api:13.1.5"));
        assertTrue("Shizuku provider dependency missing", gradle.contains("dev.rikka.shizuku:provider:13.1.5"));
        assertTrue("Shizuku provider missing", manifest.contains("rikka.shizuku.ShizukuProvider"));
        assertTrue("production bridge missing", new File("app/src/main/java/com/alfa/device_ctrl/ShizukuExecutionBridge.java").isFile());
        assertTrue("UserService missing", new File("app/src/main/java/com/alfa/device_ctrl/AlfaShizukuUserService.java").isFile());
        assertTrue("AIDL missing", new File("app/src/main/aidl/com/alfa/device_ctrl/IAlfaShizukuService.aidl").isFile());
    }

    @Test public void termuxProductionEdgeIsPresent() throws Exception {
        String gradle = read("app/build.gradle");
        String manifest = read("app/src/main/AndroidManifest.xml");
        assertTrue("Termux shared dependency missing", gradle.contains("com.termux:termux-shared:0.109"));
        assertTrue("RUN_COMMAND permission missing", manifest.contains("com.termux.permission.RUN_COMMAND"));
        assertTrue("result receiver missing", new File("app/src/main/java/com/alfa/device_ctrl/TermuxRunCommandResultReceiver.java").isFile());
        assertTrue("production bridge missing", new File("app/src/main/java/com/alfa/device_ctrl/TermuxRunCommandBridge.java").isFile());
    }

    @Test public void termuxApiIsSeparateCapabilityLane() throws Exception {
        assertTrue(new File("app/src/main/java/com/alfa/device_ctrl/TermuxApiCapabilityBridge.java").isFile());
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
