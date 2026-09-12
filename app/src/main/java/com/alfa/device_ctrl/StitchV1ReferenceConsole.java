package com.alfa.device_ctrl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

/** Native reconciliation console for the supplied Stitch v1 reference corpus. */
public final class StitchV1ReferenceConsole {
    private static final int TAG_ID = 0xA1FA073;
    private static final int BG = Color.rgb(16, 20, 25);
    private static final int SURFACE = Color.rgb(22, 27, 34);
    private static final int SURFACE_2 = Color.rgb(33, 38, 45);
    private static final int TEXT = Color.rgb(240, 246, 252);
    private static final int MUTED = Color.rgb(139, 148, 158);
    private static final int GREEN = Color.rgb(16, 185, 129);
    private static final int CYAN = Color.rgb(56, 189, 248);
    private static final int RED = Color.rgb(239, 68, 68);

    private StitchV1ReferenceConsole() {}

    public static void installEntry(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (!(root instanceof ViewGroup) || root.getTag(TAG_ID) != null) return;
        ViewGroup group = (ViewGroup) root;
        Button entry = new Button(activity);
        entry.setText("REF");
        entry.setTextColor(CYAN);
        entry.setTextSize(9);
        entry.setMinHeight(dp(activity, 48));
        entry.setMinWidth(dp(activity, 48));
        entry.setContentDescription("Stitch v1 reference coverage");
        entry.setOnClickListener(v -> show(activity));
        if (root instanceof FrameLayout) {
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(activity, 56), dp(activity, 48), Gravity.TOP | Gravity.END);
            p.topMargin = dp(activity, 8);
            p.rightMargin = dp(activity, 56);
            group.addView(entry, p);
        }
        group.setTag(TAG_ID, Boolean.TRUE);
    }

    public static void show(Activity activity) {
        final FrameLayout overlay = new FrameLayout(activity);
        overlay.setBackgroundColor(BG);
        LinearLayout root = column(activity, BG);
        root.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), 0);
        root.addView(header(activity, overlay), new LinearLayout.LayoutParams(-1, dp(activity, 58)));

        LinearLayout filters = row(activity, SURFACE);
        String[] cats = {"ALL", "TERMINAL", "RUNTIME", "SESSIONS", "SPLIT", "FLOATING", "SETTINGS", "SECURITY", "NETWORK", "STORAGE"};
        for (String cat : cats) {
            Button b = button(activity, cat, cat.equals("ALL") ? GREEN : TEXT);
            b.setOnClickListener(v -> renderList(activity, root, String.valueOf(b.getText()).toLowerCase(Locale.US)));
            filters.addView(b, new LinearLayout.LayoutParams(0, dp(activity, 46), 1));
        }
        ScrollView filterScroll = new ScrollView(activity);
        filterScroll.setHorizontalScrollBarEnabled(false);
        filterScroll.addView(filters, new ScrollView.LayoutParams(-1, -2));
        root.addView(filterScroll, new LinearLayout.LayoutParams(-1, dp(activity, 54)));

        EditText search = new EditText(activity);
        search.setHint("Filter 159 reference states...");
        search.setHintTextColor(MUTED);
        search.setTextColor(TEXT);
        search.setSingleLine(true);
        search.setTextSize(12);
        search.setBackgroundColor(SURFACE_2);
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(activity, 50)));

        LinearLayout list = column(activity, BG);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(footer(activity), new LinearLayout.LayoutParams(-1, dp(activity, 48)));
        overlay.addView(root, new FrameLayout.LayoutParams(-1, -1));
        activity.addContentView(overlay, new ViewGroup.LayoutParams(-1, -1));
        renderList(activity, root, "all");
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { renderList(activity, root, s.toString().trim().toLowerCase(Locale.US)); }
            public void afterTextChanged(android.text.Editable e) {}
        });
    }

    private static View header(Activity activity, FrameLayout overlay) {
        LinearLayout h = row(activity, BG);
        TextView title = text(activity, "STITCH V1 // RECONCILIATION CONSOLE", GREEN, 13, true);
        h.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
        Button close = button(activity, "CLOSE", TEXT);
        close.setOnClickListener(v -> overlay.setVisibility(View.GONE));
        h.addView(close, new LinearLayout.LayoutParams(dp(activity, 72), dp(activity, 48)));
        return h;
    }

    private static View footer(Activity activity) {
        LinearLayout f = row(activity, SURFACE);
        f.addView(text(activity, "REFERENCE=159/159  •  ZIP SHA256=" + StitchV1ReferenceCatalog.ZIP_SHA256.substring(0, 12) + "…", MUTED, 9, true), new LinearLayout.LayoutParams(0, -1, 1));
        f.addView(text(activity, "CANONICAL=" + StitchV1ReferenceCatalog.CANONICAL_PACKAGE, CYAN, 9, true), new LinearLayout.LayoutParams(-2, -1));
        return f;
    }

    private static void renderList(Activity activity, LinearLayout root, String filter) {
        ScrollView scroll = null;
        for (int i = 0; i < root.getChildCount(); i++) if (root.getChildAt(i) instanceof ScrollView) scroll = (ScrollView) root.getChildAt(i);
        if (scroll == null || !(scroll.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout list = (LinearLayout) scroll.getChildAt(0);
        list.removeAllViews();
        int shown = 0;
        for (String id : StitchV1ReferenceCatalog.screenIds()) {
            String category = StitchV1ReferenceCatalog.category(id);
            boolean match = "all".equals(filter) || category.contains(filter) || id.toLowerCase(Locale.US).contains(filter);
            if (!match) continue;
            shown++;
            Button b = button(activity, category.toUpperCase(Locale.US) + "  //  " + humanize(id), TEXT);
            b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            b.setOnClickListener(v -> showState(activity, id));
            list.addView(b, new LinearLayout.LayoutParams(-1, dp(activity, 52)));
        }
        list.addView(text(activity, "MATCHES=" + shown, MUTED, 9, true), new LinearLayout.LayoutParams(-1, dp(activity, 36)));
    }

    private static void showState(Activity activity, String id) {
        String category = StitchV1ReferenceCatalog.category(id);
        StringBuilder evidence = new StringBuilder();
        evidence.append("REFERENCE STATE\n");
        evidence.append("id=").append(id).append('\n');
        evidence.append("category=").append(category).append('\n');
        evidence.append("disposition=ADAPT_AND_IMPLEMENT\n\n");
        evidence.append("CANONICAL BOUNDARY\n");
        evidence.append("package=").append(StitchV1ReferenceCatalog.CANONICAL_PACKAGE).append('\n');
        evidence.append("host_root=").append(StitchV1ReferenceCatalog.CANONICAL_HOST_ROOT).append('\n');
        evidence.append("reference_zip_is_source_code=false\n\n");
        evidence.append(dispositionFor(category));
        evidence.append("\nLIVE EVIDENCE\n").append(liveEvidence(activity, category));

        LinearLayout box = column(activity, BG);
        box.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));
        box.addView(text(activity, category.toUpperCase(Locale.US) + " // " + humanize(id), GREEN, 12, true), new LinearLayout.LayoutParams(-1, dp(activity, 60)));
        TextView body = text(activity, evidence.toString(), TEXT, 10, true);
        ScrollView scroll = new ScrollView(activity); scroll.addView(body);
        box.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout actions = row(activity, SURFACE);
        if ("network".equals(category)) addAction(activity, actions, "OPEN NETWORK EVIDENCE", CYAN, v -> activity.addContentView(AlfaNetworkPanel.build(activity), new ViewGroup.LayoutParams(-1, -1)));
        if ("settings".equals(category)) addAction(activity, actions, "OPEN SETTINGS", GREEN, v -> openExisting(activity, "Pengaturan terminal dan tampilan"));
        box.addView(actions, new LinearLayout.LayoutParams(-1, dp(activity, 54)));
        new AlertDialog.Builder(activity).setView(box).setPositiveButton("DONE", null).show();
    }

    private static String dispositionFor(String category) {
        if ("network".equals(category)) return "network=RECONCILE_TO_READ_ONLY_ANDROID_LINUX_EVIDENCE\nfirewall_nat_vpn_packet_capture=DO_NOT_CLAIM_WITHOUT_PRIVILEGED_PROOF\n";
        if ("security".equals(category)) return "security=MAP_TO_ACTUAL_MANIFEST_RUNTIME_AND_POLICY_EVIDENCE\nroot_rish_seccomp=ONLY_REPORT_WHAT_CANONICAL_RUNTIME_PROVES\n";
        if ("floating".equals(category)) return "floating=REQUIRE_ACTUAL_SYSTEM_OVERLAY_CAPABILITY_BEFORE_ENABLEMENT\n";
        if ("split".equals(category)) return "split=REQUIRE_INDEPENDENT_SECOND_PTY_SESSION_BEFORE_BROADCAST\n";
        if ("sessions".equals(category)) return "sessions=MAP_TO_RUNTIME_SESSION_MANAGER; DO_NOT_SYNTHESIZE_SESSION_COUNT\n";
        if ("storage-policy".equals(category) || "project-storage".equals(category)) return "storage=RECONCILE_REFERENCE_PATHS_TO_CANONICAL_HOST_STORAGE_BRIDGE\n";
        if ("settings".equals(category)) return "settings=IMPLEMENT_AS_NATIVE_PERSISTED_CONFIGURATION; REFERENCE_PACKAGE_NAMES_ARE_NON-CANONICAL\n";
        return "ui=ADAPT_REFERENCE_VISUAL_STATE_TO_CANONICAL_NATIVE_ARCHITECTURE\n";
    }

    private static String liveEvidence(Activity activity, String category) {
        if ("network".equals(category)) return "ConnectivityManager + NetworkCapabilities + LinkProperties + NetworkInterface + bounded /proc snapshots are available in AlfaNetworkPanel.\n";
        if ("runtime".equals(category)) {
            StringBuilder s = new StringBuilder();
            File vault = new File(activity.getFilesDir(), "runtime-vault");
            for (RuntimeProfile p : RuntimeRegistry.all()) s.append(p.id()).append(" runtime_dir=").append(new File(new File(vault, "runtimes"), p.id()).exists()).append('\n');
            return s.toString();
        }
        if ("security".equals(category)) {
            try {
                PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
                StringBuilder s = new StringBuilder("declared_permissions=\n");
                if (info.requestedPermissions != null) for (String p : info.requestedPermissions) s.append("  ").append(p).append('\n');
                return s.toString();
            } catch (Exception e) { return "permission_evidence=ERROR:" + e.getClass().getSimpleName() + '\n'; }
        }
        if ("floating".equals(category)) return "system_overlay_permission_is_not_assumed; verify capability before enabling a true system overlay.\n";
        if ("split".equals(category)) return "canonical RuntimeSessionManager owns one session instance; independent second PTY evidence is required for real dual-pane execution.\n";
        if ("sessions".equals(category)) return "session lifecycle is backed by RuntimeSessionManager and foreground-service keep-alive; UI must reflect actual state.\n";
        return "state=CANONICAL_ADAPTATION_REQUIRED; no reference-only runtime facts are synthesized.\n";
    }

    private static void openExisting(Activity activity, String description) {
        View root = activity.findViewById(android.R.id.content);
        if (root instanceof ViewGroup) { View v = findByDescription((ViewGroup) root, description); if (v != null) v.performClick(); }
    }

    private static void addAction(Activity a, LinearLayout row, String label, int color, View.OnClickListener listener) { Button b = button(a, label, color); b.setOnClickListener(listener); row.addView(b, new LinearLayout.LayoutParams(0, dp(a, 50), 1)); }
    private static View findByDescription(ViewGroup root, String description) {
        for (int i = 0; i < root.getChildCount(); i++) { View child = root.getChildAt(i); if (description.equals(child.getContentDescription())) return child; if (child instanceof ViewGroup) { View found = findByDescription((ViewGroup) child, description); if (found != null) return found; } }
        return null;
    }
    private static String humanize(String id) { String s = id.startsWith("alfa_device_ctrl_") ? id.substring("alfa_device_ctrl_".length()) : id; s = s.replace('_', ' '); return s.length() > 76 ? s.substring(0, 76) + "…" : s; }
    private static LinearLayout row(Context c, int color) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); l.setBackgroundColor(color); return l; }
    private static LinearLayout column(Context c, int color) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); l.setBackgroundColor(color); return l; }
    private static TextView text(Context c, String value, int color, int size, boolean mono) { TextView t = new TextView(c); t.setText(value); t.setTextColor(color); t.setTextSize(size); t.setGravity(Gravity.CENTER_VERTICAL); if (mono) t.setTypeface(Typeface.MONOSPACE); return t; }
    private static Button button(Context c, String value, int color) { Button b = new Button(c); b.setText(value); b.setTextColor(color); b.setTextSize(9); b.setAllCaps(false); b.setMinHeight(dp(c, 48)); b.setMinWidth(dp(c, 48)); b.setPadding(dp(c, 5), 0, dp(c, 5), 0); b.setBackgroundColor(SURFACE_2); return b; }
    private static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
}
