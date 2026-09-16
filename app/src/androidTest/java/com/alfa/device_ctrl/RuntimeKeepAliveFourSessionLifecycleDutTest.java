package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Real Android contract: four independent PTYs survive Activity recreation and reattach to four distinct TerminalViews. */
public final class RuntimeKeepAliveFourSessionLifecycleDutTest {
    private static final String[] SESSION_IDS = {"SESSION_A", "SESSION_B", "SESSION_C", "SESSION_D"};
    private static final String[] RUNTIME_IDS = {"debian", "ubuntu", "alpine", "kali"};
    private Context context;
    private RuntimeSessionManager[] managers;
    private ActivityScenario<StitchOperationalActivity> scenario;

    @Before public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @After public void tearDown() {
        if (scenario != null) scenario.close();
        if (managers == null) return;
        for (RuntimeSessionManager manager : managers) {
            if (manager == null) continue;
            manager.finishForKeepAliveStop();
            RuntimeKeepAliveService.stop(context, manager);
        }
    }

    @Test public void fourIndependentSessionsReattachAfterActivityRecreation() throws Exception {
        assertEquals(4, SESSION_IDS.length);
        assertEquals(4, RUNTIME_IDS.length);
        for (String runtimeId : RUNTIME_IDS) assertNotNull("runtime profile missing: " + runtimeId, RuntimeRegistry.get(runtimeId));

        File vault = new File(context.getFilesDir(), "runtime-vault");
        assertTrue("Gate 7 launch contract is not installed", new File(vault, "gate7-launch.properties").isFile());

        // Provision each runtime through the real Activity startup path. The coordinator invokes the same
        // canonical RuntimeInstaller used by the product; no installer API is called directly by this test.
        scenario = ActivityScenario.launch(StitchOperationalActivity.class);
        for (int i = 0; i < SESSION_IDS.length; i++) {
            final String sessionId = SESSION_IDS[i];
            final String runtimeId = RUNTIME_IDS[i];
            scenario.onActivity(activity -> activity.getPreferences(Context.MODE_PRIVATE).edit().putString("session_id", sessionId).apply());
            scenario.recreate();
            awaitRuntimeReady(vault, runtimeId);
            scenario.onActivity(activity -> assertEquals(runtimeId, activity.getPreferences(Context.MODE_PRIVATE).getString("session_id", "" ).equals(sessionId) ? runtimeId : ""));
        }

        managers = new RuntimeSessionManager[SESSION_IDS.length];
        Set<Integer> pids = new HashSet<>();
        Set<String> runtimeIds = new HashSet<>();
        for (int i = 0; i < SESSION_IDS.length; i++) {
            String sessionId = SESSION_IDS[i];
            RuntimeProfile profile = RuntimeRegistry.get(RUNTIME_IDS[i]);
            File runtime = new File(new File(vault, "runtimes"), profile.id());
            File ready = new File(runtime, "READY.evidence");
            assertTrue("runtime READY evidence is not installed: " + profile.id(), ready.isFile());
            assertTrue("Gate 7 launch contract is not valid for " + profile.id(), Gate6LaunchContract.verify(new File(vault, "gate7-launch.properties"), profile.id()));
            runtimeIds.add(profile.id());
            assertEquals(i + 1, runtimeIds.size());

            managers[i] = new RuntimeSessionManager(contract(profile, sessionId), listener());
            assertTrue("PTY did not start for " + sessionId, managers[i].start(80, 24, 8, 16));
            awaitPrompt(managers[i]);
            TerminalSession session = managers[i].currentSession();
            assertNotNull(session);
            assertTrue("PTY PID missing for " + sessionId, session.getPid() > 0);
            assertTrue("PTY PID collision for " + sessionId, pids.add(session.getPid()));
            write(session, "printf 'G2_" + sessionId + "_" + profile.id() + "\\n'");
            awaitTranscript(session, "G2_" + sessionId + "_" + profile.id());
            RuntimeKeepAliveService.start(context, managers[i]);
        }
        assertEquals(4, runtimeIds.size());

        awaitOwners(SESSION_IDS.length);
        assertEquals(SESSION_IDS.length, RuntimeKeepAliveService.ownersSnapshot().size());
        awaitServiceActive();

        // The four PTYs now exist; recreate the Activity and require all four real TerminalViews to reattach.
        scenario.onActivity(activity -> activity.getPreferences(Context.MODE_PRIVATE).edit().putString("session_id", "SESSION_A").apply());
        scenario.recreate();
        awaitAttachedViews(SESSION_IDS);

        scenario.onActivity(activity -> {
            List<TerminalView> terminals = taggedTerminalViews(activity.getWindow().getDecorView());
            assertEquals("exactly four session-bound terminal views required", 4, terminals.size());
            Set<String> sessionTags = new HashSet<>();
            Set<TerminalSession> attachedSessions = new HashSet<>();
            for (TerminalView terminal : terminals) {
                String tag = String.valueOf(terminal.getTag());
                String sessionId = tag.substring("alfa-runtime-session:".length());
                sessionTags.add(sessionId);
                assertNotNull("terminal view is not attached to a session: " + sessionId, terminal.mTermSession);
                attachedSessions.add(terminal.mTermSession);
                assertEquals("view session name mismatch", sessionId, terminal.mTermSession.mSessionName);
            }
            assertEquals(4, sessionTags.size());
            assertEquals(4, attachedSessions.size());
            for (int i = 0; i < managers.length; i++) {
                RuntimeSessionManager manager = managers[i];
                String sessionId = SESSION_IDS[i];
                String runtimeId = RUNTIME_IDS[i];
                assertTrue("session orphaned after recreation: " + sessionId, manager.isRunning());
                assertTrue("prompt state lost after recreation: " + sessionId, manager.isPromptReady());
                assertNotNull(manager.currentSession());
                assertTrue("reattached session missing from view set: " + sessionId, attachedSessions.contains(manager.currentSession()));
                String transcript = transcript(manager.currentSession());
                String marker = "G2_" + sessionId + "_" + runtimeId;
                assertTrue("session transcript lost marker after recreation: " + marker, transcript.contains(marker));
                for (String otherSessionId : SESSION_IDS) if (!otherSessionId.equals(sessionId)) assertTrue("cross-session marker leaked into " + sessionId, !transcript.contains("G2_" + otherSessionId + "_"));
            }
        });
    }

