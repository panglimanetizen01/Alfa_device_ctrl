package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Native Stitch v1 operational surfaces; execution/evidence remain owned by Alfa boundaries. */
public final class AlfaStitchOperationalPanels {
    private static final int PAD = 12;
    private static final int RADIUS = 4;
    private AlfaStitchOperationalPanels() { }

    public static View build(Activity activity, AlfaUiNavigation.Screen screen, RuntimeSessionManager manager, Runnable appearanceAction, Runnable refreshAction) {
        switch (screen) {
            case NETWORK: return network(activity);
            case SECURITY: return security(activity);
            case STORAGE: return storage(activity);
            case AUDIT: return audit(activity);
            case PROJECT: return project(activity);
            case SESSIONS: return sessions(activity, manager);
            case SPLIT: return split(activity, manager);
            case FLOATING: return floating(activity);
            case APPEARANCE: return appearance(activity, appearanceAction);
            case SETTINGS: return settings(activity, refreshAction, appearanceAction);
            case DIAGNOSTICS: return diagnostics(activity, manager);
            default: return evidence(activity, screen.name(), "This state is handled by the canonical terminal/runtime surface.");
        }
    }

    private static View network(Activity activity) {
        LinearLayout root = base(activity, "NETWORK EVIDENCE");
        root.addView(AlfaNetworkPanel.build(activity), new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private static View security(Activity activity) {
        LinearLayout root = base(activity, "SECURITY DIAGNOSTICS");
        LinearLayout body = column(activity);
        addSection(body, activity, "APPLICATION", "Package", activity.getPackageName(), "SDK", Integer.toString(Build.VERSION.SDK_INT), "Debuggable", Boolean.toString((activity.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0));
        addSection(body, activity, "NETWORK SECURITY", "Cleartext permitted", Boolean.toString(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted()), "Policy", "OBSERVED", "VPN / firewall mutation", "BLOCKED BY UI BOUNDARY");
        addSection(body, activity, "KERNEL / PRIVILEGE", "ptrace", "NOT_CLAIMED_FROM_APP_SANDBOX", "seccomp", "NOT_CLAIMED_FROM_APP_SANDBOX", "privilege escalation", "NOT_CLAIMED");
        addCapability(body, activity, "NO MOCK TELEMETRY", "Security state is evidence-only; unsupported kernel state is not fabricated.", AlfaUiTheme.READY);
        ScrollView scroll = new ScrollView(activity); scroll.addView(body); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private static View storage(Activity activity) {
        LinearLayout root = base(activity, "STORAGE / VFS POLICY");
        LinearLayout body = column(activity);
        addSection(body, activity, "APP STORAGE", "filesDir", activity.getFilesDir().getAbsolutePath(), "externalFilesDir", externalFiles(activity), "runtime vault", new File(activity.getFilesDir(), "runtime-vault").getAbsolutePath());
        addSection(body, activity, "HOST STORAGE BOUNDARY", "canonical host root", StitchV1ReferenceCatalog.CANONICAL_HOST_ROOT, "direct app access", "NOT_ASSUMED", "host visibility proof", "SAF_OR_DEVICE_LAYER_REQUIRED");
        LinearLayout policy = card(activity);
        policy.addView(sectionTitle(activity, "DIRECTORY OVERRIDE POLICY"), full48(activity));
        policy.addView(chip(activity, "SAF_REQUIRED", AlfaUiTheme.WARNING), full48(activity));
        policy.addView(chip(activity, "POSIX_POLICY_GUARDS", AlfaUiTheme.CYAN), full48(activity));
        policy.addView(chip(activity, "RW / RO / ISOLATED", AlfaUiTheme.TEXT), full48(activity));
        Button browse = rowButton(activity, "BROWSE HOST DIRECTORY", AlfaUiTheme.CYAN, v -> { Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); activity.startActivityForResult(intent, 0xA1F4); });
        policy.addView(browse, full48(activity)); body.addView(policy, wrap());
        ScrollView scroll = new ScrollView(activity); scroll.addView(body); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private static View audit(Activity activity) {
        LinearLayout root = base(activity, "AUDIT / PROVENANCE");
        LinearLayout body = column(activity);
        addSection(body, activity, "STITCH V1 SOURCE", "ZIP SHA-256", StitchV1ReferenceCatalog.ZIP_SHA256, "catalog states", Integer.toString(StitchV1ReferenceCatalog.size()), "mode", "NATIVE_ANDROID_ADAPTERS");
        addSection(body, activity, "EXECUTION PROVENANCE", "mock telemetry", "NOT_USED", "source artifact binding", "LOCKED", "reference HTML/JS", "NOT_EXECUTED");
        addCapability(body, activity, "PROVENANCE BOUND", "The raw Stitch ZIP remains the implementation input and audit anchor.", AlfaUiTheme.READY);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1)); return root;
    }

    private static View project(Activity activity) {
        LinearLayout root = base(activity, "PROJECT EXPLORER"); LinearLayout body = column(activity);
        addSection(body, activity, "WORKSPACE", "backend", "ANDROID_APP_FILESYSTEM", "workspace root", activity.getFilesDir().getAbsolutePath(), "host browser", "SAF_REQUIRED");
        LinearLayout treeCard = card(activity); treeCard.addView(sectionTitle(activity, "APP FILE TREE"), full48(activity));
        TextView tree = mono(activity, ""); tree.setTextColor(AlfaUiTheme.TEXT_MUTED); tree.setTextSize(11); tree.setPadding(dp(activity, PAD), dp(activity, 4), dp(activity, PAD), dp(activity, 12)); StringBuilder out = new StringBuilder(); appendTree(out, activity.getFilesDir(), "", 2, 64); tree.setText(out.toString()); treeCard.addView(tree, wrap()); body.addView(treeCard, wrap());
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1)); return root;
    }

