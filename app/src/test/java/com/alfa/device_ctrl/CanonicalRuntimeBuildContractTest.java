package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
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
        assertTrue("canonical build must wire preBuild to prepareProotLoader", text.contains("dependsOn tasks.named('prepareProotLoader')"));
        assertTrue("canonical build must package generated jniLibs", text.contains("generated/jniLibs"));
    }

    @Test
    public void ciMustNotMutateSourceToInjectRuntimeLoader() throws Exception {
        Path workflow = Paths.get("../.github/workflows/alfa-ci.yml");
        String text = new String(Files.readAllBytes(workflow), StandardCharsets.UTF_8);
        assertTrue("CI must invoke the canonical Gradle loader task", text.contains(":app:prepareProotLoader"));
        assertFalse("CI must not describe the loader build as runner-only", text.contains("runner-only"));
        assertFalse("CI must not inject loader into source jniLibs", text.contains("app/src/main/jniLibs/arm64-v8a/libproot-loader.so"));
        assertFalse("CI must not mutate MainActivity during build", text.contains("Path(\"app/src/main/java/com/alfa/device_ctrl/MainActivity.java\")"));
    }

    @Test
    public void manifestMustLetModernAgpControlNativeLibraryPackaging() throws Exception {
        Path manifest = Paths.get("src/main/AndroidManifest.xml");
        String text = new String(Files.readAllBytes(manifest), StandardCharsets.UTF_8);
        assertFalse("manifest must not override extractNativeLibs", text.contains("android:extractNativeLibs"));
    }

    @Test
    public void mainActivityMustUseCanonicalRuntimeProfile() throws Exception {
        Path activity = Paths.get("../app/src/main/java/com/alfa/device_ctrl/MainActivity.java");
        String text = new String(Files.readAllBytes(activity), StandardCharsets.UTF_8);
        assertTrue("MainActivity must reference RuntimeProfile", text.contains("RuntimeProfile"));
        assertFalse("MainActivity must not hard-code Ubuntu as the acceptance runtime", text.contains("ubuntu"));
        assertFalse("MainActivity must not hard-code the legacy Ubuntu download URL", text.contains("ubuntu-base/releases"));
    }
}
