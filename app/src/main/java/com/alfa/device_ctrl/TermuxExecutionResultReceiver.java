package com.alfa.device_ctrl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Receives the real Termux RUN_COMMAND result and persists bounded evidence for the UI. */
public final class TermuxExecutionResultReceiver extends BroadcastReceiver {
    static final String EVIDENCE_FILE = "termux-execution-evidence.txt";
    private static final int MAX_OUTPUT = 8192;

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        android.os.Bundle result = intent.getBundleExtra(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE);
        if (result == null) result = intent.getExtras();
        String stdout = value(result, TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT);
        String stderr = value(result, TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDERR);
        int exit = intValue(result, TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE, Integer.MIN_VALUE);
        String error = value(result, TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG);
        String state = exit == 0 ? "PASS" : (exit == Integer.MIN_VALUE ? "UNKNOWN" : "FAIL");
        String text = "RESULT=" + state + "\nEXIT_CODE=" + exit + "\nSTDOUT=" + trim(stdout) + "\nSTDERR=" + trim(stderr) + "\nERROR=" + trim(error) + "\n";
        File file = new File(context.getFilesDir(), EVIDENCE_FILE);
        try (FileOutputStream out = new FileOutputStream(file, false)) { out.write(text.getBytes(StandardCharsets.UTF_8)); } catch (Exception ignored) { }
    }

    private static String value(android.os.Bundle b, String key) {
        if (b == null || key == null) return "";
        Object value = b.get(key);
        return value == null ? "" : String.valueOf(value);
    }
    private static int intValue(android.os.Bundle b, String key, int fallback) {
        if (b == null || key == null || !b.containsKey(key)) return fallback;
        Object value = b.get(key);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }
    private static String trim(String value) {
        if (value == null) return "";
        return value.length() <= MAX_OUTPUT ? value : value.substring(0, MAX_OUTPUT);
    }
}
