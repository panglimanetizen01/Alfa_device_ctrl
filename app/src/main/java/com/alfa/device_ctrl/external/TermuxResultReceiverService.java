package com.alfa.device_ctrl.external;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Receives the documented Termux RUN_COMMAND PendingIntent result bundle. */
public final class TermuxResultReceiverService extends IntentService {
    public static final String EXTRA_REQUEST_ID = "alfa.termux.request_id";
    private static final Map<Integer, ExternalExecutionTransport.Callback> CALLBACKS = new ConcurrentHashMap<>();

    public TermuxResultReceiverService() {
        super("AlfaTermuxResultReceiver");
    }

    static void register(int requestId, ExternalExecutionTransport.Callback callback) {
        CALLBACKS.put(requestId, callback);
    }

    static void fail(int requestId, String message) {
        ExternalExecutionTransport.Callback callback = CALLBACKS.remove(requestId);
        if (callback != null) callback.onError(message);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        int requestId = intent.getIntExtra(EXTRA_REQUEST_ID, -1);
        ExternalExecutionTransport.Callback callback = CALLBACKS.remove(requestId);
        if (callback == null) return;

        Bundle result = intent.getBundleExtra(TermuxRunCommandBridge.RESULT_BUNDLE);
        if (result == null) {
            callback.onError("Termux returned no result bundle");
            return;
        }

        String stdout = result.getString(TermuxRunCommandBridge.RESULT_STDOUT, "");
        String stderr = result.getString(TermuxRunCommandBridge.RESULT_STDERR, "");
        int exitCode = result.getInt(TermuxRunCommandBridge.RESULT_EXIT_CODE, -1);
        int errorCode = result.getInt(TermuxRunCommandBridge.RESULT_ERR, TermuxRunCommandBridge.TERMUX_RESULT_OK);
        String errorMessage = result.getString(TermuxRunCommandBridge.RESULT_ERRMSG, "");

        PidAndOutput pidAndOutput = extractPid(stdout);
        if (pidAndOutput.pid > 0) callback.onStarted(pidAndOutput.pid, -1);
        if (!pidAndOutput.output.isEmpty()) callback.onStdout(pidAndOutput.output);
        if (!stderr.isEmpty()) callback.onStderr(stderr);
        if (errorCode != TermuxRunCommandBridge.TERMUX_RESULT_OK || !errorMessage.isEmpty()) {
            callback.onError(errorMessage.isEmpty() ? "Termux returned error code " + errorCode : errorMessage);
        }
        callback.onExit(exitCode);
    }

    private PidAndOutput extractPid(String stdout) {
        if (stdout == null || stdout.isEmpty()) return new PidAndOutput(-1, "");
        int newline = stdout.indexOf('\n');
        if (newline < 0) return new PidAndOutput(-1, stdout);
        String first = stdout.substring(0, newline).trim();
        try {
            long pid = Long.parseLong(first);
            String remainder = stdout.substring(newline + 1);
            return new PidAndOutput(pid, remainder);
        } catch (NumberFormatException ignored) {
            return new PidAndOutput(-1, stdout);
        }
    }

    private static final class PidAndOutput {
        final long pid;
        final String output;

        PidAndOutput(long pid, String output) {
            this.pid = pid;
            this.output = output;
        }
    }
}
