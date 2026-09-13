package com.alfa.device_ctrl;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

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

/** Owns one runtime session; the foreground service owns its process lifetime when the Activity stops. */
public final class RuntimeSessionManager implements TerminalSessionClient {
    public interface Listener { void onState(String state); void onTextChanged(); void onSessionFinished(int exitStatus); }
    private static final long COMMAND_TIMEOUT_MS = 30000L;
    private static final int MAX_COMMAND_OUTPUT_CHARS = 12000;
    private final InteractiveSessionContract contract;
    private volatile Listener listener;
    private TerminalSession session;
    private boolean promptReady;

    public RuntimeSessionManager(InteractiveSessionContract contract, Listener listener) { if (contract == null) throw new IllegalArgumentException("contract is required"); this.contract = contract; this.listener = listener; }

    public synchronized boolean rebindListener(Listener newListener) {
        listener = newListener;
        if (session == null || !session.isRunning()) return false;
        notifyState(promptReady ? "RUNNING" : "PTY_WAITING_FOR_PROMPT");
        return true;
    }

    private void notifyState(String event) {
        Listener current = listener;
        if (current != null) current.onState(SessionUiState.resolve(event).name());
    }

    public synchronized boolean start(int columns, int rows, int cellWidthPixels, int cellHeightPixels) {
        if (session != null && session.isRunning()) { notifyState(promptReady ? "RUNNING" : "PTY_WAITING_FOR_PROMPT"); return promptReady; }
        if (columns < 1 || rows < 1 || !contract.isAuthorizedForInteractiveRuntime()) { notifyState("BLOCKED"); return false; }
        promptReady = false;
        try {
            RuntimeKeepAliveService.start(AlfaApplication.getInstance(), this);
            session = new TerminalSession(contract.prootExecutable().getAbsolutePath(), contract.hostCwd().getAbsolutePath(), contract.prootArguments(), contract.environment(), 2000, this);
            session.mSessionName = contract.sessionId();
            session.updateSize(columns, rows, cellWidthPixels, cellHeightPixels);
        } catch (RuntimeException error) {
            if (session != null) session.finishIfRunning();
            session = null;
            promptReady = false;
            RuntimeKeepAliveService.stop(AlfaApplication.getInstance(), this);
            notifyState("BLOCKED_PTY_START");
            return false;
        }
        OperationEvidence.write(contract, "PTY_CREATED", "PENDING_PROMPT", session.getPid());
        notifyState("PTY_CREATED");
        return true;
    }

    public synchronized void attachTo(TerminalView view) { if (view == null) throw new IllegalArgumentException("view is required"); if (session == null) throw new IllegalStateException("session is not started"); view.attachSession(session); }

    public void stop() {
        synchronized (this) { if (session == null || !session.isRunning()) return; }
        if (RuntimeKeepAliveService.isActivityPauseInProgress()) { notifyState("BACKGROUND_SESSION_PRESERVED"); return; }
        if (AlfaApplication.hasVisibleActivity()) { finishNow(); return; }
        new Handler(Looper.getMainLooper()).postDelayed(() -> { if (!AlfaApplication.hasVisibleActivity()) { synchronized (RuntimeSessionManager.this) { if (session != null && session.isRunning()) notifyState("BACKGROUND_SESSION_PRESERVED"); } } else finishNow(); }, 250L);
    }

    private void finishNow() {
        synchronized (this) { if (session != null) { OperationEvidence.write(contract, "STOPPING", "REQUESTED", session.getPid()); session.finishIfRunning(); } notifyState("STOPPING"); }
        RuntimeKeepAliveService.stop(AlfaApplication.getInstance(), this);
    }

    void finishForKeepAliveStop() { synchronized (this) { if (session != null && session.isRunning()) { OperationEvidence.write(contract, "STOPPING", "FOREGROUND_SERVICE_STOP", session.getPid()); session.finishIfRunning(); } } }
    public synchronized boolean isRunning() { return session != null && session.isRunning(); }
    public synchronized TerminalSession currentSession() { return session; }

    @Override public void onTextChanged(TerminalSession changedSession) {
        synchronized (this) {
            if (!promptReady && changedSession == session && changedSession.getEmulator() != null && changedSession.getEmulator().getScreen() != null) {
                String transcript = changedSession.getEmulator().getScreen().getTranscriptText();
                String runtimePrompt = "alfa:" + contract.runtimeId() + ":";
                if (transcript.contains(runtimePrompt)) { promptReady = true; OperationEvidence.write(contract, "READY", "PROMPT_OBSERVED", changedSession.getPid()); notifyState("READY"); }
            }
        }
        Listener current = listener; if (current != null) current.onTextChanged();
    }

