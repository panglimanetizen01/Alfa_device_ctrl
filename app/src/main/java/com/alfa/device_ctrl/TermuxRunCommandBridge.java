package com.alfa.device_ctrl;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.UUID;

import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE;

/** Production Termux RUN_COMMAND transport. */
public final class TermuxRunCommandBridge {
    public interface Callback { void onSubmitted(String requestId); void onFailure(String requestId,String reason); }
    private final Context context;
    public TermuxRunCommandBridge(Context context) { this.context=context.getApplicationContext(); }

    public void execute(String executable, String[] arguments, Callback callback) {
        final String id=UUID.randomUUID().toString();
        if (executable == null || executable.trim().isEmpty()) { fail(id,"invalid-command",callback); return; }
        Intent intent=new Intent();
        intent.setClassName(TermuxConstants.TERMUX_PACKAGE_NAME, TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE_NAME);
        intent.setAction(RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND);
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_COMMAND_PATH, executable);
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_ARGUMENTS, arguments == null ? new String[0] : arguments);
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_WORKDIR, TermuxConstants.TERMUX_HOME_DIR_PATH);
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_BACKGROUND, true);
        Intent resultIntent=new Intent(context,TermuxRunCommandResultReceiver.class);
        resultIntent.putExtra(TermuxRunCommandResultReceiver.EXTRA_REQUEST_ID,id);
        int flags=PendingIntent.FLAG_ONE_SHOT;
        if (android.os.Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
        PendingIntent pending=PendingIntent.getService(context,(int)(System.nanoTime() & 0x7fffffff),resultIntent,flags);
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_PENDING_INTENT,pending);
        try { context.startService(intent); if(callback!=null)callback.onSubmitted(id); }
        catch (Throwable error) { fail(id,"startService:"+error.getClass().getSimpleName(),callback); }
    }

    private void fail(String id,String reason,Callback callback){
        try { ExternalExecutionEvidence.persistFailure(context,"TERMUX_RUN_COMMAND",id,reason); } catch(Exception ignored){}
        if(callback!=null)callback.onFailure(id,reason);
    }
}