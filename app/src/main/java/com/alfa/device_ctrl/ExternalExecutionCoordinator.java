package com.alfa.device_ctrl;

import android.content.Context;

/** Production coordinator. External lanes are independent from the embedded runtime PTY. */
public final class ExternalExecutionCoordinator {
    private final ShizukuExecutionBridge shizuku;
    private final TermuxRunCommandBridge termux;
    private final TermuxApiCapabilityBridge termuxApi;

    public ExternalExecutionCoordinator(Context context) {
        shizuku=new ShizukuExecutionBridge(context);
        termux=new TermuxRunCommandBridge(context);
        termuxApi=new TermuxApiCapabilityBridge(context);
    }

    public void executeShizuku(String command, ShizukuExecutionBridge.Callback callback) {
        shizuku.execute(new String[]{"/system/bin/sh","-c",command},callback);
    }
    public void executeTermux(String command, TermuxRunCommandBridge.Callback callback) {
        termux.execute("/data/data/com.termux/files/usr/bin/sh",new String[]{"-c",command},callback);
    }
    public void probeTermuxApi(TermuxApiCapabilityBridge.Callback callback) {
        termuxApi.probeBattery(callback);
    }
}