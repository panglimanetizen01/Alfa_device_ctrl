package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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

/** Real DUT contract: four independent PTYs survive Activity recreation and reattach to four distinct TerminalViews. */
public final class RuntimeKeepAliveFourSessionLifecycleDutTest {
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
        RuntimeProfile profile = RuntimeSelection.profile(RuntimeSelection.DEFAULT_RUNTIME_ID);
        assertNotNull("default runtime profile missing", profile);
        File vault = new File(context.getFilesDir(), "runtime-vault");
        File runtime = new File(new File(vault, "runtimes"), profile.id());
        assertTrue("DUT runtime READY evidence is not installed", new File(runtime, "READY.evidence").isFile());
        assertTrue("DUT Gate 7 launch contract is not installed", Gate6LaunchContract.verify(new File(vault, "gate7-launch.properties"), profile.id()));

        String[] ids = {"SESSION_A", "SESSION_B", "SESSION_C", "SESSION_D"};
        managers = new RuntimeSessionManager[ids.length];
        Set<Integer> pids = new HashSet<>();
        for (int i = 0; i < ids.length; i++) {
            managers[i] = new RuntimeSessionManager(contract(profile, ids[i]), listener());
            assertTrue("PTY did not start for " + ids[i], managers[i].start(80, 24, 8, 16));
            awaitPrompt(managers[i]);
            TerminalSession session = managers[i].currentSession();
            assertNotNull(session);
            assertTrue("PTY PID missing for " + ids[i], session.getPid() > 0);
            assertTrue("PTY PID collision for " + ids[i], pids.add(session.getPid()));
            write(session, "printf 'G2_" + ids[i] + "\\n'");
            awaitTranscript(session, "G2_" + ids[i]);
        }

        awaitOwners(ids.length);
        assertEquals(ids.length, RuntimeKeepAliveService.ownersSnapshot().size());
        awaitServiceActive();

        scenario = ActivityScenario.launch(StitchOperationalActivity.class);
        awaitAttachedViews(ids);
        scenario.recreate();
        awaitAttachedViews(ids);

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
            for (RuntimeSessionManager manager : managers) {
                assertTrue("session orphaned after recreation", manager.isRunning());
                assertTrue("prompt state lost after recreation", manager.isPromptReady());
                assertNotNull(manager.currentSession());
                assertTrue("reattached session missing from view set", attachedSessions.contains(manager.currentSession()));
                String marker = "G2_" + manager.currentSession().mSessionName;
                assertTrue("session transcript lost marker after recreation", transcript(manager.currentSession()).contains(marker));
            }
        });
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

    private static void awaitAttachedViews(String[] ids) {
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
