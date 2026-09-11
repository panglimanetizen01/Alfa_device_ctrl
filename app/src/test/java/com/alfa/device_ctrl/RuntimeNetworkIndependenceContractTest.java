package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Architectural regression contract: local interactive execution must not acquire a
 * hidden dependency on Android/network transport facilities.
 */
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

    @Test public void networkEvidenceIsDownstreamOfReadySession() throws Exception {
        String main = source("src/main/java/com/alfa/device_ctrl/MainActivity.java");
        int networkMethod = main.indexOf("private void showNetwork()");
        assertTrue("MainActivity network surface missing", networkMethod >= 0);
        int readyGate = main.indexOf("if (!sessionReady())", networkMethod);
        assertTrue("network surface must require an already-ready local session", readyGate > networkMethod);
    }

    @Test public void networkAcquisitionRemainsConfinedToInstaller() throws Exception {
        String installer = source("src/main/java/com/alfa/device_ctrl/RuntimeInstaller.java");
        assertTrue("runtime acquisition must retain explicit URL transport boundary",
                installer.contains("URLConnection") || installer.contains("HttpURLConnection"));
    }
}
