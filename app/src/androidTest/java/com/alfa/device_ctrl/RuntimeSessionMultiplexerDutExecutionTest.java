package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.terminal.TerminalSession;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

/** Real DUT contract: two independent RuntimeSessionManagers must own two live Android PTYs. */
public final class RuntimeSessionMultiplexerDutExecutionTest {
    private RuntimeSessionMultiplexer multiplexer;

    @After public void tearDown() {
        if (multiplexer == null) return;
        for (String id : multiplexer.sessionIds()) {
            RuntimeSessionManager manager = multiplexer.getSession(id);
            if (manager != null) manager.finishForKeepAliveStop();
        }
        multiplexer.removeAll();
    }

    @Test public void twoSessionsHaveIndependentPtyPidShellAndMarkers() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RuntimeProfile profile = RuntimeSelection.profile(RuntimeSelection.DEFAULT_RUNTIME_ID);
        assertNotNull("default runtime profile missing", profile);

        File vault = new File(context.getFilesDir(), "runtime-vault");
        File runtime = new File(new File(vault, "runtimes"), profile.id());
        File ready = new File(runtime, "READY.evidence");
        File launch = new File(vault, "gate7-launch.properties");
        Assume.assumeTrue("DUT runtime READY evidence is not installed", ready.isFile());
        Assume.assumeTrue("DUT Gate 7 launch contract is not installed", Gate6LaunchContract.verify(launch, profile.id()));

        multiplexer = new RuntimeSessionMultiplexer();
        RuntimeSessionManager a = multiplexer.createSession("SESSION_A", contract(context, profile, "SESSION_A"), listener());
        RuntimeSessionManager b = multiplexer.createSession("SESSION_B", contract(context, profile, "SESSION_B"), listener());

        assertNotEquals(a, b);
        assertNotEquals(a.currentSession(), b.currentSession());

        assertTrue("session A PTY did not start", multiplexer.startSession("SESSION_A", 80, 24, 8, 16));
        assertTrue("session B PTY did not start", multiplexer.startSession("SESSION_B", 80, 24, 8, 16));
        awaitPrompt(a);
        awaitPrompt(b);

        TerminalSession ptyA = a.currentSession();
        TerminalSession ptyB = b.currentSession();
        assertNotNull(ptyA);
        assertNotNull(ptyB);
        assertNotEquals("PTY objects must be independent", ptyA, ptyB);
        assertTrue("PTY A PID missing", ptyA.getPid() > 0);
        assertTrue("PTY B PID missing", ptyB.getPid() > 0);
        assertNotEquals("PTY child processes must be independent", ptyA.getPid(), ptyB.getPid());

        write(ptyA, "printf 'A_MARKER\\nSHELL_A=%s\\n' $$");
        write(ptyB, "printf 'B_MARKER\\nSHELL_B=%s\\n' $$");
        awaitTranscript(ptyA, "A_MARKER");
        awaitTranscript(ptyB, "B_MARKER");

        String transcriptA = transcript(ptyA);
        String transcriptB = transcript(ptyB);
        assertTrue(transcriptA.contains("A_MARKER"));
        assertTrue(transcriptB.contains("B_MARKER"));
        assertTrue("marker B leaked into session A", !transcriptA.contains("B_MARKER"));
        assertTrue("marker A leaked into session B", !transcriptB.contains("A_MARKER"));

        write(ptyA, "printf 'A_MARKER_2\\n'");
        write(ptyB, "printf 'B_MARKER_2\\n'");
        awaitTranscript(ptyA, "A_MARKER_2");
        awaitTranscript(ptyB, "B_MARKER_2");
        assertTrue(!transcript(ptyA).contains("B_MARKER_2"));
        assertTrue(!transcript(ptyB).contains("A_MARKER_2"));

        multiplexer.setActiveSessionId("SESSION_A");
        assertEquals("SESSION_A", multiplexer.activeSessionId());
        assertEquals(a, multiplexer.activeSession());
        multiplexer.setActiveSessionId("SESSION_B");
        assertEquals("SESSION_B", multiplexer.activeSessionId());
        assertEquals(b, multiplexer.activeSession());
        multiplexer.setActiveSessionId("SESSION_A");
        assertEquals(a, multiplexer.activeSession());
        multiplexer.setActiveSessionId("SESSION_B");
        assertEquals(b, multiplexer.activeSession());
    }

    private static InteractiveSessionContract contract(Context context, RuntimeProfile profile, String id) {
        File vault = new File(context.getFilesDir(), "runtime-vault");
        File runtime = new File(new File(vault, "runtimes"), profile.id());
        File cwd = new File(new File(context.getFilesDir(), "session-cwd"), id.toLowerCase());
        if (!cwd.isDirectory() && !cwd.mkdirs()) throw new IllegalStateException("session-cwd-create-failed:" + id);
        File proot = new File(context.getApplicationInfo().nativeLibraryDir, "libproot.so");
        return new InteractiveSessionContract("session-" + id.toLowerCase(), "request-dut-" + id.toLowerCase(), "run-dut-" + id.toLowerCase(), profile.id(), new File(runtime, "READY.evidence"), proot, new File(runtime, "rootfs"), cwd, new String[]{"HOME=/root", "TERM=xterm-256color", "PS1=alfa:" + profile.id() + ":\\w\\$ ", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "PROOT_TMP_DIR=" + new File(runtime, "proot_tmp").getAbsolutePath()});
    }

    private static RuntimeSessionManager.Listener listener() {
        return new RuntimeSessionManager.Listener() {
            @Override public void onState(String state) { }
            @Override public void onTextChanged() { }
            @Override public void onSessionFinished(int exitStatus) { }
        };
    }

    private static void awaitPrompt(RuntimeSessionManager manager) {
        long deadline = SystemClock.uptimeMillis() + 10000L;
        while (SystemClock.uptimeMillis() < deadline && !manager.isPromptReady()) SystemClock.sleep(50L);
        assertTrue("runtime prompt was not observed", manager.isPromptReady());
    }

    private static void write(TerminalSession session, String command) {
        byte[] bytes = (command + "\n").getBytes(StandardCharsets.UTF_8);
        session.write(bytes, 0, bytes.length);
    }

    private static void awaitTranscript(TerminalSession session, String marker) {
        long deadline = SystemClock.uptimeMillis() + 10000L;
        while (SystemClock.uptimeMillis() < deadline && !transcript(session).contains(marker)) SystemClock.sleep(50L);
        assertTrue("missing marker " + marker + " in transcript: " + transcript(session), transcript(session).contains(marker));
    }

    private static String transcript(TerminalSession session) {
        if (session == null || session.getEmulator() == null || session.getEmulator().getScreen() == null) return "";
        return session.getEmulator().getScreen().getTranscriptText();
    }
}
