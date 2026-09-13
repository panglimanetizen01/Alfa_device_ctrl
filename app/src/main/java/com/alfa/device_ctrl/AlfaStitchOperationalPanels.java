package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.security.NetworkSecurityPolicy;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Native operational panels derived from the Stitch v1 state catalog, backed only by observed Android/runtime evidence. */
public final class AlfaStitchOperationalPanels {
    private AlfaStitchOperationalPanels() { }

    public static View build(Activity activity, AlfaUiNavigation.Screen screen, RuntimeSessionManager sessionManager,
            Runnable appearanceAction, Runnable refreshAction) {
        switch (screen) {
            case NETWORK:
                return AlfaNetworkPanel.build(activity);
            case SECURITY:
                return security(activity);
            case STORAGE:
                return storage(activity);
            case AUDIT:
                return audit(activity);
            case PROJECT:
                return project(activity);
            case SESSIONS:
                return sessions(activity, sessionManager);
            case SPLIT:
                return split(activity, sessionManager);
            case FLOATING:
                return floating(activity);
            case APPEARANCE:
                return appearance(activity, appearanceAction);
            case SETTINGS:
                return settings(activity, refreshAction, appearanceAction);
            case DIAGNOSTICS:
                return diagnostics(activity, sessionManager);
            default:
                return evidence(activity, screen.name(), "No dedicated operational panel is defined for this state.");
        }
    }

