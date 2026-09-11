package com.alfa.device_ctrl;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.termux.view.TerminalView;

/** Reattaches the service-owned runtime session to a newly recreated MainActivity. */
final class RuntimeSessionReattachment {
    private RuntimeSessionReattachment() { }

    static void attach(Activity activity, RuntimeSessionManager manager) {
        if (!(activity instanceof MainActivity) || manager == null || !manager.isRunning()) return;
        TerminalView terminal = findTerminalView(activity.findViewById(android.R.id.content));
        if (terminal == null) return;
        try {
            java.lang.reflect.Field field = MainActivity.class.getDeclaredField("sessionManager");
            field.setAccessible(true);
            field.set(activity, manager);
            manager.rebindListener((RuntimeSessionManager.Listener) activity);
            manager.attachTo(terminal);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("runtime-session-ui-rebind-failed", error);
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
