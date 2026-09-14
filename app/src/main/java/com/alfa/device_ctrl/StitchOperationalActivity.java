package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.UUID;

/**
 * Real Stitch v1 operational presentation. The view is driven by the existing runtime/session
 * contracts; it is not a screenshot or a mock terminal surface.
 */
public final class StitchOperationalActivity extends Activity implements RuntimeSessionManager.Listener {
    private static final int BG = Color.rgb(16, 20, 25);
    private static final int SURFACE_LOW = Color.rgb(24, 28, 33);
    private static final int SURFACE = Color.rgb(28, 32, 37);
    private static final int SURFACE_HIGH = Color.rgb(38, 42, 48);
    private static final int SURFACE_HIGHEST = Color.rgb(49, 53, 59);
    private static final int TEXT = Color.rgb(224, 226, 234);
    private static final int MUTED = Color.rgb(187, 202, 191);
    private static final int PRIMARY = Color.rgb(78, 222, 163);
    private static final int SECONDARY = Color.rgb(76, 215, 246);
    private static final int ERROR = Color.rgb(255, 180, 171);
    private static final String PROOT_SHA256 = "c902f35b3bce4013d2e78e3bf360b606523d55ab7b907578938577b243bfca38";
    private static final int REQUEST_GATE6_IMPORT = 702;