    private static View security(Activity activity) {
        StringBuilder out = new StringBuilder();
        out.append("SECURITY DIAGNOSTIC CONSOLE\n");
        out.append("===========================\n");
        out.append("package=").append(activity.getPackageName()).append('\n');
        out.append("sdk=").append(Build.VERSION.SDK_INT).append('\n');
        out.append("debuggable=").append((activity.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0).append('\n');
        out.append("cleartext_permitted=").append(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted()).append('\n');
        out.append("network_security_policy=OBSERVED\n");
        out.append("ptrace_bypass=NOT_CLAIMED_FROM_APP_SANDBOX\n");
        out.append("seccomp_filter_state=NOT_CLAIMED_FROM_APP_SANDBOX\n");
        out.append("privilege_escalation=NOT_CLAIMED\n");
        out.append("\nSecurity controls shown by this panel are observations, not assertions about kernel policy that the app cannot read.");
        return evidence(activity, "SECURITY", out.toString());
    }

    private static View storage(Activity activity) {
        StringBuilder out = new StringBuilder();
        out.append("STORAGE / VFS POLICY\n");
        out.append("===================\n");
        out.append("app_files_dir=").append(activity.getFilesDir().getAbsolutePath()).append('\n');
        File external = activity.getExternalFilesDir(null);
        out.append("app_external_files_dir=").append(external == null ? "UNAVAILABLE" : external.getAbsolutePath()).append('\n');
        out.append("canonical_host_root=").append(StitchV1ReferenceCatalog.CANONICAL_HOST_ROOT).append('\n');
        out.append("host_direct_access_from_app=NOT_ASSUMED\n");
        out.append("host_visibility_proof=SAF_OR_DEVICE_LAYER_REQUIRED\n");
        out.append("runtime_vault=").append(new File(activity.getFilesDir(), "runtime-vault").getAbsolutePath()).append('\n');
        out.append("\nTop-level app files:\n");
        appendChildren(out, activity.getFilesDir(), 32);
        return evidence(activity, "STORAGE", out.toString());
    }

    private static View audit(Activity activity) {
        int security = 0;
        int network = 0;
        int settings = 0;
        int split = 0;
        int floating = 0;
        int sessions = 0;
        int runtime = 0;
        int terminal = 0;
        int storage = 0;
        for (String id : StitchV1ReferenceCatalog.screenIds()) {
            String category = StitchV1ReferenceCatalog.category(id);
            if ("security".equals(category)) security++;
            else if ("network".equals(category)) network++;
            else if ("settings".equals(category)) settings++;
            else if ("split".equals(category)) split++;
            else if ("floating".equals(category)) floating++;
            else if ("sessions".equals(category)) sessions++;
            else if ("runtime".equals(category)) runtime++;
            else if ("terminal".equals(category)) terminal++;
            else if (category.contains("storage")) storage++;
        }
        StringBuilder out = new StringBuilder();
        out.append("STITCH V1 PROVENANCE / AUDIT\n");
        out.append("===========================\n");
        out.append("zip_sha256=").append(StitchV1ReferenceCatalog.ZIP_SHA256).append('\n');
        out.append("catalog_screens=").append(StitchV1ReferenceCatalog.size()).append('\n');
        out.append("security=").append(security).append(" network=").append(network).append(" settings=").append(settings).append('\n');
        out.append("storage=").append(storage).append(" split=").append(split).append(" floating=").append(floating).append('\n');
        out.append("sessions=").append(sessions).append(" runtime=").append(runtime).append(" terminal=").append(terminal).append('\n');
        out.append("reference_catalog=CANONICAL\n");
        out.append("implementation_mode=NATIVE_ANDROID_ADAPTERS\n");
        out.append("mock_telemetry=NOT_USED\n");
        return evidence(activity, "AUDIT", out.toString());
    }

    private static View project(Activity activity) {
        StringBuilder out = new StringBuilder();
        out.append("PROJECT EXPLORER\n");
        out.append("================\n");
        out.append("workspace_backend=ANDROID_APP_FILESYSTEM\n");
        out.append("workspace_root=").append(activity.getFilesDir().getAbsolutePath()).append('\n');
        out.append("canonical_host_root=").append(StitchV1ReferenceCatalog.CANONICAL_HOST_ROOT).append('\n');
        out.append("host_browser=SAF_REQUIRED\n\n");
        appendTree(out, activity.getFilesDir(), "", 2, 64);
        return evidence(activity, "PROJECT", out.toString());
    }

    private static View sessions(Activity activity, RuntimeSessionManager manager) {
        StringBuilder out = new StringBuilder();
        out.append("SESSION MANAGER\n");
        out.append("===============\n");
        if (manager == null) {
            out.append("manager=UNAVAILABLE\n");
        } else {
            out.append("running=").append(manager.isRunning()).append('\n');
            out.append("prompt_ready=").append(manager.isPromptReady()).append('\n');
            if (manager.currentSession() == null) {
                out.append("current_session=NONE\n");
            } else {
                out.append("current_session_pid=").append(manager.currentSession().getPid()).append('\n');
                out.append("current_session=ACTIVE\n");
            }
        }
        out.append("multi_pty_backend=NOT_EXPOSED_BY_CANONICAL_MANAGER\n");
        out.append("reference_states_for_multiple_sessions=CATALOG_ONLY_UNTIL_BACKEND_SUPPORTS_THEM\n");
        return evidence(activity, "SESSIONS", out.toString());
    }

    private static View split(Activity activity, RuntimeSessionManager manager) {
        LinearLayout root = base(activity, "SPLIT PANE TERMINAL WORKSPACE");
        TextView primary = text(activity, "PRIMARY PANE\n" + sessionState(manager), AlfaUiTheme.TEXT, 11, true);
        root.addView(primary, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView secondary = text(activity,
                "SECONDARY PANE\nATTACHMENT=NOT_AVAILABLE\n\nThe Stitch split-pane presentation is implemented as a native workspace state, but no second PTY is fabricated. The canonical backend currently exposes one current session.",
                AlfaUiTheme.TEXT_MUTED, 11, true);
        secondary.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
        root.addView(secondary, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private static View floating(Activity activity) {
        boolean allowed = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(activity);
        LinearLayout root = base(activity, "FLOATING TERMINAL / OVERLAY");
        TextView state = text(activity,
                "overlay_permission=" + allowed + "\n" +
                "window_mode=" + (allowed ? "CAPABLE" : "BLOCKED") + "\n" +
                "interactive_overlay=NOT_STARTED_BY_THIS_PANEL\n\n" +
                "No fake floating window is rendered. Android requires the overlay capability before a window can be placed above other applications.",
                AlfaUiTheme.TEXT, 11, true);
        state.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
        root.addView(state, new LinearLayout.LayoutParams(-1, 0, 1));
        Button grant = button(activity, allowed ? "OVERLAY ALREADY ALLOWED" : "OPEN OVERLAY PERMISSION", allowed ? AlfaUiTheme.READY : AlfaUiTheme.CYAN);
        grant.setEnabled(!allowed);
        grant.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        });
        root.addView(grant, full48(activity));
        return root;
    }

    private static View appearance(Activity activity, Runnable appearanceAction) {
        LinearLayout root = base(activity, "APPEARANCE / TYPOGRAPHY");
        root.addView(text(activity,
                "Native terminal appearance controls\n\n" +
                "font_sizes=11sp,13sp,15sp\n" +
                "line_heights=1.0x,1.25x,1.5x\n" +
                "themes=obsidian,amber,high-contrast\n" +
                "font_family=system monospace / terminal renderer\n" +
                "persistence=Activity preferences (current implementation)\n",
                AlfaUiTheme.TEXT, 11, true), new LinearLayout.LayoutParams(-1, 0, 1));
        Button open = button(activity, "OPEN TERMINAL APPEARANCE", AlfaUiTheme.CYAN);
        open.setOnClickListener(v -> appearanceAction.run());
        root.addView(open, full48(activity));
        return root;
    }

    private static View settings(Activity activity, Runnable refreshAction, Runnable appearanceAction) {
        LinearLayout root = base(activity, "ALFA CONTROL CENTER");
        root.addView(text(activity,
                "CONTROL CENTER\n\n" +
                "Runtime execution remains owned by RuntimeSessionManager.\n" +
                "Network evidence remains read-only.\n" +
                "Stitch reference states are mapped to native panels without mock telemetry.\n" +
                "Reference ZIP SHA256: " + StitchV1ReferenceCatalog.ZIP_SHA256 + "\n",
                AlfaUiTheme.TEXT, 11, true), new LinearLayout.LayoutParams(-1, 0, 1));
        Button appearance = button(activity, "APPEARANCE", AlfaUiTheme.CYAN);
        appearance.setOnClickListener(v -> appearanceAction.run());
        root.addView(appearance, full48(activity));
        Button refresh = button(activity, "REFRESH EVIDENCE", AlfaUiTheme.TEXT);
        refresh.setOnClickListener(v -> refreshAction.run());
        root.addView(refresh, full48(activity));
        return root;
    }

    private static View diagnostics(Activity activity, RuntimeSessionManager manager) {
        StringBuilder out = new StringBuilder();
        out.append("DIAGNOSTIC ENGINE\n");
        out.append("=================\n");
        out.append(sessionState(manager)).append('\n');
        out.append("runtime_registry_count=").append(RuntimeRegistry.all().size()).append('\n');
        out.append("stitch_catalog_count=").append(StitchV1ReferenceCatalog.size()).append('\n');
        out.append("network_panel=LIVE_READ_ONLY\n");
        out.append("security_panel=OBSERVED_ONLY\n");
        out.append("unsupported_kernel_claims=BLOCKED\n");
        return evidence(activity, "DIAGNOSTICS", out.toString());
    }

    private static String sessionState(RuntimeSessionManager manager) {
        if (manager == null) return "manager=UNAVAILABLE";
        if (!manager.isRunning()) return "manager=STOPPED";
        if (manager.isPromptReady()) return "manager=READY pid=" + (manager.currentSession() == null ? "UNKNOWN" : manager.currentSession().getPid());
        return "manager=RUNNING_PROMPT_NOT_READY";
    }

    private static View evidence(Activity activity, String title, String body) {
        LinearLayout root = base(activity, title);
        ScrollView scroll = new ScrollView(activity);
        TextView output = text(activity, body, AlfaUiTheme.TEXT, 11, true);
        output.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 20));
        scroll.addView(output);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private static LinearLayout base(Activity activity, String title) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AlfaUiTheme.CANVAS);
        root.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), 0);
        TextView header = text(activity, title, AlfaUiTheme.CYAN, 13, true);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(activity, 56)));
        return root;
    }

    private static TextView text(Context context, String value, int color, int size, boolean mono) {
        TextView v = new TextView(context);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(size);
        if (mono) v.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL);
        return v;
    }

    private static Button button(Context context, String label, int color) {
        Button b = new Button(context);
        b.setText(label);
        b.setTextColor(color);
        b.setMinHeight(dp(context, 48));
        b.setMinWidth(dp(context, 48));
        return b;
    }

    private static LinearLayout.LayoutParams full48(Context context) {
        return new LinearLayout.LayoutParams(-1, dp(context, 48));
    }

    private static int dp(Context context, int value) { return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f); }

    private static void appendChildren(StringBuilder out, File dir, int max) {
        File[] children = dir.listFiles();
        if (children == null) { out.append("  <unreadable>\n"); return; }
        List<String> names = new ArrayList<>();
        for (File child : children) names.add(child.getName() + (child.isDirectory() ? "/" : ""));
        Collections.sort(names);
        for (int i = 0; i < Math.min(max, names.size()); i++) out.append("  ").append(names.get(i)).append('\n');
        if (names.size() > max) out.append("  … truncated at ").append(max).append(" entries\n");
    }

    private static void appendTree(StringBuilder out, File dir, String prefix, int depth, int max) {
        if (depth < 0 || max <= 0) return;
        File[] children = dir.listFiles();
        if (children == null) { out.append(prefix).append("<unreadable>\n"); return; }
        List<File> sorted = new ArrayList<>();
        Collections.addAll(sorted, children);
        Collections.sort(sorted, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        int count = 0;
        for (File child : sorted) {
            if (count++ >= max) { out.append(prefix).append("… truncated\n"); break; }
            out.append(prefix).append(child.getName()).append(child.isDirectory() ? "/" : "").append('\n');
            if (child.isDirectory()) appendTree(out, child, prefix + "  ", depth - 1, max);
        }
    }
}