    private void awaitRuntimeReady(File vault, String runtimeId) {
        File runtime = new File(new File(vault, "runtimes"), runtimeId);
        File ready = new File(runtime, "READY.evidence");
        long deadline = SystemClock.uptimeMillis() + 180000L;
        while (SystemClock.uptimeMillis() < deadline && !ready.isFile()) SystemClock.sleep(500L);
        assertTrue("runtime READY evidence is not installed: " + runtimeId, ready.isFile());
        assertTrue("runtime rootfs missing: " + runtimeId, new File(runtime, "rootfs").isDirectory());
        assertTrue("runtime shell missing: " + runtimeId, new File(runtime, "rootfs/bin/sh").exists());
    }

    private InteractiveSessionContract contract(RuntimeProfile profile, String id) {
        File vault = new File(context.getFilesDir(), "runtime-vault");
        File runtime = new File(new File(vault, "runtimes"), profile.id());
        File cwd = new File(new File(context.getFilesDir(), "session-cwd"), id.toLowerCase());
        if (!cwd.isDirectory() && !cwd.mkdirs()) throw new IllegalStateException("session-cwd-create-failed:" + id);
        return new InteractiveSessionContract("session-" + id.toLowerCase(), "request-g2-" + id.toLowerCase(), "run-g2-" + id.toLowerCase(), profile.id(), new File(runtime, "READY.evidence"), new File(context.getApplicationInfo().nativeLibraryDir, "libproot.so"), new File(runtime, "rootfs"), cwd, new String[]{"HOME=/root", "TERM=xterm-256color", "PS1=alfa:" + profile.id() + ":\\w\\$ ", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "PROOT_TMP_DIR=" + new File(runtime, "proot_tmp").getAbsolutePath()});
    }

    private static RuntimeSessionManager.Listener listener() {
        return new RuntimeSessionManager.Listener() {
            @Override public void onState(String state) { }
            @Override public void onTextChanged() { }
            @Override public void onSessionFinished(int exitStatus) { }
        };
    }

    private static void awaitPrompt(RuntimeSessionManager manager) {
        long deadline = SystemClock.uptimeMillis() + 15000L;
        while (SystemClock.uptimeMillis() < deadline && !manager.isPromptReady()) SystemClock.sleep(50L);
        assertTrue("runtime prompt was not observed", manager.isPromptReady());
    }

    private static void awaitServiceActive() {
        long deadline = SystemClock.uptimeMillis() + 10000L;
        while (SystemClock.uptimeMillis() < deadline && !RuntimeKeepAliveService.isActive()) SystemClock.sleep(50L);
        assertTrue("runtime keep-alive service did not become active", RuntimeKeepAliveService.isActive());
    }

    private static void awaitOwners(int count) {
        long deadline = SystemClock.uptimeMillis() + 5000L;
        while (SystemClock.uptimeMillis() < deadline && RuntimeKeepAliveService.ownersSnapshot().size() != count) SystemClock.sleep(50L);
        assertEquals(count, RuntimeKeepAliveService.ownersSnapshot().size());
    }

    private void awaitAttachedViews(String[] ids) {
        long deadline = SystemClock.uptimeMillis() + 10000L;
        while (SystemClock.uptimeMillis() < deadline) {
            final boolean[] attached = {false};
            scenario.onActivity(activity -> {
                List<TerminalView> views = taggedTerminalViews(activity.getWindow().getDecorView());
                Set<String> tags = new HashSet<>();
                for (TerminalView view : views) if (view.mTermSession != null) tags.add(view.getTag().toString());
                attached[0] = tags.size() == ids.length;
            });
            if (attached[0]) return;
            SystemClock.sleep(100L);
        }
        scenario.onActivity(activity -> assertEquals(ids.length, taggedTerminalViews(activity.getWindow().getDecorView()).size()));
    }

    private static List<TerminalView> taggedTerminalViews(View root) {
        List<TerminalView> result = new ArrayList<>();
        collectTagged(root, result);
        return result;
    }

    private static void collectTagged(View view, List<TerminalView> result) {
        if (view instanceof TerminalView && String.valueOf(view.getTag()).startsWith("alfa-runtime-session:")) result.add((TerminalView)view);
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup)view;
        for (int i = 0; i < group.getChildCount(); i++) collectTagged(group.getChildAt(i), result);
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
