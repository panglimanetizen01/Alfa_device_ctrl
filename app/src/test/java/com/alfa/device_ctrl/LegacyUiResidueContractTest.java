package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Hard gate preventing the retired legacy product label from returning to the application resources. */
public final class LegacyUiResidueContractTest {
    @Test public void retiredAlfaOsResourceIsAbsent() throws Exception {
        String resources = new String(Files.readAllBytes(Paths.get("src/main/res/values/strings.xml")), StandardCharsets.UTF_8);
        String retiredKey = "ALFA" + "_OS";
        String retiredResourceKey = "alfa" + "_os";
        assertFalse("retired legacy UI marker must not remain in resources", resources.contains(retiredKey));
        assertFalse("retired legacy resource key must not remain", resources.contains(retiredResourceKey));
        assertTrue("canonical application name must remain", resources.contains("Alfa Device Ctrl"));
    }
}
