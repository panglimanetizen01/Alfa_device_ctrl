package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private static final int TOUCH_TARGET_DP = 48;
    private static final int RADIUS_DP = 4;

    private AlfaUiTheme() { }

    public static void apply(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        root.setBackgroundColor(CANVAS);
        applyTree(root, 0, activity.getResources().getDisplayMetrics().density);
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
            int textColor = button.getTextColors() == null ? TEXT : button.getTextColors().getDefaultColor();
            int accent = textColor == ERROR ? ERROR : (textColor == READY || textColor == TELEMETRY ? textColor : BORDER_FOCUSED);
            button.setTextSize(11);
            button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            button.setTextColor(textColor == Color.TRANSPARENT ? TEXT : textColor);
            button.setBackground(controlBackground(accent, density));
            button.setAllCaps(false);
        } else if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.isClickable()) text.setMinimumHeight(Math.max(text.getMinimumHeight(), min));
            Typeface current = text.getTypeface();
            if (current != null && current.isMonospace()) {
                text.setTypeface(Typeface.create("monospace", current.isBold() ? Typeface.BOLD : Typeface.NORMAL));
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (!(child instanceof TextView) && !(child instanceof Button) && depth == 0) {
                    child.setBackgroundColor(CANVAS);
                }
                applyTree(child, depth + 1, density);
            }
        }
    }

    private static GradientDrawable controlBackground(int accent, float density) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(SURFACE_2);
        drawable.setCornerRadius(RADIUS_DP * density);
        drawable.setStroke(Math.max(1, Math.round(density)), accent == ERROR ? ERROR : BORDER);
        return drawable;
    }
}
