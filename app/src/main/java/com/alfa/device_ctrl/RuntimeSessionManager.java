package com.alfa.device_ctrl;

import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;

/** Owns one foreground-scoped terminal session and emits evidence for the exact G7 contract. */
public final class RuntimeSessionManager implements TerminalSessionClient {
    public interface Listener {
        void onState(String state);
        void onTextChanged();
        void onSessionFinished(int exitStatus);
    }

    private static final long COMMAND_TIMEOUT_MS = 30000L;
    private static final int MAX_COMMAND_OUTPUT_CHARS = 12000;

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
        OperationEvidence.write(contract, "PTY_CREATED", "PENDING_PROMPT", session.getPid());
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
            OperationEvidence.write(contract, "STOPPING", "REQUESTED", session.getPid());
            session.finishIfRunning();
        }
        if (listener != null) listener.onState("STOPPING");
    }

    public synchronized boolean isRunning() {
        return session != null && session.isRunning();
    }

    public synchronized TerminalSession currentSession() { return session; }

    @Override public void onTextChanged(TerminalSession changedSession) {
        synchronized (this) {
            if (!promptReady && changedSession == session && changedSession.getEmulator() != null
                    && changedSession.getEmulator().getScreen() != null) {
                String transcript = changedSession.getEmulator().getScreen().getTranscriptText();
                if (transcript.contains("alfa:ubuntu:") || transcript.matches("(?s).*([#$] )$")) {
                    promptReady = true;
                    OperationEvidence.write(contract, "READY", "PROMPT_OBSERVED", changedSession.getPid());
                    if (listener != null) listener.onState("READY");
                }
            }
        }
        if (listener != null) listener.onTextChanged();
    }

    @Override public synchronized void onSessionFinished(TerminalSession finishedSession) {
        int status = finishedSession.getExitStatus();
        if (session == finishedSession) {
            OperationEvidence.write(contract, "FINISHED", Integer.toString(status), finishedSession.getPid());
            session = null;
            promptReady = false;
        }
        if (listener != null) listener.onSessionFinished(status);
    }

    @Override public void onTitleChanged(TerminalSession changedSession) { }
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

    static final class CommandResult {
        final String output;
        final int exitStatus;
        final boolean timedOut;

        CommandResult(String output, int exitStatus, boolean timedOut) {
            this.output = output;
            this.exitStatus = exitStatus;
            this.timedOut = timedOut;
        }
    }

    private static String findExecutable(String... candidates) {
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.isFile() && file.canExecute()) return candidate;
        }
        return null;
    }

    private static String requireProcessGroupTool(String... candidates) {
        String executable = findExecutable(candidates);
        if (executable == null) throw new IllegalStateException("process-group-tool-unavailable");
        return executable;
    }

    private static ProcessBuilder withProcessGroup(ProcessBuilder command) {
        String setsid = requireProcessGroupTool("/system/bin/setsid", "/usr/bin/setsid", "/bin/setsid");
        List<String> argv = new ArrayList<>();
        argv.add(setsid);
        argv.addAll(command.command());
        ProcessBuilder grouped = new ProcessBuilder(argv);
        grouped.directory(command.directory());
        grouped.environment().clear();
        grouped.environment().putAll(command.environment());
        grouped.redirectErrorStream(command.redirectErrorStream());
        return grouped;
    }

    private static void killProcessGroup(long pid) throws Exception {
        if (pid <= 0 || pid > Integer.MAX_VALUE) throw new IllegalArgumentException("invalid-process-group-pid");
        String kill = requireProcessGroupTool("/system/bin/kill", "/usr/bin/kill", "/bin/kill");
        Process killer = new ProcessBuilder(kill, "-KILL", "-" + pid)
                .redirectErrorStream(true)
                .start();
        if (!killer.waitFor(2, TimeUnit.SECONDS)) killer.destroyForcibly();
        if (killer.exitValue() != 0) throw new IllegalStateException("process-group-kill-failed:" + killer.exitValue());
    }

    static CommandResult executeProcess(ProcessBuilder builder, long timeoutMs, int maxOutputChars) throws Exception {
        ProcessBuilder groupedBuilder = withProcessGroup(builder);
        Process process = groupedBuilder.start();
        long processGroupId = process.pid();
        StringBuilder text = new StringBuilder();
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buffer = new char[4096];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    synchronized (text) {
                        if (text.length() >= maxOutputChars) continue;
                        int remaining = maxOutputChars - text.length();
                        text.append(buffer, 0, Math.min(count, remaining));
                    }
                }
            } catch (Exception ignored) {
                // Process termination is reported by the owner thread.
            }
        }, "alfa-runtime-command-output");
        readerThread.start();

        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            Exception cleanupFailure = null;
            try {
                killProcessGroup(processGroupId);
            } catch (Exception error) {
                cleanupFailure = error;
                process.destroyForcibly();
            }
            process.waitFor(2, TimeUnit.SECONDS);
            readerThread.join(2000);
            if (cleanupFailure != null) throw cleanupFailure;
            return new CommandResult(text.toString().trim(), 124, true);
        }

        readerThread.join(2000);
        return new CommandResult(text.toString().trim(), process.exitValue(), false);
    }

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
            int status = 126;
            String output;
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
                for (String entry : activeContract.environment()) {
                    if (entry == null) continue;
                    int separator = entry.indexOf('=');
                    if (separator <= 0) throw new IllegalStateException("invalid-runtime-environment-entry");
                    builder.environment().put(entry.substring(0, separator), entry.substring(separator + 1));
                }
                CommandResult result = executeProcess(builder, COMMAND_TIMEOUT_MS, MAX_COMMAND_OUTPUT_CHARS);
                status = result.exitStatus;
                output = result.timedOut ? "runtime-command-timeout\n" + result.output : result.output;
            } catch (Exception error) {
                output = error.getClass().getSimpleName() + ":" + String.valueOf(error.getMessage());
            }
            if (listener != null) listener.onResult(output, status);
        }, "alfa-runtime-command").start();
    }

    @Override public void logVerbose(String tag, String message) { }
    @Override public void logStackTraceWithMessage(String tag, String message, Exception e) { }
    @Override public void logStackTrace(String tag, Exception e) { }
}
