package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Presentation-only execution-lane panel derived from the canonical lane enum.
 * It never asserts external lane readiness without evidence.
 */
public final class ExecutionLanePanel {
    public static final String TAG = "alfa.execution.lanes";

    private ExecutionLanePanel() {
    }

    public static void install(Activity activity) {
        if (activity == null) return;
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;
        if (content.findViewWithTag(TAG) != null) return;

        LinearLayout host = findMainContent((ViewGroup) content);
        if (host == null) return;
        LinearLayout panel = new LinearLayout(activity);
        panel.setTag(TAG);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 6));
        panel.setBackgroundColor(AlfaUiTheme.SURFACE_1);

        TextView title = label(activity, "EXECUTION LANES", AlfaUiTheme.TEXT, 11, true);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(activity, 24)));

        for (ExecutionLane lane : ExecutionLane.values()) {
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(activity, 8), 0, dp(activity, 8), 0);
            row.setBackgroundColor(AlfaUiTheme.SURFACE_2);
            TextView name = label(activity, laneLabel(lane), AlfaUiTheme.TEXT, 10, true);
            row.addView(name, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
            TextView state = label(activity, laneState(lane), AlfaUiTheme.UNKNOWN, 9, true);
            state.setGravity(android.view.Gravity.CENTER);
            row.addView(state, new LinearLayout.LayoutParams(dp(activity, 112), dp(activity, 40)));
            panel.addView(row, new LinearLayout.LayoutParams(-1, dp(activity, 40)));
        }

        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(-1, dp(activity, 194));
        panelParams.setMargins(0, dp(activity, 6), 0, dp(activity, 6));
        int insertAt = Math.min(1, host.getChildCount());
        host.addView(panel, insertAt, panelParams);
        AlfaUiTheme.apply(activity);
    }

    private static LinearLayout findMainContent(ViewGroup root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout candidate = (LinearLayout) child;
            if (candidate.getOrientation() == LinearLayout.VERTICAL && candidate.getChildCount() == 3) return candidate;
        }
        return null;
    }

    private static String laneLabel(ExecutionLane lane) {
        switch (lane) {
            case TERMUX_API: return "TERMUX API";
            case SHIZUKU_RISH: return "SHIZUKU / RISH";
            case RUNTIME: return "RUNTIME";
            default: return "TERMUX";
        }
    }

    private static String laneState(ExecutionLane lane) {
        return lane == ExecutionLane.RUNTIME ? "IN-APP" : "UNKNOWN / EVIDENCE";
    }

    private static TextView label(Activity activity, String text, int color, int size, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        return view;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