    @Override public synchronized void onSessionFinished(TerminalSession finishedSession) { int status = finishedSession.getExitStatus(); if (session == finishedSession) { OperationEvidence.write(contract, "FINISHED", Integer.toString(status), finishedSession.getPid()); session = null; promptReady = false; RuntimeKeepAliveService.stop(AlfaApplication.getInstance(), this); } Listener current = listener; if (current != null) current.onSessionFinished(status); }
    @Override public void onTitleChanged(TerminalSession changedSession) { }
    @Override public void onCopyTextToClipboard(TerminalSession session, String text) {
        ClipboardManager clipboard = clipboardManager();
        if (clipboard == null) return;
        TerminalClipboardBridge.copy(new TerminalClipboardBridge.ClipboardPort() {
            @Override public void setText(String value) { clipboard.setPrimaryClip(ClipData.newPlainText("Alfa terminal", value)); }
            @Override public String getText() { return clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null ? clipboard.getPrimaryClip().getItemAt(0).coerceToText(AlfaApplication.getInstance()).toString() : null; }
        }, text);
    }
    @Override public void onPasteTextFromClipboard(TerminalSession session) {
        ClipboardManager clipboard = clipboardManager();
        if (clipboard == null || session == null || session.getEmulator() == null) return;
        TerminalClipboardBridge.paste(new TerminalClipboardBridge.ClipboardPort() {
            @Override public void setText(String value) { clipboard.setPrimaryClip(ClipData.newPlainText("Alfa terminal", value)); }
            @Override public String getText() { return clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null ? clipboard.getPrimaryClip().getItemAt(0).coerceToText(AlfaApplication.getInstance()).toString() : null; }
        }, text -> session.getEmulator().paste(text));
    }
    private ClipboardManager clipboardManager() {
        Context context = AlfaApplication.getInstance();
        return context == null ? null : (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }
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
    static final class CommandResult { final String output; final int exitStatus; final boolean timedOut; CommandResult(String output, int exitStatus, boolean timedOut) { this.output = output; this.exitStatus = exitStatus; this.timedOut = timedOut; } }

    private static String findExecutable(String... candidates) { for (String candidate : candidates) { File file = new File(candidate); if (file.isFile() && file.canExecute()) return candidate; } return null; }
    private static String requireProcessGroupTool(String... candidates) { String executable = findExecutable(candidates); if (executable == null) throw new IllegalStateException("process-group-tool-unavailable"); return executable; }
    private static String shellQuote(String value) { return "'" + value.replace("'", "'\\''") + "'"; }

    private static ProcessBuilder withProcessGroup(ProcessBuilder command, File pidFile) {
        String setsid = requireProcessGroupTool("/system/bin/setsid", "/usr/bin/setsid", "/bin/setsid"); String shell = requireProcessGroupTool("/system/bin/sh", "/usr/bin/sh", "/bin/sh");
        List<String> argv = new ArrayList<>(); argv.add(setsid); argv.add(shell); argv.add("-c"); argv.add("echo $$ > " + shellQuote(pidFile.getAbsolutePath()) + "; exec \"$@\""); argv.add("alfa-process-group"); argv.addAll(command.command());
        ProcessBuilder grouped = new ProcessBuilder(argv); grouped.directory(command.directory()); grouped.environment().clear(); grouped.environment().putAll(command.environment()); grouped.redirectErrorStream(command.redirectErrorStream()); return grouped;
    }

    private static long readProcessGroupId(File pidFile, long timeoutMs) throws Exception { long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs); while (System.nanoTime() < deadline) { if (pidFile.isFile()) { String value = new String(java.nio.file.Files.readAllBytes(pidFile.toPath()), StandardCharsets.US_ASCII).trim(); try { long pid = Long.parseLong(value); if (pid > 0 && pid <= Integer.MAX_VALUE) return pid; } catch (NumberFormatException ignored) { } } Thread.sleep(10); } throw new IllegalStateException("process-group-pid-unavailable"); }
    private static void killProcessGroup(long pid) throws Exception { if (pid <= 0 || pid > Integer.MAX_VALUE) throw new IllegalArgumentException("invalid-process-group-pid"); String kill = requireProcessGroupTool("/system/bin/kill", "/usr/bin/kill", "/bin/kill"); Process killer = new ProcessBuilder(kill, "-KILL", "--", "-" + pid).redirectErrorStream(true).start(); if (!killer.waitFor(2, TimeUnit.SECONDS)) killer.destroyForcibly(); if (killer.exitValue() != 0) throw new IllegalStateException("process-group-kill-failed:" + killer.exitValue()); }