    private TerminalView terminalView;
    private RuntimeSessionManager sessionManager;
    private RuntimeProfile selectedRuntime;
    private TextView systemState;
    private TextView telemetryCpu;
    private TextView telemetryMem;
    private TextView telemetryStorage;
    private TextView telemetryNet;
    private TextView sessionState;
    private TextView terminalTitle;
    private TextView terminalOutput;
    private TextView statusBanner;
    private EditText commandInput;
    private LinearLayout runtimeList;
    private boolean uiActive;
    private String installingRuntimeId;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        selectedRuntime = RuntimeSelection.profile(getPreferences(MODE_PRIVATE).getString("runtime_id", RuntimeSelection.DEFAULT_RUNTIME_ID));
        if (selectedRuntime == null) selectedRuntime = RuntimeSelection.profile(RuntimeSelection.DEFAULT_RUNTIME_ID);
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(Color.rgb(10, 14, 19));
        setContentView(buildUi());
        uiActive = true;
        refreshRuntimes();
    }

    @Override protected void onStart() { super.onStart(); uiActive = true; if (sessionManager != null) sessionManager.rebindListener(this); refreshRuntimes(); }

    @Override protected void onStop() { uiActive = false; if (sessionManager != null) sessionManager.stop(); super.onStop(); }

    @Override protected void onDestroy() { uiActive = false; if (sessionManager != null) sessionManager.stop(); super.onDestroy(); }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GATE6_IMPORT && resultCode == RESULT_OK) startSession();
    }

    private View buildUi() {
        LinearLayout root = column(BG);
        root.addView(header(), new LinearLayout.LayoutParams(-1, dp(96)));
        root.addView(navigation(), new LinearLayout.LayoutParams(-1, dp(48)));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = column(BG);
        content.setPadding(dp(12), dp(10), dp(12), dp(16));
        content.addView(telemetryStrip());
        content.addView(space(10));
        content.addView(terminalSurface(), new LinearLayout.LayoutParams(-1, dp(420)));
        content.addView(space(10));
        content.addView(runtimeSurface());
        content.addView(space(10));
        content.addView(statusPanel());
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(shortcuts(), new LinearLayout.LayoutParams(-1, dp(58)));
        return root;
    }

    private View header() {
        LinearLayout header = column(BG);
        header.setPadding(dp(16), dp(8), dp(12), 0);
        LinearLayout line = row(BG);
        line.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout title = column(BG);
        TextView brand = text("ALFA DEVICE CTRL", PRIMARY, 12, true);
        TextView subtitle = text("Linux Runtime Control", MUTED, 12, false);
        title.addView(brand); title.addView(subtitle);
        line.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));
        LinearLayout online = row(SURFACE_HIGH);
        online.setGravity(Gravity.CENTER_VERTICAL);
        online.setPadding(dp(10), 0, dp(10), 0);
        TextView dot = text("●", PRIMARY, 10, true);
        online.addView(dot);
        systemState = text("SYSTEM ONLINE", PRIMARY, 10, true);
        systemState.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        online.addView(systemState);
        line.addView(online, new LinearLayout.LayoutParams(-2, dp(34)));
        line.addView(action("⚙", MUTED, v -> showStatus("SETTINGS\nStitch presentation v1\nRuntime state is sourced from RuntimeSessionManager.")), new LinearLayout.LayoutParams(dp(48), dp(44)));
        header.addView(line);
        return header;
    }

    private View navigation() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout nav = row(BG);
        nav.setPadding(dp(8), 0, dp(8), 0);
        addNav(nav, "TERMINAL", true, v -> scrollToTerminal());
        addNav(nav, "RUNTIMES", false, v -> showStatus("RUNTIMES\nSelect a verified runtime below."));
        addNav(nav, "MONITOR", false, v -> refreshTelemetry());
        addNav(nav, "SECURITY", false, v -> showStatus("SECURITY\nRuntime is rootless Android-side; PRoot is not a host security sandbox."));
        addNav(nav, "SETTINGS", false, v -> showStatus("SETTINGS\nStitch operational presentation\nNo network dependency for rendering."));
        hsv.addView(nav);
        return hsv;
    }

    private View telemetryStrip() {
        LinearLayout card = column(SURFACE_LOW);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout head = row(SURFACE_LOW);
        TextView label = text("TELEMETRY", PRIMARY, 10, true);
        head.addView(label, new LinearLayout.LayoutParams(0, dp(24), 1));
        head.addView(text("HOST: aarch64", MUTED, 10, false));
        card.addView(head);
        LinearLayout metrics = row(SURFACE_LOW);
        telemetryCpu = metric(metrics, "CPU", "--", PRIMARY);
        telemetryMem = metric(metrics, "MEM", "--", SECONDARY);
        telemetryStorage = metric(metrics, "STORAGE", "--", Color.rgb(255, 185, 95));
        telemetryNet = metric(metrics, "NET", "OFFLINE", PRIMARY);
        card.addView(metrics, new LinearLayout.LayoutParams(-1, dp(62)));
        return card;
    }

    private TextView metric(LinearLayout parent, String name, String value, int valueColor) {
        LinearLayout box = column(SURFACE);
        box.setPadding(dp(7), dp(5), dp(7), dp(4));
        TextView n = text(name, MUTED, 9, true);
        TextView v = text(value, valueColor, 11, true);
        v.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        box.addView(n); box.addView(v);
        parent.addView(box, new LinearLayout.LayoutParams(0, dp(56), 1));
        if (parent.getChildCount() < 4) parent.addView(spaceH(3), new LinearLayout.LayoutParams(dp(3), dp(1)));
        return v;
    }

    private View terminalSurface() {
        LinearLayout panel = column(Color.rgb(10, 14, 19));
        panel.setBackground(round(Color.rgb(10, 14, 19), dp(10), Color.rgb(60, 72, 67)));
        LinearLayout bar = row(SURFACE_HIGH);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), 0, dp(6), 0);
        terminalTitle = text("TTY1  //  " + selectedRuntime.displayName(), PRIMARY, 10, true);
        bar.addView(terminalTitle, new LinearLayout.LayoutParams(0, dp(44), 1));
        sessionState = text("SESSION OFFLINE", MUTED, 9, true);
        bar.addView(sessionState, new LinearLayout.LayoutParams(-2, dp(44)));
        bar.addView(action("×", ERROR, v -> stopSession()), new LinearLayout.LayoutParams(dp(44), dp(44)));
        panel.addView(bar);

        FrameLayout terminalFrame = new FrameLayout(this);
        terminalView = new TerminalView(this, null);
        terminalFrame.addView(terminalView, new FrameLayout.LayoutParams(-1, -1));
        terminalOutput = text("Waiting for a verified runtime session…", MUTED, 11, false);
        terminalOutput.setTypeface(Typeface.MONOSPACE);
        terminalOutput.setPadding(dp(12), dp(12), dp(12), dp(12));
        terminalFrame.addView(terminalOutput, new FrameLayout.LayoutParams(-1, -1));
        panel.addView(terminalFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout command = row(SURFACE);
        command.setPadding(dp(8), dp(5), dp(8), dp(5));
        commandInput = new EditText(this);
        commandInput.setSingleLine(true);
        commandInput.setHint("inject guest command or script …");
        commandInput.setHintTextColor(MUTED);
        commandInput.setTextColor(TEXT);
        commandInput.setTextSize(11);
        commandInput.setTypeface(Typeface.MONOSPACE);
        commandInput.setBackground(round(SURFACE_LOW, dp(5), Color.rgb(60, 72, 67)));
        command.addView(commandInput, new LinearLayout.LayoutParams(0, dp(46), 1));
        command.addView(action("RUN ▶", PRIMARY, v -> runCommand()), new LinearLayout.LayoutParams(dp(82), dp(46)));
        panel.addView(command);
        return panel;
    }

    private View runtimeSurface() {
        LinearLayout panel = column(SURFACE_LOW);
        panel.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout head = row(SURFACE_LOW);
        head.addView(text("RUNTIMES", PRIMARY, 11, true), new LinearLayout.LayoutParams(0, dp(32), 1));
        head.addView(action("+", PRIMARY, v -> selectRuntime()), new LinearLayout.LayoutParams(dp(44), dp(40)));
        panel.addView(head);
        runtimeList = column(SURFACE_LOW);
        panel.addView(runtimeList);
        return panel;
    }

    private View statusPanel() {
        statusBanner = text("STATE\nPresentation is connected to runtime/session contracts.", MUTED, 10, false);
        statusBanner.setTypeface(Typeface.MONOSPACE);
        statusBanner.setPadding(dp(12), dp(10), dp(12), dp(10));
        statusBanner.setBackground(round(SURFACE, dp(8), Color.rgb(60, 72, 67)));
        return statusBanner;
    }

    private View shortcuts() {
        LinearLayout bar = row(BG);
        bar.setPadding(dp(8), dp(5), dp(8), dp(5));
        addShortcut(bar, "ESC", new byte[]{27});
        addShortcut(bar, "TAB", new byte[]{9});
        addShortcut(bar, "CTRL", null);
        addShortcut(bar, "↑", new byte[]{27, '[', 'A'});
        addShortcut(bar, "↓", new byte[]{27, '[', 'B'});
        addShortcut(bar, "COPY", null);
        return bar;
    }

    private void addNav(LinearLayout nav, String label, boolean active, View.OnClickListener listener) {
        Button b = action(label, active ? PRIMARY : MUTED, listener);
        b.setAllCaps(true);
        nav.addView(b, new LinearLayout.LayoutParams(dp(112), dp(44)));
    }

    private void addShortcut(LinearLayout parent, String label, byte[] bytes) {
        Button b = action(label, MUTED, v -> { if (bytes != null) send(bytes); else showStatus("SHORTCUT=" + label); });
        parent.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
    }

    private void refreshRuntimes() {
        if (runtimeList == null) return;
        runtimeList.removeAllViews();
        List<RuntimeProfile> profiles = RuntimeRegistry.all();
        for (RuntimeProfile profile : profiles) {
            File vault = new File(getFilesDir(), "runtime-vault");
            File runtime = new File(new File(vault, "runtimes"), profile.id());
            RuntimeUiState.Status state = RuntimeUiState.resolve(profile.id(), runtime, new File(runtime, "READY.evidence"), new File(getApplicationInfo().nativeLibraryDir, "libproot.so"), new File(runtime, "rootfs"));
            if (profile.id().equals(installingRuntimeId)) state = RuntimeUiState.Status.VERIFYING;
            LinearLayout card = row(SURFACE);
            card.setPadding(dp(10), dp(3), dp(4), dp(3));
            TextView name = text(profile.displayName() + "\n" + profile.id(), TEXT, 10, true);
            card.addView(name, new LinearLayout.LayoutParams(0, dp(52), 1));
            card.addView(text(RuntimeUiState.label(state), state == RuntimeUiState.Status.READY ? PRIMARY : MUTED, 9, true), new LinearLayout.LayoutParams(dp(74), dp(52)));
            Button verify = action("VERIFY", MUTED, v -> refreshRuntimes());
            card.addView(verify, new LinearLayout.LayoutParams(dp(66), dp(46)));
            Button open = action(state == RuntimeUiState.Status.READY ? "OPEN" : "INSTALL", PRIMARY, v -> { selectedRuntime = profile; if (state == RuntimeUiState.Status.READY) startSession(); else installRuntime(); });
            card.addView(open, new LinearLayout.LayoutParams(dp(70), dp(46)));
            if (profile.id().equals(selectedRuntime.id())) card.setBackground(round(Color.rgb(31, 48, 43), dp(7), Color.rgb(78, 120, 103)));
            runtimeList.addView(card);
            runtimeList.addView(space(4));
        }
    }

    private void refreshTelemetry() {
        if (!sessionReady()) { telemetryCpu.setText("--"); telemetryMem.setText("--"); telemetryStorage.setText("--"); telemetryNet.setText("OFFLINE"); showStatus("MONITOR\nWAITING\nStart a verified runtime session first."); return; }
        telemetryNet.setText("LIVE");
        sessionManager.runRuntimeCommand("mt=$(awk '/MemTotal:/{print $2}' /proc/meminfo); ma=$(awk '/MemAvailable:/{print $2}' /proc/meminfo); used=$((mt-ma)); mp=0; [ $mt -gt 0 ] && mp=$((used*100/mt)); df=$(df -P / | awk 'NR==2{gsub(/%/,\"\",$5);print $5}'); printf 'MEM=%s%%\\nSTORAGE=%s%%\\n' $mp ${df:-0}", (output, code) -> postUi(() -> {
            if (code != 0) { showStatus("MONITOR\nBLOCKED\n" + output); return; }
            for (String line : output.split("\\n")) { if (line.startsWith("MEM=")) telemetryMem.setText(line.substring(4)); if (line.startsWith("STORAGE=")) telemetryStorage.setText(line.substring(9)); }
            telemetryCpu.setText("LIVE");
        }));
    }

    private void installRuntime() {
        final RuntimeProfile profile = selectedRuntime;
        if (!uiActive || (sessionManager != null && sessionManager.isRunning())) { showStatus("INSTALL\nBLOCKED\nStop the active session first."); return; }
        installingRuntimeId = profile.id(); refreshRuntimes();
        showStatus("INSTALL\n" + profile.displayName() + "\nVerifying checksum and extracting atomically…");
        new Thread(() -> {
            RuntimeInstaller.Result result;
            try {
                File vault = new File(getFilesDir(), "runtime-vault");
                File nativeDir = new File(getApplicationInfo().nativeLibraryDir);
                result = new RuntimeInstaller(vault, message -> postUi(() -> showStatus("INSTALL\n" + message)), new File(nativeDir, "libproot.so"), nativeDir)
                        .install(profile, null, PROOT_SHA256, new URL(profile.rootfsUrl()), profile.rootfsSha256(), profile.rootfsGzip());
            } catch (Exception e) { result = RuntimeInstaller.Result.fail(profile.id(), e.getClass().getSimpleName() + ":" + e.getMessage(), null); }
            RuntimeInstaller.Result done = result;
            postUi(() -> { installingRuntimeId = null; refreshRuntimes(); showStatus(done.success ? "INSTALL\nREADY\n" + done.runtimeId : "INSTALL\nFAILED\n" + done.message); });
        }, "stitch-runtime-installer").start();
    }

    private void startSession() {
        if (!uiActive) return;
        if (sessionManager != null && sessionManager.isRunning()) { sessionManager.attachTo(terminalView); return; }
        RuntimeProfile profile = selectedRuntime;
        File runtime = new File(new File(getFilesDir(), "runtime-vault"), "runtimes/" + profile.id());
        File ready = new File(runtime, "READY.evidence");
        File launch = new File(new File(getFilesDir(), "runtime-vault"), "gate7-launch.properties");
        if (!Gate6LaunchContract.verify(launch, profile.id())) {
            showStatus("GATE 6 → GATE 7\nImport current runtime bootstrap for " + profile.displayName() + ".");
            Intent intent = new Intent(this, Gate6ImportActivity.class);
            intent.putExtra("runtime_id", profile.id());
            startActivityForResult(intent, REQUEST_GATE6_IMPORT);
            return;
        }
        File cwd = new File(getFilesDir(), "session-cwd");
        if (!ready.isFile() || (!cwd.exists() && !cwd.mkdirs())) { showStatus("SESSION\nBLOCKED\nruntime-ready.v1 or session cwd is unavailable."); return; }
        try {
            InteractiveSessionContract contract = new InteractiveSessionContract("session-" + shortId(), "request-" + shortId(), "run-" + shortId(), profile.id(), ready,
                    new File(getApplicationInfo().nativeLibraryDir, "libproot.so"), new File(runtime, "rootfs"), cwd,
                    new String[]{"HOME=/root", "TERM=xterm-256color", "PS1=alfa:" + profile.id() + ":\\w\\$ ", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "PROOT_TMP_DIR=" + new File(runtime, "proot_tmp").getAbsolutePath()});
            sessionManager = new RuntimeSessionManager(contract, this);
            if (!sessionManager.start(80, 24, 8, 16)) { showStatus("SESSION\nBLOCKED\nRuntime authorization failed."); return; }
            sessionManager.attachTo(terminalView);
            terminalOutput.setVisibility(View.GONE);
            sessionState.setText("SESSION READY");
            terminalTitle.setText("TTY1  //  " + profile.displayName());
            refreshTelemetry();
        } catch (RuntimeException e) { showStatus("SESSION\nBLOCKED\n" + e.getClass().getSimpleName() + ":" + e.getMessage()); }
    }

    private void stopSession() { if (sessionManager != null && sessionManager.isRunning()) sessionManager.stop(); else showStatus("SESSION\nNo active session."); }

    private void restartSession() { stopSession(); postUiDelayed(this::startSession, 300); }

    private void runCommand() {
        String command = commandInput == null ? "" : commandInput.getText().toString().trim();
        if (command.isEmpty()) return;
        if (!sessionReady()) { showStatus("COMMAND\nBLOCKED\nVerified runtime session required."); return; }
        sessionManager.runRuntimeCommand(command, (output, code) -> postUi(() -> showStatus("COMMAND\nEXIT=" + code + "\n" + output)));
    }

    private boolean sessionReady() { return sessionManager != null && sessionManager.isRunning() && sessionManager.isPromptReady(); }

    private void send(byte[] bytes) {
        TerminalSession session = sessionManager == null ? null : sessionManager.currentSession();
        if (session == null || !session.isRunning()) { showStatus("INPUT\nBLOCKED\nNo active PTY."); return; }
        session.write(bytes, 0, bytes.length);
    }

    private void selectRuntime() {
        if (sessionManager != null && sessionManager.isRunning()) { showStatus("RUNTIMES\nStop the active session before switching runtime."); return; }
        List<RuntimeProfile> profiles = RuntimeRegistry.all();
        String[] names = new String[profiles.size()];
        int selected = 0;
        for (int i = 0; i < profiles.size(); i++) { names[i] = profiles.get(i).displayName(); if (profiles.get(i).id().equals(selectedRuntime.id())) selected = i; }
        new android.app.AlertDialog.Builder(this).setTitle("Select runtime").setSingleChoiceItems(names, selected, (dialog, which) -> { selectedRuntime = profiles.get(which); getPreferences(MODE_PRIVATE).edit().putString("runtime_id", selectedRuntime.id()).apply(); dialog.dismiss(); refreshRuntimes(); terminalTitle.setText("TTY1  //  " + selectedRuntime.displayName()); }).setNegativeButton("Cancel", null).show();
    }

    private void scrollToTerminal() { }

    private void showStatus(String text) { if (statusBanner != null) { statusBanner.setVisibility(View.VISIBLE); statusBanner.setText(text); } }

    private void postUi(Runnable r) { if (uiActive && !isFinishing() && !isDestroyed()) runOnUiThread(r); }
    private void postUiDelayed(Runnable r, long delay) { if (uiActive) runOnUiThreadDelayed(r, delay); }
    private String shortId() { return UUID.randomUUID().toString().replace("-", "").substring(0, 12); }

    private Button action(String value, int color, View.OnClickListener listener) { Button b = new Button(this); b.setText(value); b.setTextColor(color); b.setTextSize(10); b.setAllCaps(false); b.setMinHeight(0); b.setMinWidth(0); b.setPadding(dp(5), 0, dp(5), 0); b.setGravity(Gravity.CENTER); b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); b.setBackground(round(SURFACE_HIGH, dp(6), Color.rgb(60, 72, 67))); b.setStateListAnimator(null); b.setOnClickListener(listener); return b; }
    private TextView text(String value, int color, int size, boolean bold) { TextView t = new TextView(this); t.setText(value); t.setTextColor(color); t.setTextSize(size); t.setGravity(Gravity.CENTER_VERTICAL); if (bold) t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); return t; }
    private LinearLayout row(int color) { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setBackgroundColor(color); return l; }
    private LinearLayout column(int color) { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setBackgroundColor(color); return l; }
    private GradientDrawable round(int color, int radius, int strokeColor) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); d.setStroke(dp(1), strokeColor); return d; }
    private View space(int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(h))); return v; }
    private View spaceH(int w) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(dp(w), 1)); return v; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onState(String state) { postUi(() -> { sessionState.setText("SESSION " + state); if ("READY".equals(state) || "RUNNING".equals(state)) { terminalOutput.setVisibility(View.GONE); systemState.setText("SYSTEM ONLINE"); } else terminalOutput.setVisibility(View.VISIBLE); refreshRuntimes(); }); }
    @Override public void onTextChanged() { postUi(() -> { if (terminalView != null) terminalView.invalidate(); }); }
    @Override public void onSessionFinished(int exitStatus) { postUi(() -> { sessionState.setText("SESSION FINISHED"); terminalOutput.setVisibility(View.VISIBLE); terminalOutput.setText("exit_status=" + exitStatus); refreshRuntimes(); }); }
}
