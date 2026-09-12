package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

/** Final-shell navigation bindings that expose evidence-backed secondary panels. */
public final class AlfaFinalUiHooks {
    private static final int TAG_ID = 0xA1FA002;
    private AlfaFinalUiHooks() { }

    public static void apply(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (!(root instanceof ViewGroup) || root.getTag(TAG_ID) != null) return;
        Button network = findButton((ViewGroup) root, "Alfa RF");
        Button diagnostics = findButton((ViewGroup) root, "Diagnostics");
        if (network != null) network.setOnClickListener(v -> showOverlay(activity, AlfaNetworkPanel.build(activity)));
        if (diagnostics != null) diagnostics.setOnClickListener(v -> showOverlay(activity, diagnostics(activity)));
        root.setTag(TAG_ID, Boolean.TRUE);
    }

    private static View diagnostics(Activity activity) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(AlfaUiTheme.CANVAS);
        panel.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), 0);
        TextView title = text(activity, "DIAGNOSTICS  •  EVIDENCE CONSOLE", AlfaUiTheme.CYAN, 14, true);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(activity, 52)));
        TextView body = text(activity, collectRuntimeEvidence(activity), AlfaUiTheme.TEXT, 11, true);
        ScrollView scroll = new ScrollView(activity); scroll.addView(body); panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return panel;
    }

    private static String collectRuntimeEvidence(Activity activity) {
        StringBuilder out = new StringBuilder("RUNTIME REGISTRY\n");
        File vault = new File(activity.getFilesDir(), "runtime-vault");
        File engine = new File(activity.getApplicationInfo().nativeLibraryDir, "libproot.so");
        List<RuntimeProfile> profiles = RuntimeRegistry.all();
        for (RuntimeProfile profile : profiles) {
            File runtime = new File(new File(vault, "runtimes"), profile.id());
            File ready = new File(runtime, "READY.evidence");
            File rootfs = new File(runtime, "rootfs");
            RuntimeUiState.Status state = RuntimeUiState.resolve(profile.id(), runtime, ready, engine, rootfs);
            out.append(profile.id()).append(" status=").append(RuntimeUiState.label(state)).append('\n');
            out.append("  runtime_dir=").append(runtime.exists()).append(" ready_evidence=").append(ready.exists()).append(" rootfs=").append(rootfs.exists()).append('\n');
        }
        out.append("native_libproot=").append(engine.exists()).append('\n');
        out.append("\nNETWORK: use Alfa RF for live ConnectivityManager/LinkProperties evidence.\n");
        out.append("NO MOCK TELEMETRY: unavailable privileged data is not synthesized.\n");
        return out.toString();
    }

    private static void showOverlay(Activity activity, View panel) {
        ViewGroup content = (ViewGroup) activity.findViewById(android.R.id.content);
        if (content == null) return;
        FrameOverlay.show(activity, content, panel);
    }

    private static Button findButton(ViewGroup root, String text) {
        View view = findText(root, text); return view instanceof Button ? (Button) view : null;
    }
    private static View findText(View root, String text) {
        if (root instanceof TextView && text.contentEquals(((TextView) root).getText())) return root;
        if (root instanceof ViewGroup) { ViewGroup group = (ViewGroup) root; for (int i = 0; i < group.getChildCount(); i++) { View result = findText(group.getChildAt(i), text); if (result != null) return result; } }
        return null;
    }
    private static TextView text(Activity a, String value, int color, int size, boolean mono) { TextView t = new TextView(a); t.setText(value); t.setTextColor(color); t.setTextSize(size); if (mono) t.setTypeface(Typeface.MONOSPACE); return t; }
    private static int dp(Activity a, int value) { return Math.round(value * a.getResources().getDisplayMetrics().density); }

    private static final class FrameOverlay {
        static void show(Activity activity, ViewGroup content, View panel) {
            android.widget.FrameLayout overlay = new android.widget.FrameLayout(activity);
            overlay.setBackgroundColor(AlfaUiTheme.CANVAS);
            overlay.addView(panel, new android.widget.FrameLayout.LayoutParams(-1, -1));
            Button close = new Button(activity); close.setText("BACK"); close.setTextColor(AlfaUiTheme.CYAN); close.setMinHeight(dp(activity, 48)); close.setOnClickListener(v -> content.removeView(overlay));
            android.widget.FrameLayout.LayoutParams closeParams = new android.widget.FrameLayout.LayoutParams(dp(activity, 76), dp(activity, 48), android.view.Gravity.TOP | android.view.Gravity.END);
            overlay.addView(close, closeParams); content.addView(overlay, new android.widget.FrameLayout.LayoutParams(-1, -1));
        }
    }
}
