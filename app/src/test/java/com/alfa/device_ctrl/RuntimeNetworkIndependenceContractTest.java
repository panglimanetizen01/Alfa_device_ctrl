package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Architectural regression contract for the network acquisition/execution boundary and Stitch evidence surface. */
public final class RuntimeNetworkIndependenceContractTest {
    private static String source(String path) throws Exception {
        File file = new File(path);
        assertTrue("missing source file: " + path, file.isFile());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void localExecutionPlaneContainsNoNetworkTransportDependency() throws Exception {
        String session = source("src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java");
        String contract = source("src/main/java/com/alfa/device_ctrl/InteractiveSessionContract.java");
        String forbidden = "android.net. java.net.URL java.net.HttpURLConnection java.net.Socket java.net.InetAddress "
                + "ConnectivityManager NetworkCapabilities LinkProperties VpnService";
        for (String token : forbidden.split(" ")) {
            assertFalse("local execution plane must not depend on network transport API: " + token,
                    session.contains("import " + token) || contract.contains("import " + token));
        }
    }

    @Test public void StitchNetworkEvidenceUsesAndroidNetworkEvidenceBoundary() throws Exception {
        String panel = source("src/main/java/com/alfa/device_ctrl/AlfaNetworkPanel.java");
        String hooks = source("src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java");
        assertTrue(panel.contains("ConnectivityManager"));
        assertTrue(panel.contains("NetworkCapabilities"));
        assertTrue(panel.contains("LinkProperties"));
        assertTrue(panel.contains("getActiveNetwork()"));
        assertTrue(panel.contains("getDnsServers()"));
        assertTrue(panel.contains("/proc/net/tcp"));
        assertTrue(hooks.contains("case NETWORK: return network(a)"));
    }

    @Test public void networkAcquisitionRemainsConfinedToInstaller() throws Exception {
        String installer = source("src/main/java/com/alfa/device_ctrl/RuntimeInstaller.java");
        assertTrue("runtime acquisition must retain explicit URL transport boundary",
                installer.contains("URLConnection") || installer.contains("HttpURLConnection"));
    }
}
