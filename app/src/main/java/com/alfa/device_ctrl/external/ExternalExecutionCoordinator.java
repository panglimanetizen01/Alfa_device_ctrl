package com.alfa.device_ctrl.external;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.alfa.device_ctrl.ExecutionLane;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Production caller for the external capability lanes. It owns capability selection,
 * invokes one concrete transport, aggregates process evidence, and returns one immutable
 * ExternalExecutionResult. It never participates in the Linux runtime/PTTY engine.
 */
public final class ExternalExecutionCoordinator {
    public interface Listener {
        void onResult(ExternalExecutionResult result);
        void onEvent(String event);
    }

    private static final long PROBE_TIMEOUT_MS = 15000L;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ExternalExecutionCoordinator(Context context) {
        this.context = context.getApplicationContext();
    }

    public void executeShizukuProbe(Listener listener) {
        String marker = "ALFA_SHIZUKU_PROBE_" + UUID.randomUUID();
        ShizukuExecutionBridge bridge = new ShizukuExecutionBridge(context);
        if (!bridge.isAvailable()) {
            listener.onResult(failure(ExecutionLane.SHIZUKU_RISH, "Shizuku binder unavailable"));
            return;
        }
        if (!bridge.hasPermission()) {
            listener.onResult(failure(ExecutionLane.SHIZUKU_RISH, "Shizuku permission not granted"));
            return;
        }
        execute(ExecutionLane.SHIZUKU_RISH, bridge,
                new String[]{"/system/bin/sh", "-c", "printf '%s\\n' '" + marker + "'; printf '%s\\n' 'ALFA_SHIZUKU_STDERR' >&2; exit 0"},
                null, listener, marker);
    }

    public void executeTermuxProbe(Listener listener) {
        String marker = "ALFA_TERMUX_PROBE_" + UUID.randomUUID();
        TermuxRunCommandBridge bridge = new TermuxRunCommandBridge(context);
        if (!bridge.isInstalled()) {
            listener.onResult(failure(ExecutionLane.TERMUX, "Termux package not installed"));
            return;
        }
        if (!bridge.hasPermission()) {
            listener.onResult(failure(ExecutionLane.TERMUX, "Termux RUN_COMMAND permission not granted"));
            return;
        }
        execute(ExecutionLane.TERMUX, bridge,
                new String[]{TermuxRunCommandBridge.TERMUX_SHELL, "-c", "printf '%s\\n' '" + marker + "'; printf '%s\\n' 'ALFA_TERMUX_STDERR' >&2; exit 0"},
                null, listener, marker);
    }

    public void executeTermuxApiProbe(Listener listener) {
        String marker = "ALFA_TERMUX_API_PROBE_" + UUID.randomUUID();
        TermuxApiCapability api = new TermuxApiCapability(context);
        if (!api.isInstalled()) {
            listener.onResult(failure(ExecutionLane.TERMUX_API, "Termux:API package not installed"));
            return;
        }
        TermuxRunCommandBridge bridge = new TermuxRunCommandBridge(context);
        if (!bridge.isInstalled() || !bridge.hasPermission()) {
            listener.onResult(failure(ExecutionLane.TERMUX_API, "Termux RUN_COMMAND transport unavailable"));
            return;
        }
        // Two boundaries are explicit: RUN_COMMAND is the transport; termux-battery-status
        // is the Termux:API device capability executed inside that transport.
        execute(ExecutionLane.TERMUX_API, bridge,
                new String[]{TermuxRunCommandBridge.TERMUX_SHELL, "-c", "printf '%s\\n' '" + marker + "'; termux-battery-status; exit $?"},
                null, listener, marker);
    }

    private void execute(ExecutionLane lane, ExternalExecutionTransport bridge, String[] command,
                         String workDir, Listener listener, String marker) {
        EvidenceAccumulator evidence = new EvidenceAccumulator(lane, marker, bridge, listener);
        listener.onEvent("DISPATCH lane=" + lane + " marker=" + marker);
        mainHandler.postDelayed(evidence::timeout, PROBE_TIMEOUT_MS);
        bridge.execute(command, workDir, null, new ExternalExecutionTransport.Callback() {
            @Override public void onStarted(long pid, int uid) {
                evidence.pid = pid;
                listener.onEvent("PROCESS_STARTED lane=" + lane + " pid=" + pid + " uid=" + uid);
            }
            @Override public void onStdout(String chunk) { evidence.stdout.append(chunk); }
            @Override public void onStderr(String chunk) { evidence.stderr.append(chunk); }
            @Override public void onExit(int exitCode) { evidence.exitCode = exitCode; evidence.finish(); }
            @Override public void onError(String message) { evidence.error = message; listener.onEvent("ERROR lane=" + lane + " message=" + message); }
        });
    }

    private ExternalExecutionResult failure(ExecutionLane lane, String error) {
        return new ExternalExecutionResult(lane, -1L, -1, "", "", error);
    }

    private final class EvidenceAccumulator {
        final ExecutionLane lane;
        final String marker;
        final ExternalExecutionTransport bridge;
        final Listener listener;
        final StringBuilder stdout = new StringBuilder();
        final StringBuilder stderr = new StringBuilder();
        final AtomicBoolean finished = new AtomicBoolean();
        volatile long pid = -1L;
        volatile int exitCode = -1;
        volatile String error;

        EvidenceAccumulator(ExecutionLane lane, String marker, ExternalExecutionTransport bridge, Listener listener) {
            this.lane = lane;
            this.marker = marker;
            this.bridge = bridge;
            this.listener = listener;
        }

        void finish() {
            if (!finished.compareAndSet(false, true)) return;
            ExternalExecutionResult result = new ExternalExecutionResult(
                    lane, pid, exitCode, stdout.toString(), stderr.toString(), error);
            mainHandler.removeCallbacks(this::timeout);
            listener.onEvent("EXIT lane=" + lane + " pid=" + pid + " exit=" + exitCode + " marker=" + marker);
            listener.onResult(result);
        }

        void timeout() {
            if (finished.get()) return;
            error = error == null ? "external execution timeout" : error;
            bridge.terminate();
            exitCode = -1;
            finish();
        }
    }
}
