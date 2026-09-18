package com.alfa.device_ctrl;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;

public final class TermuxRunCommandResultReceiver extends IntentService {
    static final String EXTRA_REQUEST_ID = "alfa_request_id";
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
        int termuxErr=result.getInt(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERR,0);
        String termuxMsg=result.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG,"");
        try {
            ExternalExecutionEvidence.persist(this,"TERMUX_RUN_COMMAND",id,
                    android.os.Process.myPid(),out,err,exit,termuxErr == -1 ? "EXECUTED" : "FAILED:"+termuxErr+":"+termuxMsg);
        } catch (Exception ignored) {}
    }
}