    static CommandResult executeProcess(ProcessBuilder builder, long timeoutMs, int maxOutputChars) throws Exception {
        File pidFile = File.createTempFile("alfa-process-group-", ".pid"); if (!pidFile.delete()) throw new IllegalStateException("process-group-pid-file-create-failed"); Process process = null;
        try {
            ProcessBuilder groupedBuilder = withProcessGroup(builder, pidFile); process = groupedBuilder.start(); long processGroupId = readProcessGroupId(pidFile, 2000); StringBuilder text = new StringBuilder(); Process activeProcess = process;
            Thread readerThread = new Thread(() -> { try (BufferedReader reader = new BufferedReader(new InputStreamReader(activeProcess.getInputStream(), StandardCharsets.UTF_8))) { char[] buffer = new char[4096]; int count; while ((count = reader.read(buffer)) != -1) synchronized (text) { if (text.length() >= maxOutputChars) continue; int remaining = maxOutputChars - text.length(); text.append(buffer, 0, Math.min(count, remaining)); } } catch (Exception ignored) { } }, "alfa-runtime-command-output");
            readerThread.start(); boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) { Exception cleanupFailure = null; try { killProcessGroup(processGroupId); } catch (Exception error) { cleanupFailure = error; process.destroyForcibly(); } process.waitFor(2, TimeUnit.SECONDS); readerThread.join(2000); if (cleanupFailure != null) throw cleanupFailure; return new CommandResult(text.toString().trim(), 124, true); }
            readerThread.join(2000); return new CommandResult(text.toString().trim(), process.exitValue(), false);
        } finally { if (process != null && process.isAlive()) process.destroyForcibly(); if (pidFile.exists()) pidFile.delete(); }
    }

    public void runRuntimeCommand(String command, CommandListener listener) {
        if (command == null || command.trim().isEmpty()) { if (listener != null) listener.onResult("invalid-command", 2); return; }
        final String requested = command; final InteractiveSessionContract activeContract;
        synchronized (this) { if (!promptReady || session == null || !session.isRunning() || !contract.isAuthorizedForInteractiveRuntime()) { if (listener != null) listener.onResult("runtime-session-not-ready", 126); return; } activeContract = contract; }
        new Thread(() -> {
            int status = 126; String output;
            try {
                String[] profileArgs = activeContract.prootArguments();
                List<String> argv = new ArrayList<>(); for (String value : profileArgs) argv.add(value);
                if (argv.size() < 2 || !"-i".equals(argv.get(argv.size() - 1))) throw new IllegalStateException("profile-proot-command-contract-invalid");
                argv.set(argv.size() - 1, "-c"); argv.add(requested); argv.add(0, activeContract.prootExecutable().getAbsolutePath());
                ProcessBuilder builder = new ProcessBuilder(argv); builder.directory(activeContract.hostCwd()); builder.redirectErrorStream(true);
                for (String entry : activeContract.environment()) { if (entry == null) continue; int separator = entry.indexOf('='); if (separator <= 0) throw new IllegalStateException("invalid-runtime-environment-entry"); builder.environment().put(entry.substring(0, separator), entry.substring(separator + 1)); }
                CommandResult result = executeProcess(builder, COMMAND_TIMEOUT_MS, MAX_COMMAND_OUTPUT_CHARS); status = result.exitStatus; output = result.timedOut ? "runtime-command-timeout\n" + result.output : result.output;
            } catch (Exception error) { output = error.getClass().getSimpleName() + ":" + String.valueOf(error.getMessage()); }
            if (listener != null) listener.onResult(output, status);
        }, "alfa-runtime-command").start();
    }

    @Override public void logVerbose(String tag, String message) { }
    @Override public void logStackTraceWithMessage(String tag, String message, Exception e) { }
    @Override public void logStackTrace(String tag, Exception e) { }
}