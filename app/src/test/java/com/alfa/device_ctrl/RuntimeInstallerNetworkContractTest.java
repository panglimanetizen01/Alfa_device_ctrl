package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public final class RuntimeInstallerNetworkContractTest {
    @Test public void resolverUsesActiveAndroidNetworkDns() throws Exception {
        File source = new File("app/src/main/java/com/alfa/device_ctrl/RuntimeInstaller.java");
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        assertFalse(text.contains("nameserver 1.1.1.1"));
        assertFalse(text.contains("nameserver 8.8.8.8"));
        assertTrue(text.contains("getActiveNetwork()"));
        assertTrue(text.contains("getLinkProperties(active)"));
        assertTrue(text.contains("getDnsServers()"));
        assertTrue(text.contains("active-network-dns-unavailable"));
    }
}