package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Runtime substrate proof: the embedded terminal path must create a real Android PTY, not only render a view. */
@RunWith(AndroidJUnit4.class)
public final class TerminalSessionPtyExecutionTest {
    private TerminalSession session;

    @After public void tearDown() {
        if (session != null) session.finishIfRunning();
    }

    @Test public void createsPtyAndExecutesRealChildProcess() {
        InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtomicBoolean finished = new AtomicBoolean(false);
        AtomicReference<String> transcript = new AtomicReference<>("");
        TerminalSessionClient client = new TerminalSessionClient() {
            @Override public void onTextChanged(TerminalSession changed) {
                if (changed.getEmulator() != null && changed.getEmulator().getScreen() != null) {
                    transcript.set(changed.getEmulator().getScreen().getTranscriptText());
                }
            }
            @Override public void onSessionFinished(TerminalSession changed) { finished.set(true); }
            @Override public void onTitleChanged(TerminalSession changed) { }
            @Override public void onCopyTextToClipboard(TerminalSession changed, String text) { }
            @Override public void onPasteTextFromClipboard(TerminalSession changed) { }
            @Override public void onBell(TerminalSession changed) { }
            @Override public void onColorsChanged(TerminalSession changed) { }
            @Override public void onTerminalCursorStateChange(boolean state) { }
            @Override public void setTerminalShellPid(TerminalSession changed, int pid) { }
            @Override public Integer getTerminalCursorStyle() { return null; }
            @Override public void logError(String tag, String message) { }
            @Override public void logWarn(String tag, String message) { }
            @Override public void logInfo(String tag, String message) { }
            @Override public void logDebug(String tag, String message) { }
            @Override public void logVerbose(String tag, String message) { }
            @Override public void logStackTraceWithMessage(String tag, String message, Exception e) { }
            @Override public void logStackTrace(String tag, Exception e) { }
        };
        session = new TerminalSession("/system/bin/sh", "/", new String[]{"/system/bin/sh", "-c", "printf 'ALFA_PTY_EXECUTION_OK\\n'"}, new String[]{"TERM=dumb"}, 2000, client);
        session.updateSize(80, 24, 8, 16);
        assertTrue("PTY child was not created", session.getPid() > 0);

        long deadline = SystemClock.uptimeMillis() + 5000L;
        while (SystemClock.uptimeMillis() < deadline && !finished.get()) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            SystemClock.sleep(25L);
        }
        assertTrue("PTY child did not finish", finished.get());
        assertTrue("PTY transcript did not contain real child output: " + transcript.get(), transcript.get().contains("ALFA_PTY_EXECUTION_OK"));
    }
}
