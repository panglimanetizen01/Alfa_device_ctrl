package com.alfa.device_ctrl;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Native, catalog-driven browser for all supplied Stitch v1 states with explicit action boundaries. */
public final class StitchV1StatePanel {
    public interface StateOpenAction { void open(AlfaUiNavigation.Screen screen); }

    private StitchV1StatePanel() { }

    public static View build(Activity activity, RuntimeSessionManager manager, StateOpenAction action) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AlfaUiTheme.CANVAS);
        TextView summary = new TextView(activity);
        summary.setText("STITCH V1 STATE CATALOG\n159 supplied states • native mapping • executable states use the real Alfa runtime boundary");
        summary.setTextColor(AlfaUiTheme.TEXT);
        summary.setTextSize(11);
        summary.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        summary.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
        root.addView(summary, new LinearLayout.LayoutParams(-1, dp(activity, 64)));

        ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        String lastDomain = "";
        for (String id : StitchV1ReferenceCatalog.screenIds()) {
            StitchV1StateModel.State state = StitchV1StateModel.resolve(id);
            if (state == null) continue;
            if (!state.domain().equals(lastDomain)) {
                TextView section = new TextView(activity);
                section.setText(state.domain().toUpperCase() + " SURFACE");
                section.setTextColor(AlfaUiTheme.CYAN);
                section.setTextSize(10);
                section.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                section.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 6));
                list.addView(section, new LinearLayout.LayoutParams(-1, dp(activity, 40)));
                lastDomain = state.domain();
            }
            Button item = new Button(activity);
            item.setText(compactLabel(state));
            item.setTextColor(state.referenceOnly() ? AlfaUiTheme.TEXT_MUTED : AlfaUiTheme.TEXT);
            item.setTextSize(9);
            item.setAllCaps(false);
            item.setMinHeight(dp(activity, 48));
            item.setMinWidth(dp(activity, 48));
            item.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            item.setContentDescription("Stitch state " + id);
            item.setOnClickListener(v -> showState(activity, manager, state, action));
            list.addView(item, new LinearLayout.LayoutParams(-1, dp(activity, 52)));
        }
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private static String compactLabel(StitchV1StateModel.State state) {
        String id = state.id();
        if (id.startsWith("alfa_device_ctrl_")) id = id.substring("alfa_device_ctrl_".length());
        String disposition = state.referenceOnly() ? "REFERENCE / NON-EXECUTABLE" : "NATIVE / LIVE BOUNDARY";
        return id + "\n" + disposition + " • ACTION=" + state.action().name();
    }

    private static void showState(Activity activity, RuntimeSessionManager manager,
                                  StitchV1StateModel.State state, StateOpenAction action) {
        String body = "STATE\n" + state.id() + "\n\n" +
                "DOMAIN=" + state.domain() + "\n" +
                "PRIMARY_SURFACE=" + state.primaryScreen().name() + "\n" +
                "REFERENCE_ONLY=" + state.referenceOnly() + "\n" +
                "CLAIMS_LIVE_EXECUTION=" + state.claimsLiveExecution() + "\n" +
                "ACTION=" + state.action().name() + "\n" +
                (state.runtimeCommand() == null ? "" : "RUNTIME_COMMAND=" + state.runtimeCommand() + "\n") +
                "\nThe Stitch state is rendered through the native Alfa surface. Result/simulation states cannot fabricate runtime evidence.";
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle("STITCH STATE").setMessage(body)
                .setNegativeButton("CLOSE", null)
                .setPositiveButton("OPEN NATIVE SURFACE", (d, which) -> action.open(state.primaryScreen()));
        if (!state.referenceOnly() && state.action() == StitchV1StateModel.Action.RUN_RUNTIME_COMMAND) {
            builder.setNeutralButton("EXECUTE REAL COMMAND", (d, which) -> executeRuntimeCommand(activity, manager, state));
        } else if (!state.referenceOnly() && state.action() == StitchV1StateModel.Action.CONFIRM_STOP_SESSION) {
            builder.setNeutralButton("STOP CURRENT SESSION", (d, which) -> stopSession(activity, manager));
        }
        builder.show();
    }

    private static void executeRuntimeCommand(Activity activity, RuntimeSessionManager manager, StitchV1StateModel.State state) {
        if (manager == null || !manager.isRunning() || !manager.isPromptReady()) {
            showResult(activity, "RUNTIME BLOCKED", "The canonical RuntimeSessionManager is not READY. No command was executed.");
            return;
        }
        final String command = state.runtimeCommand();
        manager.runRuntimeCommand(command, (output, exitStatus) -> activity.runOnUiThread(() ->
                showResult(activity, "RUNTIME COMMAND RESULT", "command=" + command + "\nexit_status=" + exitStatus + "\n\n" + output)));
    }

    private static void stopSession(Activity activity, RuntimeSessionManager manager) {
        if (manager == null || !manager.isRunning()) {
            showResult(activity, "SESSION STOP BLOCKED", "No running canonical runtime session is available.");
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle("STOP CURRENT SESSION")
                .setMessage("This invokes RuntimeSessionManager.stop() on the current real session. It does not fabricate a terminated state.")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("STOP", (d, which) -> manager.stop())
                .show();
    }

    private static void showResult(Activity activity, String title, String body) {
        new AlertDialog.Builder(activity).setTitle(title).setMessage(body).setPositiveButton("CLOSE", null).show();
    }

    private static int dp(Activity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
