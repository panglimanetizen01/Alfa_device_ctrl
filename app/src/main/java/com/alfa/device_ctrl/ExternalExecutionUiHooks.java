package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alfa.device_ctrl.external.ExternalExecutionCoordinator;
import com.alfa.device_ctrl.external.ExternalExecutionResult;

/** Adds the real external-lane controls to the active Stitch launcher without touching the PTY engine. */
public final class ExternalExecutionUiHooks {
    private static final int TAG_ID = 0xA1FAEE1;
    private static final int SURFACE = Color.rgb(28, 32, 37);
    private static final int TEXT = Color.rgb(224, 226, 234);
    private static final int CYAN = Color.rgb(76, 215, 246);
    private static final int GREEN = Color.rgb(78, 222, 163);
    private static final int MUTED = Color.rgb(187, 202, 191);

    private ExternalExecutionUiHooks() { }

    public static void apply(Activity activity) {
        if (!(activity instanceof StitchOperationalActivityV2)) return;
        View root = activity.findViewById(android.R.id.content);
        if (!(root instanceof FrameLayout) || root.getTag(TAG_ID) != null) return;

        FrameLayout content = (FrameLayout) root;
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 6));
        panel.setBackgroundColor(SURFACE);

        TextView title = new TextView(activity);
        title.setText("EXTERNAL CAPABILITY // EVIDENCE");
        title.setTextColor(CYAN);
        title.setTextSize(10);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(activity, 28)));

        TextView status = new TextView(activity);
        status.setText("idle — external lanes do not replace PTY/PRoot");
        status.setTextColor(MUTED);
        status.setTextSize(9);
        status.setTypeface(Typeface.MONOSPACE);
        panel.addView(status, new LinearLayout.LayoutParams(-1, dp(activity, 34)));

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button shizuku = button(activity, "SHIZUKU", GREEN);
        Button termux = button(activity, "TERMUX", CYAN);
        Button api = button(activity, "TERMUX API", CYAN);
        actions.addView(shizuku, weightParams(activity));
        actions.addView(termux, weightParams(activity));
        actions.addView(api, weightParams(activity));
        panel.addView(actions, new LinearLayout.LayoutParams(-1, dp(activity, 44)));

        ExternalExecutionCoordinator coordinator = new ExternalExecutionCoordinator(activity);
        shizuku.setOnClickListener(v -> coordinator.executeShizukuProbe(listener(status)));
        termux.setOnClickListener(v -> coordinator.executeTermuxProbe(listener(status)));
        api.setOnClickListener(v -> coordinator.executeTermuxApiProbe(listener(status)));

        FrameLayout.LayoutParams placement = new FrameLayout.LayoutParams(-1, dp(activity, 112), Gravity.BOTTOM);
        placement.setMargins(dp(activity, 8), 0, dp(activity, 8), dp(activity, 66));
        content.addView(panel, placement);
        content.setTag(TAG_ID, Boolean.TRUE);
    }

    private static ExternalExecutionCoordinator.Listener listener(TextView status) {
        return new ExternalExecutionCoordinator.Listener() {
            @Override public void onResult(ExternalExecutionResult result) {
                String state = "lane=" + result.getLane()
                        + " pid=" + result.getPid()
                        + " exit=" + result.getExitCode()
                        + " stdout=" + compact(result.getStdout())
                        + " stderr=" + compact(result.getStderr());
                status.post(() -> status.setText(result.hasError() ? "ERROR // " + result.getError() + " // " + state : "RESULT // " + state));
            }
            @Override public void onEvent(String event) {
                status.post(() -> status.setText(event));
            }
        };
    }

    private static String compact(String value) {
        if (value == null || value.isEmpty()) return "<empty>";
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() > 96 ? normalized.substring(0, 96) + "…" : normalized;
    }

    private static Button button(Activity activity, String label, int color) {
        Button b = new Button(activity);
        b.setText(label);
        b.setTextColor(color);
        b.setTextSize(9);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    private static LinearLayout.LayoutParams weightParams(Activity activity) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(activity, 44), 1f);
        p.setMargins(dp(activity, 2), 0, dp(activity, 2), 0);
        return p;
    }

    private static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
