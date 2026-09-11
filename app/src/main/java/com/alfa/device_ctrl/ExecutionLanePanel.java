package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
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
        if (!(activity instanceof MainActivity)) return;
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;
        if (content.findViewWithTag(TAG) != null) return;

        LinearLayout host = findMainContent((ViewGroup) content);
        if (host == null) return;

        LinearLayout section = new LinearLayout(activity);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 6));
        section.setBackgroundColor(AlfaUiTheme.SURFACE_1);

        TextView title = label(activity, "EXECUTION LANES", AlfaUiTheme.TEXT, 11, true);
        section.addView(title, new LinearLayout.LayoutParams(-1, dp(activity, 24)));

        LinearLayout laneRows = new LinearLayout(activity);
        laneRows.setTag(TAG);
        laneRows.setOrientation(LinearLayout.VERTICAL);
        laneRows.setBackgroundColor(AlfaUiTheme.SURFACE_1);
        for (ExecutionLane lane : ExecutionLane.values()) {
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(activity, 8), 0, dp(activity, 8), 0);
            row.setBackgroundColor(AlfaUiTheme.SURFACE_2);
            TextView name = label(activity, laneLabel(lane), AlfaUiTheme.TEXT, 10, true);
            row.addView(name, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
            TextView state = label(activity, laneState(lane), AlfaUiTheme.UNKNOWN, 9, true);
            state.setGravity(Gravity.CENTER);
            row.addView(state, new LinearLayout.LayoutParams(dp(activity, 112), dp(activity, 40)));
            laneRows.addView(row, new LinearLayout.LayoutParams(-1, dp(activity, 40)));
        }
        section.addView(laneRows, new LinearLayout.LayoutParams(-1, dp(activity, 160)));

        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(-1, dp(activity, 194));
        sectionParams.setMargins(0, dp(activity, 6), 0, dp(activity, 6));
        int insertAt = Math.min(1, host.getChildCount());
        host.addView(section, insertAt, sectionParams);
        AlfaUiTheme.apply(activity);
    }

    private static LinearLayout findMainContent(ViewGroup root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (!(child instanceof ViewGroup)) continue;
            LinearLayout found = findMainContentIn(child);
            if (found != null) return found;
        }
        return null;
    }

    private static LinearLayout findMainContentIn(View view) {
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        if (group instanceof LinearLayout) {
            LinearLayout candidate = (LinearLayout) group;
            if (candidate.getOrientation() == LinearLayout.VERTICAL && candidate.getChildCount() == 3) return candidate;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            LinearLayout found = findMainContentIn(group.getChildAt(i));
            if (found != null) return found;
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
