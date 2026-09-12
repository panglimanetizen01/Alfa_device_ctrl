package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Regression contract for the proven runtime-engine-missing installation failure. */
public final class NativeRuntimeExtractionContractTest {
    @Test public void packagedProotEngineMustBeExtractedToNativeLibraryDir() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("app/build.gradle")), StandardCharsets.UTF_8);
        assertTrue("APK must use legacy JNI packaging so libproot.so is extracted to nativeLibraryDir",
                source.contains("useLegacyPackaging true"));
    }
}
