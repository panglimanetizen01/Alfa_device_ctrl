package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/** Centralized Terminal Obsidian visual tokens and runtime layout normalization. */
public final class AlfaUiTheme {
    public static final int CANVAS = Color.rgb(11, 15, 20);
    public static final int SURFACE_1 = Color.rgb(18, 24, 32);
    public static final int SURFACE_2 = Color.rgb(26, 34, 45);
    public static final int BORDER = Color.rgb(31, 41, 55);
    public static final int FOCUS_BORDER = Color.rgb(55, 65, 81);
    public static final int EMERALD = Color.rgb(16, 185, 129);
    public static final int AMBER = Color.rgb(245, 158, 11);
    public static final int CRIMSON = Color.rgb(239, 68, 68);
    public static final int CYAN = Color.rgb(6, 182, 212);
    public static final int SLATE = Color.rgb(100, 116, 139);

    private AlfaUiTheme() {}

    public static void apply(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        root.setBackgroundColor(CANVAS);
        applyTree(root, new HashSet<Integer>());
        root.post(() -> applyAdaptiveRuntimeLayout(root));
    }

    private static void applyTree(View view, Set<Integer> visited) {
        if (view == null || visited.contains(view.getId())) return;
        if (view.getId() != View.NO_ID) visited.add(view.getId());
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getTextSize() > 0f && text.getTextSize() < 14f) {
                text.setTextColor(SLATE);
            }
        }
        if (view instanceof Button) {
            Button button = (Button) view;
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(SURFACE_2);
            bg.setStroke(dp(1, view.getResources().getDisplayMetrics().density), BORDER);
            bg.setCornerRadius(dp(4, view.getResources().getDisplayMetrics().density));
            button.setBackground(bg);
            button.setTextColor(EMERALD);
            button.setMinHeight(dp(48, view.getResources().getDisplayMetrics().density));
            button.setMinWidth(dp(48, view.getResources().getDisplayMetrics().density));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTree(group.getChildAt(i), visited);
            }
        }
    }

    private static void applyAdaptiveRuntimeLayout(View root) {
        LinearLayout content = findContentColumn(root);
        if (content == null || content.getChildCount() < 5) return;
        View runtimeWindow = content.getChildAt(1);
        View monitorWindow = content.getChildAt(2);
        View terminalWindow = content.getChildAt(4);
        float density = root.getResources().getDisplayMetrics().density;
        int heightDp = Math.round(content.getHeight() / density);
        AdaptiveRuntimeLayoutPolicy.Layout layout = AdaptiveRuntimeLayoutPolicy.resolve(heightDp, 1);
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

    private static LinearLayout findContentColumn(View root) {
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        if (group instanceof LinearLayout
                && ((LinearLayout) group).getOrientation() == LinearLayout.VERTICAL
                && group.getChildCount() == 5
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
            LinearLayout.LayoutParams linear = (LinearLayout.LayoutParams) lp;
            linear.height = heightPx;
            linear.weight = 0f;
        } else {
            lp.height = heightPx;
        }
        view.setLayoutParams(lp);
    }

    private static int dp(int value, float density) {
        return Math.round(value * density);
    }
}