    private static View sessions(Activity activity, RuntimeSessionManager manager) {
        LinearLayout root = base(activity, "SESSION MULTIPLEXER"); LinearLayout body = column(activity);
        LinearLayout filters = card(activity); filters.addView(chip(activity, "ALL 15", AlfaUiTheme.CYAN), full48(activity)); filters.addView(chip(activity, "UBUNTU (5)", AlfaUiTheme.READY), full48(activity)); filters.addView(chip(activity, "KALI (5)", AlfaUiTheme.CYAN), full48(activity)); filters.addView(chip(activity, "ALPINE (5)", AlfaUiTheme.WARNING), full48(activity)); body.addView(filters, wrap());
        LinearLayout live = card(activity); live.addView(sectionTitle(activity, "CANONICAL SESSION"), full48(activity)); String state = manager == null ? "MANAGER=UNAVAILABLE" : manager.isPromptReady() ? "READY" : manager.isRunning() ? "PTY_WAITING" : "STOPPED"; live.addView(chip(activity, "STATE=" + state, AlfaUiTheme.statusColor(state)), full48(activity)); String pid = manager != null && manager.currentSession() != null ? Integer.toString(manager.currentSession().getPid()) : "NONE"; live.addView(mono(activity, "PID=" + pid + "\nMULTI_PTY_BACKEND=NOT_EXPOSED_BY_CANONICAL_MANAGER"), wrap());
        LinearLayout actions = row(activity, AlfaUiTheme.SURFACE_1); Button spawn = rowButton(activity, "SPAWN +", AlfaUiTheme.READY, v -> { if (manager != null) manager.start(80, 24, 0, 0); }); Button stop = rowButton(activity, "STOP SESSION", AlfaUiTheme.ERROR, v -> { if (manager != null) manager.stop(); }); actions.addView(spawn, new LinearLayout.LayoutParams(0, dp(activity, 48), 1)); actions.addView(stop, new LinearLayout.LayoutParams(0, dp(activity, 48), 1)); live.addView(actions, wrap()); body.addView(live, wrap());
        LinearLayout list = card(activity); list.addView(sectionTitle(activity, "REFERENCE SESSION LANES"), full48(activity)); String[] runtimes = {"UBUNTU", "KALI", "ALPINE"};
        for (String runtime : runtimes) for (int i = 1; i <= 5; i++) { LinearLayout r = row(activity, AlfaUiTheme.SURFACE_1); TextView label = text(activity, runtime + " • SESSION " + i, AlfaUiTheme.TEXT, 11, true); r.addView(label, new LinearLayout.LayoutParams(0, dp(activity, 48), 1)); Button switcher = rowButton(activity, "SWITCH SESSION", AlfaUiTheme.TEXT_MUTED, null); switcher.setEnabled(false); switcher.setContentDescription("Switch session belum tersedia di canonical manager"); r.addView(switcher, new LinearLayout.LayoutParams(dp(activity, 140), dp(activity, 48))); list.addView(r, wrap()); }
        list.addView(chip(activity, "REFERENCE_MULTIPLE_SESSIONS=CATALOG_STATE_WITHOUT_FABRICATED_PTYS", AlfaUiTheme.WARNING), full48(activity)); body.addView(list, wrap());
        ScrollView scroll = new ScrollView(activity); scroll.addView(body); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); return root;
    }

    private static View split(Activity activity, RuntimeSessionManager manager) {
        LinearLayout root = base(activity, "SPLIT VIEW"); LinearLayout controls = card(activity); controls.addView(chip(activity, "HORIZONTAL", AlfaUiTheme.CYAN), full48(activity)); controls.addView(chip(activity, "VERTICAL", AlfaUiTheme.TEXT), full48(activity)); controls.addView(chip(activity, "SYNC INPUT", AlfaUiTheme.TEXT_MUTED), full48(activity)); controls.addView(chip(activity, "SECOND PTY=NOT_EXPOSED_BY_CANONICAL_MANAGER", AlfaUiTheme.WARNING), full48(activity)); root.addView(controls, wrap());
        LinearLayout panes = row(activity, AlfaUiTheme.CANVAS); panes.setWeightSum(2f); panes.addView(pane(activity, "PRIMARY PANE", sessionState(manager), AlfaUiTheme.TEXT), new LinearLayout.LayoutParams(0, -1, 1)); panes.addView(pane(activity, "SECONDARY PANE", "ATTACHMENT=NOT_AVAILABLE\n\nNo second PTY is fabricated.", AlfaUiTheme.TEXT_MUTED), new LinearLayout.LayoutParams(0, -1, 1)); root.addView(panes, new LinearLayout.LayoutParams(-1, 0, 1)); return root;
    }

    private static View floating(Activity activity) {
        boolean allowed = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(activity); LinearLayout root = base(activity, "FLOATING TERMINAL"); LinearLayout body = column(activity);
        addSection(body, activity, "OVERLAY CAPABILITY", "overlay permission", Boolean.toString(allowed), "window mode", allowed ? "CAPABLE" : "BLOCKED", "interactive overlay", "NOT_STARTED_BY_THIS_PANEL");
        Button open = rowButton(activity, allowed ? "OVERLAY ALREADY ALLOWED" : "OPEN OVERLAY PERMISSION", allowed ? AlfaUiTheme.READY : AlfaUiTheme.CYAN, v -> { if (!allowed) activity.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()))); }); body.addView(open, full48(activity)); body.addView(chip(activity, "NO_FAKE_FLOATING_WINDOW", AlfaUiTheme.WARNING), full48(activity)); root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1)); return root;
    }

    private static View appearance(Activity activity, Runnable appearanceAction) {
        LinearLayout root = base(activity, "APPEARANCE / TYPOGRAPHY"); LinearLayout body = column(activity);
        LinearLayout presets = card(activity); presets.addView(sectionTitle(activity, "APPEARANCE PRESETS"), full48(activity)); presets.addView(chip(activity, "Terminal Obsidian", AlfaUiTheme.READY), full48(activity)); presets.addView(chip(activity, "Obsidian Cyan Sub-zero", AlfaUiTheme.CYAN), full48(activity)); presets.addView(chip(activity, "Classic Amber VT220", AlfaUiTheme.WARNING), full48(activity)); presets.addView(chip(activity, "High Contrast", AlfaUiTheme.TEXT), full48(activity)); body.addView(presets, wrap());
        LinearLayout typography = card(activity); typography.addView(sectionTitle(activity, "TYPOGRAPHY"), full48(activity)); typography.addView(chip(activity, "SMALL", AlfaUiTheme.TEXT_MUTED), full48(activity)); typography.addView(chip(activity, "DEFAULT • 13sp", AlfaUiTheme.READY), full48(activity)); typography.addView(chip(activity, "LARGE", AlfaUiTheme.TEXT), full48(activity)); typography.addView(chip(activity, "XL", AlfaUiTheme.CYAN), full48(activity)); typography.addView(chip(activity, "FONT SIZE 13sp  −  +", AlfaUiTheme.TEXT), full48(activity)); typography.addView(chip(activity, "LINE HEIGHT 1.25x  −  +", AlfaUiTheme.TEXT), full48(activity)); typography.addView(chip(activity, "Geist Sans (UI) / JetBrains Mono (PTY)", AlfaUiTheme.TEXT_MUTED), full48(activity)); typography.addView(chip(activity, "CURSOR • BLOCK / UNDERLINE / BAR", AlfaUiTheme.TEXT), full48(activity)); body.addView(typography, wrap());
        Button open = rowButton(activity, "OPEN TERMINAL APPEARANCE", AlfaUiTheme.CYAN, v -> appearanceAction.run()); body.addView(open, full48(activity)); body.addView(chip(activity, "SCROLLBACK 10,000 • IME ACCESSORY BAR • MULTILINE PASTE CONFIRMATION", AlfaUiTheme.TEXT_MUTED), full48(activity));
        ScrollView scroll = new ScrollView(activity); scroll.addView(body); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); return root;
    }

    private static View settings(Activity activity, Runnable refreshAction, Runnable appearanceAction) {
        LinearLayout root = base(activity, "ALFA CONTROL CENTER"); LinearLayout body = column(activity); addSection(body, activity, "ALFA DEVICE CTRL", "presentation", "NATIVE_STITCH_ADAPTER", "runtime execution", "RuntimeSessionManager", "network evidence", "READ_ONLY"); addSection(body, activity, "CONTROL CENTER", "terminal", "INTERACTIVE PTY", "appearance", "STITCH_NATIVE_SURFACE", "diagnostics", "EVIDENCE_BOUND"); Button appearance = rowButton(activity, "APPEARANCE", AlfaUiTheme.CYAN, v -> appearanceAction.run()); body.addView(appearance, full48(activity)); Button refresh = rowButton(activity, "REFRESH EVIDENCE", AlfaUiTheme.READY, v -> refreshAction.run()); body.addView(refresh, full48(activity)); body.addView(chip(activity, "STITCH ZIP SHA256=" + StitchV1ReferenceCatalog.ZIP_SHA256, AlfaUiTheme.TEXT_MUTED), full48(activity)); root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1)); return root;
    }

    private static View diagnostics(Activity activity, RuntimeSessionManager manager) {
        LinearLayout root = base(activity, "DIAGNOSTIC ENGINE"); LinearLayout body = column(activity); addSection(body, activity, "RUNTIME", "session", sessionState(manager), "registry count", Integer.toString(RuntimeRegistry.all().size()), "Stitch states", Integer.toString(StitchV1ReferenceCatalog.size())); addSection(body, activity, "NETWORK", "panel", "LIVE_READ_ONLY", "DNS / routes / interface", "EVIDENCE-BACKED", "privileged mutation", "BLOCKED"); addSection(body, activity, "SECURITY", "sandbox claims", "NOT_CLAIMED_FROM_APP_SANDBOX", "mock telemetry", "NOT_USED", "unsupported kernel state", "BLOCKED"); addCapability(body, activity, "DIAGNOSTIC ENGINE", "Network, runtime, security and storage surfaces report boundaries instead of inventing capabilities.", AlfaUiTheme.READY); root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1)); return root;
    }

    private static View evidence(Activity activity, String title, String body) { LinearLayout root = base(activity, title); ScrollView scroll = new ScrollView(activity); TextView output = mono(activity, body); output.setTextSize(11); output.setPadding(dp(activity, PAD), dp(activity, PAD), dp(activity, PAD), dp(activity, 20)); scroll.addView(output); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); return root; }
    private static LinearLayout base(Activity activity, String title) { LinearLayout root = column(activity); root.setPadding(dp(activity, 8), dp(activity, 4), dp(activity, 8), 0); LinearLayout header = row(activity, AlfaUiTheme.SURFACE_1); TextView label = text(activity, title, AlfaUiTheme.CYAN, 13, true); label.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); header.addView(label, new LinearLayout.LayoutParams(0, -1, 1)); root.addView(header, new LinearLayout.LayoutParams(-1, dp(activity, 56))); return root; }
    private static LinearLayout column(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); l.setBackgroundColor(AlfaUiTheme.CANVAS); return l; }
    private static LinearLayout row(Context c, int background) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); l.setBackgroundColor(background); return l; }
    private static LinearLayout card(Context c) { LinearLayout l = column(c); l.setPadding(dp(c, 6), dp(c, 6), dp(c, 6), dp(c, 6)); GradientDrawable bg = new GradientDrawable(); bg.setColor(AlfaUiTheme.SURFACE_1); bg.setCornerRadius(dp(RADIUS, c)); bg.setStroke(Math.max(1, dp(1, c)), AlfaUiTheme.BORDER); l.setBackground(bg); return l; }
    private static TextView sectionTitle(Context c, String value) { TextView v = text(c, value, AlfaUiTheme.TEXT, 11, true); v.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); return v; }
    private static TextView mono(Context c, String value) { return text(c, value, AlfaUiTheme.TEXT, 11, true); }
    private static TextView text(Context c, String value, int color, int size, boolean mono) { TextView v = new TextView(c); v.setText(value); v.setTextColor(color); v.setTextSize(size); if (mono) v.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private static Button rowButton(Context c, String label, int color, View.OnClickListener listener) { Button b = new Button(c); b.setText(label); b.setTextColor(color); b.setTextSize(10); b.setAllCaps(false); b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); b.setMinHeight(dp(c, AlfaUiTheme.TOUCH_TARGET_DP)); b.setMinWidth(dp(c, AlfaUiTheme.TOUCH_TARGET_DP)); b.setPadding(dp(c, 8), dp(c, 4), dp(c, 8), dp(c, 4)); b.setBackground(controlBackground(c, color)); if (listener != null) b.setOnClickListener(listener); return b; }
    private static TextView chip(Context c, String label, int color) { TextView v = text(c, label, color, 10, true); v.setPadding(dp(c, 10), 0, dp(c, 10), 0); GradientDrawable bg = new GradientDrawable(); bg.setColor(AlfaUiTheme.SURFACE_2); bg.setCornerRadius(dp(4, c)); bg.setStroke(Math.max(1, dp(1, c)), color); v.setBackground(bg); return v; }
    private static void addSection(LinearLayout body, Activity a, String title, String... pairs) { LinearLayout c = card(a); c.addView(sectionTitle(a, title), full48(a)); for (int i = 0; i + 1 < pairs.length; i += 2) { LinearLayout r = row(a, AlfaUiTheme.SURFACE_1); r.addView(text(a, pairs[i], AlfaUiTheme.TEXT_MUTED, 10, true), new LinearLayout.LayoutParams(0, dp(a, 48), 1)); r.addView(text(a, pairs[i + 1], AlfaUiTheme.TEXT, 10, true), new LinearLayout.LayoutParams(0, dp(a, 48), 2)); c.addView(r, wrap()); } body.addView(c, wrap()); }
    private static void addCapability(LinearLayout body, Activity a, String title, String detail, int color) { LinearLayout c = card(a); c.addView(chip(a, title, color), full48(a)); TextView d = text(a, detail, AlfaUiTheme.TEXT_MUTED, 10, false); d.setPadding(dp(a, 10), dp(a, 6), dp(a, 10), dp(a, 10)); c.addView(d, wrap()); body.addView(c, wrap()); }
    private static View pane(Activity a, String title, String value, int color) { LinearLayout c = card(a); c.addView(chip(a, title, color), full48(a)); TextView body = mono(a, value); body.setTextColor(color); body.setPadding(dp(a, 10), dp(a, 8), dp(a, 10), dp(a, 8)); c.addView(body, new LinearLayout.LayoutParams(-1, 0, 1)); return c; }
    private static LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private static LinearLayout.LayoutParams full48(Context c) { return new LinearLayout.LayoutParams(-1, dp(c, AlfaUiTheme.TOUCH_TARGET_DP)); }
    private static GradientDrawable controlBackground(Context c, int accent) { GradientDrawable bg = new GradientDrawable(); bg.setColor(AlfaUiTheme.SURFACE_2); bg.setCornerRadius(dp(4, c)); bg.setStroke(Math.max(1, dp(1, c)), accent); return bg; }
    private static int dp(Context c, int value) { return (int) (value * c.getResources().getDisplayMetrics().density + 0.5f); }
    private static String externalFiles(Activity a) { File f = a.getExternalFilesDir(null); return f == null ? "UNAVAILABLE" : f.getAbsolutePath(); }
    private static String sessionState(RuntimeSessionManager manager) { if (manager == null) return "MANAGER=UNAVAILABLE"; if (!manager.isRunning()) return "STOPPED"; return manager.isPromptReady() ? "READY" : "PTY_WAITING_FOR_PROMPT"; }
    private static void appendTree(StringBuilder out, File dir, String prefix, int depth, int max) { if (depth < 0 || max <= 0) return; File[] children = dir.listFiles(); if (children == null) { out.append(prefix).append("<unreadable>\n"); return; } List<File> sorted = new ArrayList<>(); Collections.addAll(sorted, children); Collections.sort(sorted, (a, b) -> a.getName().compareToIgnoreCase(b.getName())); int count = 0; for (File child : sorted) { if (count++ >= max) { out.append(prefix).append("… truncated\n"); break; } out.append(prefix).append(child.getName()).append(child.isDirectory() ? "/" : "").append('\n'); if (child.isDirectory()) appendTree(out, child, prefix + "  ", depth - 1, max); } }
}
