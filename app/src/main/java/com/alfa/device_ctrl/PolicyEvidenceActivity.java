package com.alfa.device_ctrl;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

/** Security/policy surface whose status is derived from the real interactive-session contract. */
public final class PolicyEvidenceActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AlfaUiTheme.CANVAS);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));

        TextView title = text("SECURITY / INTERACTIVE POLICY", AlfaUiTheme.TEXT, 16, true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(52)));

        String runtimeId = AlfaSettingsStore.get(this).getRuntimeId(RuntimeSelection.DEFAULT_RUNTIME_ID);
        StringBuilder r = new StringBuilder();
        r.append("POLICY_ID=").append(InteractiveSessionContract.POLICY_ID).append('\n');
        r.append("POLICY_VERSION=").append(InteractiveSessionContract.POLICY_VERSION).append('\n');
        r.append("POLICY_SCOPE=").append(InteractiveSessionContract.POLICY_SCOPE).append('\n');
        r.append("RUNTIME_ID=").append(runtimeId).append('\n');
        r.append("ANDROID_ROOT=NOT_GRANTED_BY_APP\nOTHER_APP_DATA=NOT_GRANTED_BY_APP\n");

        RuntimeProfile profile = RuntimeSelection.profile(runtimeId);
        if (profile == null) {
            r.append("CONTRACT_STATUS=BLOCKED\nREASON=unsupported-runtime-id");
        } else {
            File files = getFilesDir();
            File vault = new File(files, "runtime-vault");
            File runtime = new File(new File(vault, "runtimes"), runtimeId);
            File ready = new File(runtime, "READY.evidence");
            File launch = new File(vault, "gate7-launch.properties");
            File engine = new File(getApplicationInfo().nativeLibraryDir, "libproot.so");
            File rootfs = new File(runtime, "rootfs");
            r.append("GATE7_LAUNCH=").append(Gate6LaunchContract.verify(launch, runtimeId) ? "PASS" : "BLOCKED").append('\n');
            r.append("RUNTIME_READY_EVIDENCE=").append(RuntimeEvidence.verify(ready, runtimeId, engine, rootfs) ? "PASS" : "BLOCKED").append('\n');
            try {
                File cwd = new File(files, "session-cwd");
                if (!cwd.exists() && !cwd.mkdirs()) throw new IllegalStateException("session-cwd-unavailable");
                InteractiveSessionContract contract = new InteractiveSessionContract(
                        "policy-inspection", "policy-inspection", "policy-inspection", runtimeId,
                        ready, engine, rootfs, cwd,
                        new String[]{"HOME=/root", "TERM=xterm-256color", "PS1=alfa:" + runtimeId + ":\\w\\$ ",
                                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                                "PROOT_TMP_DIR=" + new File(runtime, "proot_tmp").getAbsolutePath()});
                r.append("CONTRACT_STATUS=").append(contract.isAuthorizedForInteractiveRuntime() ? "AUTHORIZED" : "BLOCKED").append('\n');
                r.append("PROOT_LOADER_POLICY=REQUIRED_AND_RUNTIME_BOUND\n");
                r.append("RUNTIME_ROOT=").append(rootfs.getCanonicalPath()).append('\n');
                r.append("HOST_CWD=").append(cwd.getCanonicalPath());
            } catch (Exception e) {
                r.append("CONTRACT_STATUS=BLOCKED\nREASON=").append(e.getClass().getSimpleName()).append(':').append(e.getMessage());
            }
        }

        TextView body = text(r.toString(), AlfaUiTheme.TEXT_MUTED, 11, false);
        body.setTypeface(android.graphics.Typeface.MONOSPACE);
        body.setGravity(Gravity.TOP | Gravity.START);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private TextView text(String value, int color, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        if (bold) view.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
