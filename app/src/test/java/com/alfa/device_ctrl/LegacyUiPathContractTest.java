package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

/** Locks removal of the retired pre-Stitch launcher/UI source path without touching reference assets. */
public final class LegacyUiPathContractTest {
    @Test public void retiredLauncherSourcesAreAbsent() {
        Path sourceRoot = Paths.get("src/main/java/com/alfa/device_ctrl");
        assertFalse("retired MainActivity must not remain", Files.exists(sourceRoot.resolve("MainActivity.java")));
        assertFalse("retired StitchOperationalActivityV2 must not remain", Files.exists(sourceRoot.resolve("StitchOperationalActivityV2.java")));
    }

    @Test public void canonicalLauncherSourceRemainsPresent() {
        Path sourceRoot = Paths.get("src/main/java/com/alfa/device_ctrl");
        assertTrue("canonical StitchOperationalActivity must remain", Files.exists(sourceRoot.resolve("StitchOperationalActivity.java")));
    }

    @Test public void legacyUiStringsAreAbsent() throws Exception {
        Path resources = Paths.get("src/main/res/values/strings.xml");
        String text = new String(Files.readAllBytes(resources), StandardCharsets.UTF_8);
        assertFalse(text.contains("start_verified_session"));
        assertFalse(text.contains("session_blocked"));
        assertFalse(text.contains("resource_alert_title"));
        assertFalse(text.contains("kill_process"));
        assertFalse(text.contains("ALFA_OS"));
        assertTrue(text.contains("Alfa Device Ctrl"));
    }
}
