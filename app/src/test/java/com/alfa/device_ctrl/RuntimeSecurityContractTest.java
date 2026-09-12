package com.alfa.device_ctrl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Static regression contracts for the runtime security boundary. */
public final class RuntimeSecurityContractTest {
    @Test public void hostStorageProofActivityIsNotExternallyExported() throws Exception {
        String manifest = new String(Files.readAllBytes(Paths.get("src/main/AndroidManifest.xml")), StandardCharsets.UTF_8);
        int activity = manifest.indexOf("HostStorageDeviceProofActivity");
        assertTrue("Host storage proof activity missing", activity >= 0);
        int end = manifest.indexOf("</activity>", activity);
        assertTrue("Host storage proof activity block missing", end > activity);
        String block = manifest.substring(activity, end);
        assertTrue("host storage proof must be internal-only", block.contains("android:exported=\"false\""));
        assertFalse("host storage proof must not expose a custom external intent filter", block.contains("HOST_STORAGE_PROOF"));
        assertTrue("cleartext traffic must be disabled", manifest.contains("android:usesCleartextTraffic=\"false\""));
    }

    @Test public void runtimeDoesNotBindHostSysTree() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/com/alfa/device_ctrl/RuntimeProfile.java")), StandardCharsets.UTF_8);
        int method = source.indexOf("defaultProotArguments");
        assertTrue("runtime argument builder missing", method >= 0);
        String contract = source.substring(method, Math.min(source.length(), method + 900));
        assertTrue("runtime must retain /dev for PTY compatibility", contract.contains("\"/dev\""));
        assertTrue("runtime must retain /proc for process evidence", contract.contains("\"/proc\""));
        assertFalse("host /sys must not be bound into the guest", contract.contains("\"/sys\""));
    }

    @Test public void runtimeStartAppliesNoNewPrivilegesBeforeCreatingPty() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java")), StandardCharsets.UTF_8);
        int start = source.indexOf("public synchronized boolean start(");
        int security = source.indexOf("applyRuntimeSecurityBoundary();", start);
        int pty = source.indexOf("new TerminalSession(", start);
        assertTrue("runtime start missing", start >= 0);
        assertTrue("runtime security boundary missing", security > start);
        assertTrue("PTY creation missing", pty > security);
        assertTrue("no-new-privileges syscall missing", source.contains("OsConstants.PR_SET_NO_NEW_PRIVS"));
        assertTrue("no-new-privileges must request value 1", source.contains("Os.prctl(OsConstants.PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0)"));
    }
}
