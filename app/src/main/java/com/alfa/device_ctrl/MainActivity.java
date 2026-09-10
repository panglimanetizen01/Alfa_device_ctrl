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
        runtimeDashboard = column(Color.BLACK);
        runtimeDashboard.setPadding(dp(8), dp(5), dp(8), dp(5));
        window.addView(runtimeDashboard, new LinearLayout.LayoutParams(-1, 0, 1));
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
            if (profile != profiles.get(profiles.size() - 1)) runtimeDashboard.addView(space(3));
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
        addDock(dock, "⌘", "Buka alat runtime", v -> showRuntimeTools());
        addDock(dock, "▣", "Tampilkan status terminal", v -> showTerminalState());
        addDock(dock, "□", "Tampilkan file runtime", v -> showRuntimeFiles());
        addDock(dock, "⚙", "Tampilkan kebijakan runtime", v -> showPolicy());
        return dock;
    }

    private boolean sessionReady() { return uiActive && sessionManager != null && sessionManager.isRunning() && sessionManager.isPromptReady(); }

    private void createSession() {
        if (sessionReady()) showStatus("SESSION_STATUS=ALREADY_READY\nSesi aktif dipakai oleh terminal utama");
        else if (sessionManager != null && sessionManager.isRunning()) showStatus("SESSION_STATUS=WAITING\nPTY aktif, menunggu prompt runtime terverifikasi");
        else startSession();
    }

    private void showRuntimeDashboard() {
        activeTab.setText("runtime");
        refreshRuntimeDashboard();
        showStatus("RUNTIME_DASHBOARD\nSelected=" + selectedRuntime.displayName() + "\nStatus berasal dari runtime-ready.v1 evidence.");
    }

    private void showHtop() {
        activeTab.setText("htop");
        if (!sessionReady()) { showStatus("TAB=htop\nSTATUS=WAITING\nSesi " + selectedRuntime.displayName() + " terverifikasi diperlukan untuk telemetry live"); return; }
        monitorText.setText("htop\nMengambil CPU, MEM, SWAP, dan proses dari runtime " + selectedRuntime.displayName() + "...");
        String command = "cpu_line=$(head -n 1 /proc/stat); set -- $cpu_line; u1=$2; n1=$3; s1=$4; i1=$5; w1=$6; q1=$7; sq1=$8; st1=$9; t1=$((u1+n1+s1+i1+w1+q1+sq1+st1)); id1=$((i1+w1)); sleep 1; cpu_line=$(head -n 1 /proc/stat); set -- $cpu_line; u2=$2; n2=$3; s2=$4; i2=$5; w2=$6; q2=$7; sq2=$8; st2=$9; t2=$((u2+n2+s2+i2+w2+q2+sq2+st2)); id2=$((i2+w2)); dt=$((t2-t1)); di=$((id2-id1)); used=$((dt-di)); if [ $dt -gt 0 ]; then cpu=$((used*100/dt)); else cpu=0; fi; mt=$(awk '/MemTotal:/{print $2}' /proc/meminfo); ma=$(awk '/MemAvailable:/{print $2}' /proc/meminfo); st=$(awk '/SwapTotal:/{print $2}' /proc/meminfo); sf=$(awk '/SwapFree:/{print $2}' /proc/meminfo); mt=${mt:-0}; ma=${ma:-0}; st=${st:-0}; sf=${sf:-0}; mu=$((mt-ma)); su=$((st-sf)); if [ $mt -gt 0 ]; then mp=$((mu*100/mt)); else mp=0; fi; if [ $st -gt 0 ]; then sp=$((su*100/st)); else sp=0; fi; printf 'TELEMETRY_EVIDENCE=PASS\\nCPU=%s%%\\nMEM=%s/%s kB (%s%%)\\nSWAP=%s/%s kB (%s%%)\\n' $cpu $mu $mt $mp $su $st $sp; printf 'PROCESS_EVIDENCE=PASS\\n'; ps -eo pid=,user=,stat=,pcpu=,pmem=,comm= --sort=-pcpu | head -n 12";
        sessionManager.runRuntimeCommand(command, (output, code) -> postUi(() -> {
            processEvidence = code == 0 && output.contains("PROCESS_EVIDENCE=PASS");
            if (killProcessButton != null) { killProcessButton.setEnabled(processEvidence); killProcessButton.setAlpha(processEvidence ? 1f : .45f); }
            monitorText.setText(code == 0 && output.contains("TELEMETRY_EVIDENCE=PASS") ? output : "htop\nSTATUS=BLOCKED\ntelemetry evidence gagal\n" + output);
        }));
    }

    private void showNetwork() {
        activeTab.setText("jaringan");
        if (!sessionReady()) { showStatus("TAB=jaringan\nSTATUS=WAITING\nSesi " + selectedRuntime.displayName() + " terverifikasi diperlukan untuk network evidence"); return; }
        monitorText.setText("jaringan\nMengambil interface dan route dari runtime " + selectedRuntime.displayName() + "...");
        String command = "iface_count=$(awk 'NR>2 && $1 !~ /^lo:/ {c++} END{print c+0}' /proc/net/dev); route_count=$(awk 'NR>1 && $2==\"00000000\" && $1!=\"lo\" {c++} END{print c+0}' /proc/net/route); if [ \"$iface_count\" -gt 0 ]; then printf 'NETWORK_EVIDENCE=PASS\\nINTERFACES\\n'; awk 'NR>2 {print}' /proc/net/dev; printf '\\nROUTES\\n'; cat /proc/net/route; printf '\\nDEFAULT_ROUTE_COUNT=%s\\n' $route_count; else printf 'NETWORK_EVIDENCE=BLOCKED\\nREASON=no-nonloopback-interface-in-runtime\\n'; fi";
        sessionManager.runRuntimeCommand(command, (output, code) -> postUi(() -> monitorText.setText(code == 0 && output.contains("NETWORK_EVIDENCE=PASS") ? output : "jaringan\nSTATUS=BLOCKED\n" + output)));
    }

    private void showProcessPicker() {
        if (!sessionReady() || !processEvidence) { blocked("hentikan proses", "process evidence live belum tersedia"); return; }
        sessionManager.runRuntimeCommand("ps -eo pid=,comm= --sort=-pcpu | head -n 20", (output, code) -> postUi(() -> {
            if (code != 0 || output.trim().isEmpty()) { processEvidence = false; disableKillButton(); blocked("hentikan proses", "process evidence snapshot gagal"); return; }
            ArrayList<String> pids = new ArrayList<>(), labels = new ArrayList<>();
            for (String line : output.split("\\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                String[] parts = trimmed.split("\\s+", 2);
                if (parts.length == 0 || !parts[0].matches("[0-9]+")) continue;
                pids.add(parts[0]); labels.add(trimmed);
            }
            if (pids.isEmpty()) { blocked("hentikan proses", "process evidence tidak berisi PID valid"); return; }
            new AlertDialog.Builder(this).setTitle("Pilih proses dalam runtime " + selectedRuntime.displayName()).setItems(labels.toArray(new String[0]), (dialog, which) -> killSelectedProcess(pids.get(which))).setNegativeButton("Batal", null).show();
        }));
    }

    private void killSelectedProcess(String pid) {
        if (!pid.matches("[0-9]+") || !sessionReady() || !processEvidence) { blocked("hentikan proses", "capability/process evidence tidak tersedia"); return; }
        String command = "pid=" + pid + "; if [ \"$pid\" -le 1 ] || [ \"$pid\" -eq \"$$\" ]; then printf 'PROCESS_ACTION=BLOCKED\\nREASON=protected-runtime-process\\n'; else if [ -r \"/proc/$pid/stat\" ]; then kill -TERM \"$pid\" 2>/dev/null; rc=$?; printf 'PROCESS_SCOPE=selected-rootless-runtime\\nPROCESS_PID=%s\\nPROCESS_ACTION=%s\\n' \"$pid\" $rc; else printf 'PROCESS_ACTION=BLOCKED\\nREASON=pid-no-longer-present\\n'; fi; fi";
        sessionManager.runRuntimeCommand(command, (output, code) -> postUi(() -> { processEvidence = false; disableKillButton(); showStatus("PROCESS_OPERATION_EXIT=" + code + "\n" + output); }));
    }

    private void showRuntimeTools() {
        if (!sessionReady()) { showRuntimeSelector(); return; }
        sessionManager.runRuntimeCommand("printf 'runtime-tools\\n'; command -v sh; command -v ps; command -v ip 2>/dev/null || true", (output, code) -> postUi(() -> showStatus("TOOLS_STATUS=" + code + "\n" + output)));
    }

    private void showRuntimeSelector() {
        if (sessionManager != null && sessionManager.isRunning()) { blocked("runtime selection", "hentikan session aktif terlebih dahulu"); return; }
        List<RuntimeProfile> profiles = RuntimeRegistry.all();
        String[] labels = new String[profiles.size()];
        int checked = 0;
        for (int i = 0; i < profiles.size(); i++) { RuntimeProfile p = profiles.get(i); labels[i] = p.displayName(); if (p.id().equals(selectedRuntime.id())) checked = i; }
        new AlertDialog.Builder(this).setTitle("Pilih runtime Linux").setSingleChoiceItems(labels, checked, (dialog, which) -> { selectRuntime(profiles.get(which).id()); dialog.dismiss(); }).setNegativeButton("Batal", null).show();
    }

    private void selectRuntime(String runtimeId) {
        RuntimeProfile profile = RuntimeSelection.profile(runtimeId);
        if (profile == null) { blocked("runtime selection", "runtime tidak didukung"); return; }
        if (sessionManager != null && sessionManager.isRunning()) { blocked("runtime selection", "hentikan session aktif terlebih dahulu"); return; }
        selectedRuntime = profile;
        getPreferences(MODE_PRIVATE).edit().putString(RUNTIME_PREF, profile.id()).apply();
        if (terminalTitle != null) terminalTitle.setText("runtime: " + profile.displayName());
        if (monitorText != null) monitorText.setText("Runtime telemetry menunggu sesi " + profile.displayName() + " terverifikasi.\nCPU: UNKNOWN    MEM: UNKNOWN    SWAP: UNKNOWN\nTidak ada angka mockup yang ditampilkan.");
        refreshRuntimeDashboard();
        showStatus("RUNTIME_SELECTED=" + profile.id() + "\n" + profile.displayName() + " akan dipakai untuk install dan session berikutnya");
    }

    private void showTerminalState() {
        activeTab.setText("terminal");
        showStatus(sessionReady() ? "TERMINAL_STATUS=READY\nPTY prompt terverifikasi; input dikirim ke runtime " + selectedRuntime.displayName() : "TERMINAL_STATUS=WAITING\nTekan Mulai sesi runtime terverifikasi setelah runtime READY");
    }

    private void showRuntimeFiles() {
        if (!sessionReady()) { showStatus("FILES_STATUS=WAITING\nFile view hanya membaca root runtime terpilih setelah sesi READY"); return; }
        sessionManager.runRuntimeCommand("pwd; ls -la", (output, code) -> postUi(() -> showStatus("FILES_STATUS=" + code + "\n" + output)));
    }

    private void showPolicy() {
        showStatus("POLICY=interactive-runtime.v1\nSCOPE=full-user-access-inside-selected-rootless-runtime\nANDROID_ROOT=NOT_GRANTED\nOTHER_APP_DATA=NOT_GRANTED");
    }

    private void showStatus(String text) {
        if (!uiActive || status == null) return;
        status.setVisibility(View.VISIBLE);
        status.setText(text);
    }

    private void installRuntime() {
        if (!uiActive) return;
        if (sessionManager != null && sessionManager.isRunning()) { blocked("installer", "hentikan session aktif terlebih dahulu"); return; }
        final RuntimeProfile profile = selectedRuntime;
        installingRuntimeId = profile.id();
        refreshRuntimeDashboard();
        status.setVisibility(View.VISIBLE);
        status.setText("INSTALL_STATUS=STARTING\nchecksum dan extraction berjalan di staging atomic\nRUNTIME=" + profile.id());
        new Thread(() -> {
            RuntimeInstaller.Result result;
            try {
                File vault = new File(getFilesDir(), "runtime-vault");
                File nativeLibraryDir = new File(getApplicationInfo().nativeLibraryDir);
                File packagedEngine = new File(nativeLibraryDir, "libproot.so");
                RuntimeInstaller installer = new RuntimeInstaller(vault, message -> postUi(() -> status.setText("INSTALL_STATUS=" + message)), packagedEngine, nativeLibraryDir);
                result = installer.install(profile, null, PROOT_SHA256, new URL(profile.rootfsUrl()), profile.rootfsSha256(), profile.rootfsGzip());
            } catch (Exception error) {
                result = RuntimeInstaller.Result.fail(profile.id(), error.getClass().getSimpleName() + ":" + error.getMessage(), null);
            }
            RuntimeInstaller.Result finalResult = result;
            postUi(() -> {
                installingRuntimeId = null;
                refreshRuntimeDashboard();
                status.setVisibility(View.VISIBLE);
                status.setText(finalResult.success ? "INSTALL_STATUS=READY\nRUNTIME=" + finalResult.runtimeId + "\nEvidence runtime-ready.v1 verified" : "INSTALL_STATUS=FAILED\n" + finalResult.message);
            });
        }, "alfa-ui-installer").start();
    }

    private void startSession() {
        if (!uiActive) return;
        if (sessionManager != null && sessionManager.isRunning()) { sessionManager.attachTo(terminalView); return; }
        final RuntimeProfile profile = selectedRuntime;
        File files = getFilesDir();
        File runtime = new File(new File(files, "runtime-vault"), "runtimes/" + profile.id());
        File sessionCwd = new File(files, "session-cwd");
        File ready = new File(runtime, "READY.evidence");
        File launch = new File(new File(files, "runtime-vault"), "gate7-launch.properties");
        if (!Gate6LaunchContract.verify(launch, profile.id())) {
            status.setVisibility(View.VISIBLE);
            status.setText("GATE6→GATE7=REQUIRED\nRUNTIME=" + profile.id() + "\nImport bootstrap Gate 6 current-run untuk runtime ini.");
            Intent intent = new Intent(this, Gate6ImportActivity.class);
            intent.putExtra("runtime_id", profile.id());
            startActivityForResult(intent, REQUEST_GATE6_IMPORT);
            return;
        }
        if (!ready.isFile()) { blocked("sesi terminal", "runtime-ready.v1 belum tersedia untuk " + profile.displayName()); return; }
        if (!sessionCwd.exists() && !sessionCwd.mkdirs()) { blocked("sesi terminal", "session cwd tidak dapat dibuat"); return; }
        InteractiveSessionContract contract = new InteractiveSessionContract(
                "session-" + shortId(), "request-" + shortId(), "run-" + shortId(), profile.id(), ready,
                new File(getApplicationInfo().nativeLibraryDir, "libproot.so"), new File(runtime, "rootfs"), sessionCwd,
                new String[]{"HOME=/root", "TERM=xterm-256color", "PS1=alfa:" + profile.id() + ":\\w\\$ ", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "PROOT_TMP_DIR=" + new File(runtime, "proot_tmp").getAbsolutePath()});
        sessionManager = new RuntimeSessionManager(contract, this);
        if (sessionManager.start(80, 24, 8, 16)) {
            sessionManager.attachTo(terminalView);
            if (sessionManager.currentSession() != null) terminalTitle.setText("session: " + sessionManager.currentSession().mSessionName);
            refreshRuntimeDashboard();
        } else blocked("sesi terminal", "runtime evidence belum lengkap untuk " + profile.displayName());
    }

    private void stopSession() {
        if (sessionManager != null && sessionManager.isRunning()) sessionManager.stop();
        else blocked("terminal", "tidak ada session aktif");
    }

    private void addShortcut(LinearLayout parent, String text, byte[] bytes) {
        Button b = actionButton(text, MUTED, v -> send(bytes));
        b.setContentDescription("Kirim tombol " + text + " ke terminal");
        parent.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
    }

    private void send(byte[] bytes) {
        if (!uiActive) return;
        TerminalSession s = sessionManager == null ? null : sessionManager.currentSession();
        if (s == null || !s.isRunning()) { blocked("input", "session PTY belum aktif"); return; }
        if (ctrlLatch && bytes.length == 1 && bytes[0] >= 0x40 && bytes[0] <= 0x7f) bytes[0] = (byte) (bytes[0] & 0x1f);
        s.write(bytes, 0, bytes.length);
        ctrlLatch = false;
    }

    private void blocked(String target, String reason) { showStatus("STATUS=BLOCKED\n" + target + ": " + reason); }
    private String shortId() { return UUID.randomUUID().toString().replace("-", "").substring(0, 12); }
    private void disableKillButton() { if (killProcessButton != null) { killProcessButton.setEnabled(false); killProcessButton.setAlpha(.45f); } }

    private void addTab(LinearLayout parent, TextView text, String title, View.OnClickListener click) {
        text.setText(title); text.setGravity(Gravity.CENTER); text.setContentDescription("Buka tab " + title); text.setFocusable(true); text.setOnClickListener(click);
        parent.addView(text, new LinearLayout.LayoutParams(0, dp(48), 1));
    }

    private void addDock(LinearLayout parent, String text, String description, View.OnClickListener click) {
        Button b = actionButton(text, MUTED, click); b.setContentDescription(description); parent.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1));
    }

    private Button actionButton(String text, int color, View.OnClickListener click) {
        Button b = new Button(this); b.setText(text); b.setTextColor(color); b.setTextSize(11); b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setMinHeight(dp(48)); b.setMinWidth(0); b.setPadding(dp(6), 0, dp(6), 0); b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); b.setBackground(controlBackground(color)); b.setStateListAnimator(null); b.setOnClickListener(click); return b;
    }

    private TextView label(String text, int color, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(text); t.setTextColor(color); t.setTextSize(size); if (bold) t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); return t;
    }

    private LinearLayout row(int color) { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setBackgroundColor(color); return l; }
    private LinearLayout column(int color) { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setBackgroundColor(color); return l; }

    private GradientDrawable panelBackground(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); d.setStroke(dp(1), Color.argb(70, 255, 255, 255)); return d; }
    private GradientDrawable controlBackground(int textColor) { int fill = textColor == PRIMARY ? Color.rgb(50, 73, 111) : PANEL_HIGH; int stroke = textColor == ERROR ? Color.rgb(104, 70, 70) : Color.argb(110, 220, 225, 240); GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(7)); d.setStroke(dp(1), stroke); return d; }
    private View space(int height) { View spacer = new View(this); spacer.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(height))); return spacer; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onState(String state) {
        postUi(() -> { if (status == null) return; status.setText("SESSION_STATUS=" + state); if ("READY".equals(state) || "RUNNING".equals(state)) { status.setVisibility(View.GONE); if (terminalTitle != null && sessionManager != null && sessionManager.currentSession() != null) terminalTitle.setText("session: " + sessionManager.currentSession().mSessionName); } else status.setVisibility(View.VISIBLE); refreshRuntimeDashboard(); });
    }

    @Override public void onTextChanged() { postUi(() -> { if (terminalView != null) terminalView.invalidate(); }); }

    @Override public void onSessionFinished(int exitStatus) { postUi(() -> { if (status != null) { status.setVisibility(View.VISIBLE); status.setText("SESSION_STATUS=FINISHED\nexit_status=" + exitStatus); } refreshRuntimeDashboard(); }); }
}
