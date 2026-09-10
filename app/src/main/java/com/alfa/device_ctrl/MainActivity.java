package com.alfa.device_ctrl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Native Alfa UI: preserves the terminal shell while exposing the canonical multi-runtime model. */
public final class MainActivity extends Activity implements RuntimeSessionManager.Listener {
    private static final int BG = Color.rgb(19, 19, 21);
    private static final int PANEL = Color.rgb(32, 31, 33);
    private static final int PANEL_HIGH = Color.rgb(42, 42, 44);
    private static final int PRIMARY = Color.rgb(171, 199, 255);
    private static final int MUTED = Color.rgb(193, 198, 213);
    private static final int ERROR = Color.rgb(255, 180, 171);
    private static final String PROOT_SHA256 = "c902f35b3bce4013d2e78e3bf360b606523d55ab7b907578938577b243bfca38";
    private static final String RUNTIME_PREF = "runtime_id";
    private static final int REQUEST_GATE6_IMPORT = 702;

    private TerminalView terminalView;
    private RuntimeSessionManager sessionManager;
    private RuntimeProfile selectedRuntime;
    private TextView status;
    private TextView terminalTitle;
    private TextView activeTab;
    private TextView monitorText;
    private TextView telemetryText;
    private TextView runtimeSummary;
    private LinearLayout runtimeDashboard;
    private Button killProcessButton;
    private boolean processEvidence;
    private boolean ctrlLatch;
    private volatile boolean uiActive;
    private String installingRuntimeId;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        selectedRuntime = RuntimeSelection.profile(getPreferences(MODE_PRIVATE).getString(RUNTIME_PREF, RuntimeSelection.DEFAULT_RUNTIME_ID));
        if (selectedRuntime == null) selectedRuntime = RuntimeSelection.profile(RuntimeSelection.DEFAULT_RUNTIME_ID);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.rgb(14, 14, 16));
        setContentView(buildShellUi());
        refreshRuntimeDashboard();
    }

    @Override protected void onStart() { super.onStart(); uiActive = true; refreshRuntimeDashboard(); }

    @Override protected void onStop() {
        uiActive = false;
        if (sessionManager != null && sessionManager.isRunning()) sessionManager.stop();
        super.onStop();
    }

    @Override protected void onDestroy() {
        uiActive = false;
        if (sessionManager != null && sessionManager.isRunning()) sessionManager.stop();
        super.onDestroy();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GATE6_IMPORT) {
            if (resultCode == RESULT_OK) startSession();
            else blocked("Gate 6 → Gate 7", "runtime-bound bootstrap belum diimpor");
        }
    }

    private void postUi(Runnable action) {
        if (!uiActive || isFinishing() || isDestroyed()) return;
        runOnUiThread(() -> {
            if (uiActive && !isFinishing() && !isDestroyed()) action.run();
        });
    }

    private View buildShellUi() {
        LinearLayout root = column(BG);
        root.addView(topBar(), new LinearLayout.LayoutParams(-1, dp(52)));
        root.addView(tabBar(), new LinearLayout.LayoutParams(-1, dp(52)));
        root.addView(resourceAlertCard(), new LinearLayout.LayoutParams(-1, -2));

        LinearLayout content = column(BG);
        content.setPadding(dp(12), dp(10), dp(12), dp(8));
        content.addView(runtimeManagementWindow(), new LinearLayout.LayoutParams(-1, dp(248)));
        content.addView(space(8));
        content.addView(monitorWindow(), new LinearLayout.LayoutParams(-1, dp(158)));
        content.addView(space(8));
        LinearLayout.LayoutParams terminalParams = new LinearLayout.LayoutParams(-1, 0, 1);
        terminalParams.setMargins(0, 0, 0, dp(8));
        content.addView(terminalWindow(), terminalParams);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(shortcutBar());
        root.addView(dockBar());
        return root;
    }

    private View topBar() {
        LinearLayout bar = row(PANEL_HIGH);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), 0, dp(12), 0);
        TextView brand = label(getString(R.string.alfa_os), PRIMARY, 16, true);
        brand.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        bar.addView(brand, new LinearLayout.LayoutParams(0, dp(42), 1));
        telemetryText = label("RUNTIME: UNKNOWN  |  READY: 0/0", MUTED, 10, false);
        telemetryText.setTypeface(Typeface.MONOSPACE);
        bar.addView(telemetryText, new LinearLayout.LayoutParams(-2, dp(42)));
        return bar;
    }

    private View tabBar() {
        LinearLayout bar = row(PANEL);
        bar.setPadding(dp(8), 0, dp(8), 0);
        activeTab = label("runtime", PRIMARY, 13, true);
        addTab(bar, activeTab, "runtime", v -> showRuntimeDashboard());
        addTab(bar, label("terminal", MUTED, 13, false), "terminal", v -> showTerminalState());
        addTab(bar, label("htop", MUTED, 13, false), "htop", v -> showHtop());
        addTab(bar, label("jaringan", MUTED, 13, false), "jaringan", v -> showNetwork());
        Button plus = actionButton("+", PRIMARY, v -> createSession());
        plus.setContentDescription("Buat sesi runtime baru");
        bar.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return bar;
    }

    private View resourceAlertCard() {
        final LinearLayout card = row(Color.rgb(53, 52, 55));
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(7), dp(10), dp(7));
        TextView message = label(getString(R.string.resource_alert_title) + "\n" + ResourceAlertState.unknown().displayMessage(), ERROR, 11, true);
        card.addView(message, new LinearLayout.LayoutParams(0, -2, 1));
        Button dismiss = actionButton(getString(R.string.dismiss), MUTED, v -> card.setVisibility(View.GONE));
        dismiss.setContentDescription("Tutup peringatan sumber daya");
        card.addView(dismiss, new LinearLayout.LayoutParams(dp(72), dp(48)));
        killProcessButton = actionButton(getString(R.string.kill_process), ERROR, v -> showProcessPicker());
        killProcessButton.setContentDescription("Hentikan proses runtime terpilih");
        killProcessButton.setEnabled(false);
        killProcessButton.setAlpha(.45f);
        card.addView(killProcessButton, new LinearLayout.LayoutParams(dp(126), dp(48)));
        return card;
    }

    private View runtimeManagementWindow() {
        LinearLayout window = column(Color.BLACK);
        window.setBackground(panelBackground(PANEL, dp(12)));
        LinearLayout header = row(PANEL_HIGH);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), 0, dp(8), 0);
        TextView title = label("LINUX RUNTIMES", PRIMARY, 12, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(38), 1));
        runtimeSummary = label("state dari runtime evidence", MUTED, 9, false);
        runtimeSummary.setTypeface(Typeface.MONOSPACE);
        header.addView(runtimeSummary, new LinearLayout.LayoutParams(-2, dp(38)));
        window.addView(header);
        ScrollView runtimeScroll = new ScrollView(this);
        runtimeScroll.setFillViewport(true);
        runtimeScroll.setClipToPadding(false);
        runtimeScroll.setVerticalScrollBarEnabled(true);
        runtimeDashboard = column(Color.BLACK);
        runtimeDashboard.setPadding(dp(8), dp(5), dp(8), dp(5));
        runtimeScroll.addView(runtimeDashboard, new ScrollView.LayoutParams(-1, -2));
        window.addView(runtimeScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return window;
    }

    private void refreshRuntimeDashboard() {
        if (runtimeDashboard == null) return;
        runtimeDashboard.removeAllViews();
        List<RuntimeProfile> profiles = RuntimeRegistry.all();
        int readyCount = 0;
        for (RuntimeProfile profile : profiles) {
            RuntimeUiState.Status state = runtimeStatus(profile);
            if (state == RuntimeUiState.Status.READY) readyCount++;
            runtimeDashboard.addView(runtimeCard(profile, state), new LinearLayout.LayoutParams(-1, dp(54)));
            if (profile.id().equals(profiles.get(profiles.size() - 1).id())) continue;
            runtimeDashboard.addView(space(3));
        }
        if (runtimeSummary != null) runtimeSummary.setText("READY=" + readyCount + "/" + profiles.size());
        if (telemetryText != null) telemetryText.setText("RUNTIME: " + selectedRuntime.displayName() + "  |  READY: " + readyCount + "/" + profiles.size());
    }

    private RuntimeUiState.Status runtimeStatus(RuntimeProfile profile) {
        File vault = new File(getFilesDir(), "runtime-vault");
        File runtime = new File(new File(vault, "runtimes"), profile.id());
        File ready = new File(runtime, "READY.evidence");
        File rootfs = new File(runtime, "rootfs");
        File engine = new File(getApplicationInfo().nativeLibraryDir, "libproot.so");
        if (profile.id().equals(installingRuntimeId)) return RuntimeUiState.Status.VERIFYING;
        return RuntimeUiState.resolve(profile.id(), runtime, ready, engine, rootfs);
    }

    private View runtimeCard(RuntimeProfile profile, RuntimeUiState.Status state) {
        LinearLayout card = row(PANEL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(9), 0, dp(5), 0);
        TextView name = label(profile.displayName() + "\n" + profile.id(), PRIMARY, 10, true);
        name.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        card.addView(name, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView stateText = label(RuntimeUiState.label(state), state == RuntimeUiState.Status.READY ? PRIMARY : MUTED, 9, true);
        stateText.setGravity(Gravity.CENTER);
        stateText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        card.addView(stateText, new LinearLayout.LayoutParams(dp(86), dp(48)));
        Button verify = actionButton("VERIFY", MUTED, v -> { refreshRuntimeDashboard(); showStatus("RUNTIME_VERIFY=" + profile.id() + "\nSTATE=" + RuntimeUiState.label(runtimeStatus(profile))); });
        verify.setContentDescription("Verifikasi runtime " + profile.displayName());
        card.addView(verify, new LinearLayout.LayoutParams(dp(70), dp(46)));
        if (RuntimeUiState.canOpen(state)) {
            Button open = actionButton("OPEN", PRIMARY, v -> { selectRuntime(profile.id()); startSession(); });
            open.setContentDescription("Buka runtime " + profile.displayName());
            card.addView(open, new LinearLayout.LayoutParams(dp(64), dp(46)));
        } else {
            Button install = actionButton("INSTALL", PRIMARY, v -> { selectRuntime(profile.id()); installRuntime(); });
            install.setContentDescription("Instal runtime " + profile.displayName());
            install.setEnabled(state != RuntimeUiState.Status.VERIFYING);
            card.addView(install, new LinearLayout.LayoutParams(dp(68), dp(46)));
        }
        if (profile.id().equals(selectedRuntime.id())) card.setBackground(panelBackground(Color.rgb(39, 47, 62), dp(7)));
        return card;
    }

    private View monitorWindow() {
        LinearLayout window = column(Color.BLACK);
        window.setBackground(panelBackground(PANEL, dp(12)));
        LinearLayout header = row(PANEL_HIGH);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), 0, dp(8), 0);
        TextView title = label("htop / telemetry", MUTED, 11, false);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(34), 1));
        TextView state = label("EVIDENCE", MUTED, 9, false);
        state.setTypeface(Typeface.MONOSPACE);
        header.addView(state, new LinearLayout.LayoutParams(-2, dp(34)));
        window.addView(header);
        monitorText = label("Runtime telemetry menunggu sesi " + selectedRuntime.displayName() + " terverifikasi.\nCPU: UNKNOWN    MEM: UNKNOWN    SWAP: UNKNOWN\nTidak ada angka mockup yang ditampilkan.", MUTED, 11, false);
        monitorText.setTypeface(Typeface.MONOSPACE);
        monitorText.setPadding(dp(12), dp(9), dp(12), dp(9));
        window.addView(monitorText, new LinearLayout.LayoutParams(-1, 0, 1));
        return window;
    }

    private View terminalWindow() {
        LinearLayout window = column(Color.BLACK);
        window.setBackground(panelBackground(PANEL, dp(14)));
        LinearLayout header = row(PANEL_HIGH);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), 0, dp(10), 0);
        terminalTitle = label("runtime: " + selectedRuntime.displayName(), PRIMARY, 12, true);
        terminalTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        header.addView(terminalTitle, new LinearLayout.LayoutParams(0, dp(40), 1));
        Button close = actionButton("×", ERROR, v -> stopSession());
        close.setContentDescription("Hentikan sesi runtime");
        header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        window.addView(header);
        FrameLayout terminalFrame = new FrameLayout(this);
        terminalView = new TerminalView(this, null);
        terminalView.setTerminalViewClient(new AlfaTerminalViewClient());
        terminalFrame.addView(terminalView, new FrameLayout.LayoutParams(-1, -1));
        status = label(getString(R.string.session_blocked) + "\n" + getString(R.string.session_blocked_reason), MUTED, 12, false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(18), dp(18), dp(18), dp(18));
        status.setBackgroundColor(Color.argb(210, 19, 19, 21));
        terminalFrame.addView(status, new FrameLayout.LayoutParams(-1, -1));
        window.addView(terminalFrame, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout actions = row(Color.BLACK);
        actions.setPadding(dp(6), dp(4), dp(6), dp(4));
        Button install = actionButton("INSTALL " + selectedRuntime.displayName().toUpperCase(), PRIMARY, v -> installRuntime());
        Button start = actionButton(getString(R.string.start_verified_session), PRIMARY, v -> startSession());
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, dp(48), 1);
        ip.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(48), 1);
        sp.setMargins(dp(4), 0, 0, 0);
        actions.addView(install, ip);
        actions.addView(start, sp);
        window.addView(actions, new LinearLayout.LayoutParams(-1, dp(56)));
        return window;
    }

    private View shortcutBar() {
        LinearLayout bar = row(PANEL_HIGH);
        bar.setPadding(dp(8), dp(5), dp(8), dp(5));
        addShortcut(bar, "ESC", new byte[]{27});
        Button ctrl = actionButton("CTRL", MUTED, v -> { ctrlLatch = !ctrlLatch; v.setAlpha(ctrlLatch ? 1f : .65f); });
        ctrl.setContentDescription("Aktifkan atau nonaktifkan CTRL");
        bar.addView(ctrl, new LinearLayout.LayoutParams(0, dp(48), 1));
        addShortcut(bar, "ALT", new byte[]{27});
        addShortcut(bar, "TAB", new byte[]{9});
        addShortcut(bar, "←", new byte[]{27, '[', 'D'});
        addShortcut(bar, "↓", new byte[]{27, '[', 'B'});
        addShortcut(bar, "↑", new byte[]{27, '[', 'A'});
        addShortcut(bar, "→", new byte[]{27, '[', 'C'});
        return bar;
    }

    private View dockBar() {
        LinearLayout dock = row(PANEL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8), dp(4), dp(8), dp(4));
        return dock;
    }

    // ... remainder of canonical MainActivity implementation unchanged ...
}
