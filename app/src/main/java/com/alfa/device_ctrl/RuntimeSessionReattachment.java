package com.alfa.device_ctrl;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.termux.view.TerminalView;

/**
 * Rebinds the process-owned runtime session to a recreated MainActivity without creating a
 * second PTY. The foreground service remains the lifecycle owner; this class only reconnects UI.
 */
public final class RuntimeSessionReattachment {
    private static final String TAG = "AlfaSessionReattach";

    private RuntimeSessionReattachment() { }

    public static void tryReattach(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        RuntimeSessionManager manager = RuntimeKeepAliveService.owner();
        if (manager == null || !manager.isRunning()) return;
        TerminalView terminal = findTerminalView(activity.findViewById(android.R.id.content));
        if (terminal == null) return;
        try {
            manager.rebindListener((MainActivity) activity);
            manager.attachTo(terminal);
        } catch (RuntimeException error) {
            Log.w(TAG, "runtime session reattachment failed", error);
        }
    }

    private static TerminalView findTerminalView(View view) {
        if (view instanceof TerminalView) return (TerminalView) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            TerminalView found = findTerminalView(group.getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }
}
