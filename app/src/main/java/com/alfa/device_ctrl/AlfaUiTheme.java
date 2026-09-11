package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
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
        activity.getWindow().setStatusBarColor(CANVAS);
        activity.getWindow().setNavigationBarColor(CANVAS);
        float density = activity.getResources().getDisplayMetrics().density;
        applyTree(root, 0, density);
        root.post(() -> adaptRuntimeDashboard(root, density));
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
            if (current != null) {
                text.setTypeface(Typeface.create(current, current.isBold() ? Typeface.BOLD : Typeface.NORMAL));
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (!(child instanceof TextView) && !(child instanceof Button) && depth == 0) {
                    child.setBackgroundColor(SURFACE_1);
                }
                applyTree(child, depth + 1, density);
            }
        }
    }

    private static int normalizeTextColor(int color) {
        if (color == LEGACY_PRIMARY) return READY;
        if (color == LEGACY_ERROR) return ERROR;
        if (color == LEGACY_MUTED) return TEXT_MUTED;
        if (color == Color.TRANSPARENT) return TEXT;
        return color;
    }

    /**
     * Adapts runtime cards to the measured dashboard window. It never uses physical display size
     * or device-class assumptions. Every card remains at least 48dp high, including its controls.
     */
    private static void adaptRuntimeDashboard(View root, float density) {
        LinearLayout dashboard = findRuntimeDashboard(root);
        if (dashboard == null || dashboard.getHeight() <= 0) return;
        int runtimeCount = RuntimeRegistry.all().size();
        if (runtimeCount <= 0) return;

        int padding = dashboard.getPaddingTop() + dashboard.getPaddingBottom();
        int separatorHeight = Math.max(1, Math.round(2 * density));
        int separators = Math.max(0, runtimeCount - 1);
        int available = Math.max(0, dashboard.getHeight() - padding - separators * separatorHeight);
        int cardHeight = Math.max(Math.round(48 * density), available / runtimeCount);

        int cards = 0;
        for (int i = 0; i < dashboard.getChildCount(); i++) {
            View child = dashboard.getChildAt(i);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (!(lp instanceof LinearLayout.LayoutParams)) continue;
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
            if (child instanceof LinearLayout && cards < runtimeCount) {
                params.height = cardHeight;
                params.weight = 0;
                child.setLayoutParams(params);
                cards++;
            } else {
                params.height = separatorHeight;
                params.weight = 0;
                child.setLayoutParams(params);
            }
        }
        dashboard.requestLayout();
    }

    private static LinearLayout findRuntimeDashboard(View root) {
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        int expected = RuntimeRegistry.all().size();
        if (expected > 0 && group.getChildCount() >= expected * 2 - 1) {
            int cards = 0;
            for (int i = 0; i < group.getChildCount(); i += 2) {
                if (group.getChildAt(i) instanceof LinearLayout) cards++;
            }
            if (cards == expected) return (LinearLayout) group;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            LinearLayout found = findRuntimeDashboard(group.getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }

    private static GradientDrawable controlBackground(int accent, float density) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(SURFACE_2);
        drawable.setCornerRadius(RADIUS_DP * density);
        drawable.setStroke(Math.max(1, Math.round(density)), accent == ERROR ? ERROR : (accent == READY ? READY : BORDER));
        return drawable;
    }
}
