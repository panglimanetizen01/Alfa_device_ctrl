package com.alfa.device_ctrl;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TermuxRunCommandResultReceiver extends IntentService {
    static final String EXTRA_REQUEST_ID = "alfa_request_id";
    private static final Pattern PID_MARKER = Pattern.compile("(?m)^ALFA_CHILD_PID=(\\d+)\\s*$");

    public TermuxRunCommandResultReceiver() { super("AlfaTermuxRunCommandResultReceiver"); }

    @Override protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        String id = intent.getStringExtra(EXTRA_REQUEST_ID);
        if (id == null || id.trim().isEmpty()) return;
        Bundle result = intent.getBundleExtra(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE);
        if (result == null) {
            try { ExternalExecutionEvidence.persistFailure(this,"TERMUX_RUN_COMMAND",id,"missing-result-bundle"); } catch (Exception ignored) {}
            return;
        }
        String out=result.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT,"");
        String err=result.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDERR,"");
        int exit=result.getInt(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE,126);
        long pid=parsePid(out);
        if (pid <= 0) {
            try { ExternalExecutionEvidence.persistFailure(this,"TERMUX_RUN_COMMAND",id,"child-pid-marker-missing"); } catch (Exception ignored) {}
            return;
        }
        int termuxErr=result.getInt(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERR,0);
        String termuxMsg=result.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG,"");
        try {
            ExternalExecutionEvidence.persist(this,"TERMUX_RUN_COMMAND",id,
                    pid,out,err,exit,termuxErr == -1 ? "EXECUTED" : "FAILED:"+termuxErr+":"+termuxMsg);
        } catch (Exception ignored) {}
    }

    private static long parsePid(String stdout) {
        if (stdout == null) return 0;
        Matcher matcher=PID_MARKER.matcher(stdout);
        if (!matcher.find()) return 0;
        try { return Long.parseLong(matcher.group(1)); }
        catch (NumberFormatException ignored) { return 0; }
    }
}