package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import com.termux.view.TerminalView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Transitional adapter exposing reconciled native surfaces without duplicating MainActivity UI. */
public final class AlfaUiCapabilityRouter {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AlfaUiCapabilityRouter() {}

    public static void install(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        MAIN.post(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            route(activity, "Buka alat runtime", RuntimeToolsActivity.class);
            route(activity, "Tampilkan file runtime", ProjectExplorerActivity.class);
            route(activity, "Tampilkan kebijakan runtime", PolicyEvidenceActivity.class);
            applyAppearance(activity);
            restoreRuntimeSelection(activity);
        });
    }

    public static void persist(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        try {
            Field field = MainActivity.class.getDeclaredField("selectedRuntime");
            field.setAccessible(true);
            RuntimeProfile profile = (RuntimeProfile) field.get(activity);
            if (profile != null) AlfaSettingsStore.get(activity).setRuntimeId(profile.id());
        } catch (Exception ignored) {
            // Persistence failure remains observable as unchanged state; never fabricate success.
        }
    }

    private static void applyAppearance(Activity activity) {
        new Thread(() -> {
            int size = AlfaSettingsStore.get(activity).getTerminalFontSize(12);
            MAIN.post(() -> {
                View root = activity.findViewById(android.R.id.content);
                TerminalView terminal = findTerminal(root);
                if (terminal != null) AppearanceLanguageSettingsActivity.applyToTerminal(terminal, size);
            });
        }, "alfa-settings-appearance").start();
    }

    private static void restoreRuntimeSelection(Activity activity) {
        new Thread(() -> {
            String id = AlfaSettingsStore.get(activity).getRuntimeId(RuntimeSelection.DEFAULT_RUNTIME_ID);
            if (RuntimeSelection.profile(id) == null) id = RuntimeSelection.DEFAULT_RUNTIME_ID;
            final String chosen = id;
            MAIN.post(() -> {
                try {
                    Field field = MainActivity.class.getDeclaredField("selectedRuntime");
                    field.setAccessible(true);
                    RuntimeProfile current = (RuntimeProfile) field.get(activity);
                    if (current == null || !chosen.equals(current.id())) {
                        Method select = MainActivity.class.getDeclaredMethod("selectRuntime", String.class);
                        select.setAccessible(true);
                        select.invoke(activity, chosen);
                    }
                } catch (Exception ignored) {
                    // Runtime selection remains at the existing canonical default.
                }
            });
        }, "alfa-settings-load").start();
    }

    private static void route(Activity activity, String description, Class<? extends Activity> target) {
        View root = activity.findViewById(android.R.id.content);
        View found = find(root, description);
        if (found == null) return;
        found.setOnClickListener(v -> activity.startActivity(new Intent(activity, target)));
    }

    private static View find(View view, String description) {
        if (view == null) return null;
        if (description.equals(view.getContentDescription())) return view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            View result = find(group.getChildAt(i), description);
            if (result != null) return result;
        }
        return null;
    }

    private static TerminalView findTerminal(View view) {
        if (view instanceof TerminalView) return (TerminalView) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            TerminalView result = findTerminal(group.getChildAt(i));
            if (result != null) return result;
        }
        return null;
    }
}
