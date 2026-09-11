package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public final class CanonicalRuntimeBuildContractTest {
    @Test public void appBuildMustOwnProotLoaderPreparation() throws Exception {
        Path buildFile=Paths.get("build.gradle"); String text=new String(Files.readAllBytes(buildFile),StandardCharsets.UTF_8);
        assertTrue(text.contains("prepareProotLoader")); assertTrue(text.contains("dependsOn tasks.named('prepareProotLoader')")); assertTrue(text.contains("generated/jniLibs"));
    }
    @Test public void gate7LaunchContractMustBeGeneratedOutsideCanonicalSourceAssets() throws Exception {
        Path buildFile=Paths.get("build.gradle"); String text=new String(Files.readAllBytes(buildFile),StandardCharsets.UTF_8);
        assertTrue(text.contains("prepareGate7LaunchContract")); assertTrue(text.contains("generated/assets")); assertTrue(text.contains("ALFA_GATE7_ASSET")); assertFalse(Files.exists(Paths.get("src/main/assets/gate7-launch.properties")));
    }
    @Test public void ciMustNotMutateSourceToInjectRuntimeLoader() throws Exception {
        Path workflow=Paths.get("../.github/workflows/apk-readiness.yml"); String text=new String(Files.readAllBytes(workflow),StandardCharsets.UTF_8);
        assertTrue(text.contains(":app:assembleDebug")); assertFalse(text.contains("runner-only")); assertFalse(text.contains("app/src/main/jniLibs/arm64-v8a/libproot-loader.so")); assertFalse(text.contains("Path(\"app/src/main/java/com/alfa/device_ctrl/MainActivity.java\")"));
    }
    @Test public void manifestMustLetModernAgpControlNativeLibraryPackaging() throws Exception {
        String text=new String(Files.readAllBytes(Paths.get("src/main/AndroidManifest.xml")),StandardCharsets.UTF_8); assertFalse(text.contains("android:extractNativeLibs"));
    }
    @Test public void mainActivityMustUseSelectedRuntimeProfile() throws Exception {
        Path activity=Paths.get("src/main/java/com/alfa/device_ctrl/MainActivity.java"); String text=new String(Files.readAllBytes(activity),StandardCharsets.UTF_8);
        assertTrue(text.contains("RuntimeProfile")); assertTrue(text.contains("selectedRuntime")); assertTrue(text.contains("installer.install(profile"));
        assertFalse(text.contains("ubuntu-base/releases")); assertFalse(text.contains("alfa:ubuntu:"));
    }
}
