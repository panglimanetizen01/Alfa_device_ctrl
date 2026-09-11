package com.alfa.device_ctrl;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Real Termux execution/API lane. Unavailable dependencies remain explicitly unavailable. */
public final class TermuxExecutionActivity extends Activity {
    private TextView report;
    private final File evidenceFile() { return new File(getFilesDir(), TermuxExecutionResultReceiver.EVIDENCE_FILE); }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12), dp(12), dp(12), dp(12));
        TextView title = text("TERMUX EXECUTION & API", 16, true); root.addView(title, new LinearLayout.LayoutParams(-1, dp(52)));
        report = text("", 10, false); report.setTypeface(android.graphics.Typeface.MONOSPACE); root.addView(report, new LinearLayout.LayoutParams(-1, 0, 1));
        Button run = button("RUN TERMUX", v -> dispatch(new String[]{"-c", "printf 'ALFA_TERMUX_EXECUTION_OK\\n'"}, "TERMUX_EXECUTION"));
        run.setContentDescription("Jalankan command nyata di Termux"); root.addView(run, new LinearLayout.LayoutParams(-1, dp(48)));
        Button api = button("RUN TERMUX API", v -> dispatch(new String[]{"-c", "termux-battery-status"}, "TERMUX_API"));
        api.setContentDescription("Jalankan Termux API nyata"); root.addView(api, new LinearLayout.LayoutParams(-1, dp(48)));
        Button refresh = button("REFRESH EVIDENCE", v -> render()); root.addView(refresh, new LinearLayout.LayoutParams(-1, dp(48)));
        setContentView(root); render();
    }

    private void dispatch(String[] args, String lane) {
        if (!getPackageManager().hasPackage(TermuxConstants.TERMUX_PACKAGE_NAME)) { renderState(lane + "=NOT_AVAILABLE\nTERMUX_PACKAGE=ABSENT\nRESULT=UNKNOWN\nEVIDENCE=NOT_MEASURED"); return; }
        try {
            Intent intent = new Intent();
            intent.setClassName(TermuxConstants.TERMUX_PACKAGE_NAME, TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE_NAME);
            intent.setAction(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND);
            intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_COMMAND_PATH, TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/bash");
            intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_ARGUMENTS, args);
            intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_WORKDIR, TermuxConstants.TERMUX_HOME_DIR_PATH);
            intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_BACKGROUND, true);
            intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_COMMAND_LABEL, lane);
            intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_COMMAND_DESCRIPTION, "Alfa real execution evidence");
            int request = (int) (System.currentTimeMillis() & 0x7fffffff);
            PendingIntent callback = PendingIntent.getBroadcast(this, request, new Intent(this, TermuxExecutionResultReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            intent.putExtra(TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE.EXTRA_PENDING_INTENT, callback);
            startService(intent);
            renderState(lane + "=DISPATCHED\nTERMUX_PACKAGE=PRESENT\nRESULT=WAITING\nEVIDENCE=AWAITING_TERMUX_CALLBACK");
        } catch (SecurityException e) { renderState(lane + "=BLOCKED\nRESULT=UNKNOWN\nEVIDENCE=PERMISSION_OR_EXTERNAL_APP_POLICY"); }
        catch (Exception e) { renderState(lane + "=ERROR\nRESULT=UNKNOWN\nEVIDENCE=" + e.getClass().getSimpleName()); }
    }

    private void render() {
        String evidence = "";
        try { if (evidenceFile().isFile()) evidence = new String(Files.readAllBytes(evidenceFile().toPath()), StandardCharsets.UTF_8); } catch (Exception ignored) { }
        String packageState = getPackageManager().hasPackage(TermuxConstants.TERMUX_PACKAGE_NAME) ? "PRESENT" : "ABSENT";
        String apiState = getPackageManager().hasPackage(TermuxConstants.TERMUX_API_PACKAGE_NAME) ? "PRESENT" : "ABSENT";
        renderState("TERMUX_EXECUTION=" + packageState + "\nTERMUX_API=" + apiState + "\n" + (evidence.isEmpty() ? "RESULT=UNKNOWN\nEVIDENCE=NOT_MEASURED" : evidence));
    }
    private void renderState(String value) { if (report != null) report.setText(value); }
    private TextView text(String value, int size, boolean bold) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setGravity(Gravity.CENTER_VERTICAL); if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD); return t; }
    private Button button(String value, android.view.View.OnClickListener listener) { Button b = new Button(this); b.setText(value); b.setMinHeight(dp(48)); b.setOnClickListener(listener); return b; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
