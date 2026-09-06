package com.alfa.device_ctrl;

import android.view.View;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;

/**
 * Owns one foreground-scoped terminal session independently from Activity view code.
 * It does not claim background persistence or turn Gate 5 pwd into full access.
 */
public final class RuntimeSessionManager implements TerminalSessionClient {
    public interface Listener {
        void onState(String state);
        void onTextChanged();
        void onSessionFinished(int exitStatus);
    }

    private final InteractiveSessionContract contract;
    private final Listener listener;
    private TerminalSession session;
    private boolean promptReady;

    public RuntimeSessionManager(InteractiveSessionContract contract, Listener listener) {
        if (contract == null) throw new IllegalArgumentException("contract is required");
        this.contract = contract;
        this.listener = listener;
    }

    public synchronized boolean start(int columns, int rows, int cellWidthPixels, int cellHeightPixels) {
        if (session != null && session.isRunning()) {
            if (listener != null) listener.onState(promptReady ? "RUNNING" : "PTY_WAITING_FOR_PROMPT");
            return promptReady;
        }
        if (columns < 1 || rows < 1 || !contract.isAuthorizedForInteractiveRuntime()) {
            if (listener != null) listener.onState("BLOCKED");
            return false;
        }
        promptReady = false;
        session = new TerminalSession(
                contract.prootExecutable().getAbsolutePath(),
                contract.hostCwd().getAbsolutePath(),
                contract.prootArguments(),
                contract.environment(),
                2000,
                this);
        session.mSessionName = contract.sessionId();
        session.updateSize(columns, rows, cellWidthPixels, cellHeightPixels);
        OperationEvidence.write(contract, "PTY_CREATED", "PENDING_PROMPT");
        if (listener != null) listener.onState("PTY_CREATED");
        return true;
    }

    public synchronized void attachTo(TerminalView view) {
        if (view == null) throw new IllegalArgumentException("view is required");
        if (session == null) throw new IllegalStateException("session is not started");
        view.attachSession(session);
    }

    public synchronized void stop() {
        if (session != null) {
            OperationEvidence.write(contract, "STOPPING", "REQUESTED");
            session.finishIfRunning();
        }
        if (listener != null) listener.onState("STOPPING");
    }

    public synchronized boolean isRunning() {
        return session != null && session.isRunning();
    }

    public synchronized TerminalSession currentSession() {
        return session;
    }

    @Override public void onTextChanged(TerminalSession changedSession) {
        synchronized (this) {
            if (!promptReady && changedSession == session && changedSession.getEmulator() != null
                    && changedSession.getEmulator().getScreen() != null) {
                String transcript = changedSession.getEmulator().getScreen().getTranscriptText();
                if (transcript.contains("alfa:ubuntu:") || transcript.matches("(?s).*([#$] )$")) {
                    promptReady = true;
                    OperationEvidence.write(contract, "READY", "PROMPT_OBSERVED");
                    if (listener != null) listener.onState("READY");
                }
            }
        }
        if (listener != null) listener.onTextChanged();
    }
    @Override public void onTitleChanged(TerminalSession changedSession) { }
    @Override public synchronized void onSessionFinished(TerminalSession finishedSession) {
        int status = finishedSession.getExitStatus();
        if (session == finishedSession) {
            OperationEvidence.write(contract, "FINISHED", Integer.toString(status));
            session = null;
            promptReady = false;
        }
        if (listener != null) listener.onSessionFinished(status);
    }
    @Override public void onCopyTextToClipboard(TerminalSession session, String text) { }
    @Override public void onPasteTextFromClipboard(TerminalSession session) { }
    @Override public void onBell(TerminalSession session) { }
    @Override public void onColorsChanged(TerminalSession session) { }
    @Override public void onTerminalCursorStateChange(boolean state) { }
    @Override public void setTerminalShellPid(TerminalSession session, int pid) { }
    @Override public Integer getTerminalCursorStyle() { return null; }
    @Override public void logError(String tag, String message) { }
    @Override public void logWarn(String tag, String message) { }
    @Override public void logInfo(String tag, String message) { }
    @Override public void logDebug(String tag, String message) { }
    public synchronized boolean isPromptReady() { return promptReady; }

    public interface CommandListener { void onResult(String output, int exitStatus); }

    public void runRuntimeCommand(String command, CommandListener listener) {
        if (command == null || command.trim().isEmpty()) {
            if (listener != null) listener.onResult("invalid-command", 2);
            return;
        }
        final String requested = command;
        final InteractiveSessionContract activeContract;
        synchronized (this) {
            if (!promptReady || session == null || !session.isRunning() || !contract.isAuthorizedForInteractiveRuntime()) {
                if (listener != null) listener.onResult("runtime-session-not-ready", 126);
                return;
            }
            activeContract = contract;
        }
        new Thread(() -> {
            Process process = null;
            String output = "";
            int status = 126;
            try {
                List<String> argv = new ArrayList<>();
                argv.add(activeContract.prootExecutable().getAbsolutePath());
                argv.add("-0"); argv.add("-r"); argv.add(activeContract.runtimeRoot().getAbsolutePath());
                argv.add("-b"); argv.add("/dev"); argv.add("-b"); argv.add("/proc"); argv.add("-b"); argv.add("/sys");
                argv.add("-w"); argv.add("/root");
                argv.add("/usr/bin/env"); argv.add("-i");
                argv.add("HOME=/root"); argv.add("PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");
                argv.add("TERM=xterm-256color"); argv.add("/bin/sh"); argv.add("-c"); argv.add(requested);
                ProcessBuilder builder = new ProcessBuilder(argv);
                builder.directory(activeContract.hostCwd());
                builder.redirectErrorStream(true);

                // ProcessBuilder.environment() requires key/value entries.
                // The PRoot process itself must receive PROOT_TMP_DIR before start.
                for (String entry : activeContract.environment()) {
                    if (entry == null) continue;
                    int separator = entry.indexOf('=');
                    if (separator <= 0) {
                        throw new IllegalStateException("invalid-runtime-environment-entry");
                    }
                    builder.environment().put(
                            entry.substring(0, separator),
                            entry.substring(separator + 1));
                }

                process = builder.start();
                StringBuilder text = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null && text.length() < 12000) text.append(line).append('\n');
                }
                status = process.waitFor();
                output = text.toString().trim();
            } catch (Exception error) {
                output = error.getClass().getSimpleName() + ":" + String.valueOf(error.getMessage());
                if (process != null) process.destroyForcibly();
            }
            if (listener != null) listener.onResult(output, status);
        }, "alfa-runtime-command").start();
    }

    @Override public void logVerbose(String tag, String message) { }
    @Override public void logStackTraceWithMessage(String tag, String message, Exception e) { }
    @Override public void logStackTrace(String tag, Exception e) { }
}
