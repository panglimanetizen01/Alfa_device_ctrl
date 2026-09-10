package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Presentation-only Terminal Obsidian styling. Never supplies runtime state or capability state. */
public final class AlfaUiTheme {
    public static final int CANVAS = Color.rgb(11, 15, 20);
    public static final int SURFACE_1 = Color.rgb(18, 24, 32);
    public static final int SURFACE_2 = Color.rgb(26, 34, 45);
    public static final int BORDER = Color.rgb(31, 41, 55);
    public static final int BORDER_FOCUSED = Color.rgb(55, 65, 81);
    public static final int TEXT = Color.rgb(224, 226, 234);
    public static final int TEXT_MUTED = Color.rgb(187, 202, 191);
    public static final int READY = Color.rgb(16, 185, 129);
    public static final int WARNING = Color.rgb(245, 158, 11);
    public static final int ERROR = Color.rgb(239, 68, 68);
    public static final int TELEMETRY = Color.rgb(6, 182, 212);
    public static final int UNKNOWN = Color.rgb(100, 116, 139);

    private static final int LEGACY_PRIMARY = Color.rgb(171, 199, 255);
    private static final int LEGACY_MUTED = Color.rgb(193, 198, 213);
    private static final int LEGACY_ERROR = Color.rgb(255, 180, 171);
    private static final int TOUCH_TARGET_DP = 48;
    private static final int RADIUS_DP = 4;

    private AlfaUiTheme() { }

    public static void apply(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        root.setBackgroundColor(CANVAS);
        float density = activity.getResources().getDisplayMetrics().density;
        applyTree(root, 0, density);
        root.post(() -> adaptRuntimeDashboard(activity, root, density));
    }

    private static void applyTree(View view, int depth, float density) {
        if (view.getClass().getName().contains("TerminalView")) return;
        int min = Math.round(TOUCH_TARGET_DP * density);
        if (view.isClickable() || view instanceof Button) {
            view.setMinimumHeight(Math.max(view.getMinimumHeight(), min));
            view.setMinimumWidth(Math.max(view.getMinimumWidth(), min));
        }
        if (view instanceof Button) {
            Button button = (Button) view;
            int original = button.getTextColors() == null ? TEXT : button.getTextColors().getDefaultColor();
            int textColor = normalizeTextColor(original);
            int accent = textColor == ERROR ? ERROR : (textColor == READY ? READY : (textColor == TELEMETRY ? TELEMETRY : BORDER_FOCUSED));
            button.setTextSize(11);
            button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            button.setTextColor(textColor);
            button.setBackground(controlBackground(accent, density));
            button.setAllCaps(false);
        } else if (view instanceof TextView) {
            TextView text = (TextView) view;
            int original = text.getCurrentTextColor();
            text.setTextColor(normalizeTextColor(original));
            if (text.isClickable()) text.setMinimumHeight(Math.max(text.getMinimumHeight(), min));
            Typeface current = text.getTypeface();
            if (current != null) text.setTypeface(Typeface.create(current, current.isBold() ? Typeface.BOLD : Typeface.NORMAL));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyTree(group.getChildAt(i), depth + 1, density);
        }
    }

    private static int normalizeTextColor(int color) {
        if (color == LEGACY_PRIMARY) return READY;
        if (color == LEGACY_ERROR) return ERROR;
        if (color == LEGACY_MUTED) return TEXT_MUTED;
        if (color == Color.TRANSPARENT) return TEXT;
        return color;
    }

    /** Uses the current Activity window bounds and keeps terminal space available in compact windows. */
    private static void adaptRuntimeDashboard(Activity activity, View root, float density) {
        LinearLayout content = findContentColumn(root);
        if (content == null || content.getHeight() <= 0) return;
        int runtimeCount = Math.max(1, RuntimeRegistry.all().size());
        int windowHeightDp = currentWindowHeightDp(activity, density, content.getHeight());
        AdaptiveRuntimeLayoutPolicy.Layout layout = AdaptiveRuntimeLayoutPolicy.resolve(windowHeightDp, runtimeCount);

        View runtimeWindow = content.getChildAt(0);
        View monitorWindow = content.getChildAt(2);
        View terminalWindow = content.getChildAt(4);
        setFixedHeight(runtimeWindow, dp(layout.runtimeDp, density));
        setFixedHeight(monitorWindow, dp(layout.monitorDp, density));
        if (terminalWindow.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams terminal = (LinearLayout.LayoutParams) terminalWindow.getLayoutParams();
            terminal.height = 0;
            terminal.weight = 1f;
            terminalWindow.setLayoutParams(terminal);
            terminalWindow.setMinimumHeight(dp(layout.terminalMinDp, density));
        }
        content.requestLayout();
    }

    private static int currentWindowHeightDp(Activity activity, float density, int fallbackPx) {
        try {
            WindowManager windowManager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
            if (windowManager != null && android.os.Build.VERSION.SDK_INT >= 30) {
                android.view.WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
                return Math.max(1, Math.round(metrics.getBounds().height() / metrics.getDensity()));
            }
        } catch (RuntimeException ignored) { }
        return Math.max(1, Math.round(fallbackPx / density));
    }

    private static LinearLayout findContentColumn(View root) {
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        if (group instanceof LinearLayout && group.getOrientation() == LinearLayout.VERTICAL && group.getChildCount() == 5
                && group.getChildAt(0) instanceof LinearLayout
                && group.getChildAt(2) instanceof LinearLayout
                && group.getChildAt(4) instanceof LinearLayout) {
            return (LinearLayout) group;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            LinearLayout found = findContentColumn(group.getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }

    private static void setFixedHeight(View view, int heightPx) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
            params.height = heightPx;
            params.weight = 0f;
            view.setLayoutParams(params);
        }
    }

    private static int dp(int value, float density) { return Math.round(value * density); }

    private static GradientDrawable controlBackground(int accent, float density) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(SURFACE_2);
        drawable.setCornerRadius(RADIUS_DP * density);
        drawable.setStroke(Math.max(1, Math.round(density)), accent == ERROR ? ERROR : (accent == READY ? READY : BORDER));
        return drawable;
    }
}
