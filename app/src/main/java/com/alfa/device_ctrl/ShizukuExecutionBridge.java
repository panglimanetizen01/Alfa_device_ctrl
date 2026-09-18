package com.alfa.device_ctrl;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.UUID;

import rikka.shizuku.Shizuku;

/** Production Shizuku/Rish execution/control bridge. */
public final class ShizukuExecutionBridge {
    public interface Callback {
        void onResult(ExternalResult result);
    }
    public static final class ExternalResult {
        public final String requestId, stdout, stderr, status;
        public final long pid;
        public final int exitCode;
        ExternalResult(String id, long pid, String out, String err, int code, String state) {
            requestId=id; this.pid=pid; stdout=out; stderr=err; exitCode=code; status=state;
        }
    }

    private final Context context;
    public ShizukuExecutionBridge(Context context) { this.context = context.getApplicationContext(); }

    public void execute(String[] command, Callback callback) {
        final String requestId = UUID.randomUUID().toString();
        if (command == null || command.length == 0) {
            fail(requestId, "invalid-command", callback);
            return;
        }
        if (Shizuku.isPreV11() || !Shizuku.pingBinder()) {
            fail(requestId, "shizuku-binder-unavailable", callback);
            return;
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            fail(requestId, "shizuku-permission-not-granted", callback);
            return;
        }
        final Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                new ComponentName(context, AlfaShizukuUserService.class))
                .version(1).tag("alfa-external-exec").processNameSuffix("external").daemon(false);
        final StringBuilder stdout = new StringBuilder(), stderr = new StringBuilder();
        final long[] pid = {0};
        final boolean[] completed = {false};
        final ServiceConnection[] holder = new ServiceConnection[1];
        holder[0] = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                IAlfaShizukuService service = IAlfaShizukuService.Stub.asInterface(binder);
                try {
                    service.execute(command, new IAlfaShizukuCallback.Stub() {
                        @Override public void onStarted(long value) { pid[0]=value; }
                        @Override public void onStdout(String chunk) { append(stdout, chunk); }
                        @Override public void onStderr(String chunk) { append(stderr, chunk); }
                        @Override public void onExit(int code) {
                            if (completed[0]) return; completed[0]=true;
                            try { Shizuku.unbindUserService(args, holder[0], false); } catch (Throwable ignored) {}
                            finish(requestId, pid[0], stdout.toString(), stderr.toString(), code, "EXECUTED", callback);
                        }
                        @Override public void onFailure(String reason) {
                            if (completed[0]) return; completed[0]=true;
                            try { Shizuku.unbindUserService(args, holder[0], false); } catch (Throwable ignored) {}
                            fail(requestId, reason, pid[0], stdout.toString(), stderr.toString(), callback);
                        }
                    });
                } catch (RemoteException error) {
                    fail(requestId, "binder-call:" + error.getClass().getSimpleName(), pid[0], stdout.toString(), stderr.toString(), callback);
                }
            }
            @Override public void onServiceDisconnected(ComponentName name) {
                if (!completed[0]) fail(requestId, "user-service-disconnected", pid[0], stdout.toString(), stderr.toString(), callback);
            }
        };
        try { Shizuku.bindUserService(args, holder[0]); }
        catch (Throwable error) { fail(requestId, "bind:" + error.getClass().getSimpleName(), callback); }
    }

    private void finish(String id,long pid,String out,String err,int code,String status,Callback cb) {
        try {
            ExternalExecutionEvidence.persist(context,"SHIZUKU_RISH",id,pid,out,err,code,status);
            if (cb != null) cb.onResult(new ExternalResult(id,pid,out,err,code,status));
        } catch (Exception e) { if (cb != null) cb.onResult(new ExternalResult(id,pid,out,err,code,"EVIDENCE_PERSIST_FAILED:"+e.getMessage())); }
    }
    private void fail(String id,String reason,Callback cb) { fail(id,reason,0,"","",cb); }
    private void fail(String id,String reason,long pid,String out,String err,Callback cb) {
        try { ExternalExecutionEvidence.persistFailure(context,"SHIZUKU_RISH",id,reason); } catch (Exception ignored) {}
        if (cb != null) cb.onResult(new ExternalResult(id,pid,out,err,126,"FAILED:"+reason));
    }
    private static void append(StringBuilder b,String s){if(s!=null&&b.length()<100000)b.append(s,0,Math.min(s.length(),100000-b.length()));}
}