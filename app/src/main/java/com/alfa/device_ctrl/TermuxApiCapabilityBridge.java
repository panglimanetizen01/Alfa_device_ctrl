package com.alfa.device_ctrl;

import android.content.Context;

/** Termux API capability lane: invoked through RUN_COMMAND, never treated as shell transport. */
public final class TermuxApiCapabilityBridge {
    public interface Callback extends TermuxRunCommandBridge.Callback { }
    private final TermuxRunCommandBridge runCommand;
    public TermuxApiCapabilityBridge(Context context) { runCommand=new TermuxRunCommandBridge(context); }

    public void probeBattery(Callback callback) {
        runCommand.execute("/data/data/com.termux/files/usr/bin/termux-battery-status", new String[0],
                callback);
    }
}