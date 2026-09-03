package com.alfa.device_ctrl;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

/** Internal native UI adaptation of the supplied Alfa desktop reference. */
public final class MainActivity extends Activity implements RuntimeSessionManager.Listener {
    private static final int BG = Color.rgb(19, 19, 21);
    private static final int PANEL = Color.rgb(32, 31, 33);
    private static final int PANEL_HIGH = Color.rgb(42, 42, 44);
    private static final int PRIMARY = Color.rgb(171, 199, 255);
    private static final int MUTED = Color.rgb(193, 198, 213);
    private static final int ERROR = Color.rgb(255, 180, 171);
    private static final String PROOT_URL = "https://skirsten.github.io/proot-portable-android-binaries/aarch64/proot";
    private static final String PROOT_SHA256 = "c902f35b3bce4013d2e78e3bf360b606523d55ab7b907578938577b243bfca38";
    private static final String UBUNTU_URL = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.4-base-arm64.tar.gz";
    private static final String UBUNTU_SHA256 = "04207713ece899c3740823d33690441ad3a7f0ded1101aca744e2b0f37ac7ff2";

    private TerminalView terminalView;
    private RuntimeSessionManager sessionManager;
    private TextView status;
    private TextView terminalTitle;
    private TextView activeTab;
    private TextView monitorText;
    private Button killProcessButton;
    private boolean processEvidence;
    private boolean ctrlLatch;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.rgb(14, 14, 16));
        setContentView(buildShellUi());
    }

    private View buildShellUi() {
        LinearLayout root = column(BG);
        root.addView(topBar(), new LinearLayout.LayoutParams(-1, dp(52)));
        root.addView(tabBar(), new LinearLayout.LayoutParams(-1, dp(52)));
        root.addView(resourceAlertCard(), new LinearLayout.LayoutParams(-1, -2));

        LinearLayout content = column(BG);
        content.setPadding(dp(12), dp(12), dp(12), dp(10));
        content.addView(monitorWindow(), new LinearLayout.LayoutParams(-1, dp(188)));
        content.addView(space(10));
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
        TextView brand = label(getString(com.alfa.device_ctrl.R.string.alfa_os), PRIMARY, 16, true);
        brand.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        bar.addView(brand, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView telemetry = label("PERF: UNKNOWN  |  network: UNKNOWN", MUTED, 10, false);
        telemetry.setTypeface(Typeface.MONOSPACE);
        bar.addView(telemetry, new LinearLayout.LayoutParams(-2, dp(42)));
        return bar;
    }

    private View tabBar() {
        LinearLayout bar = row(PANEL);
        bar.setPadding(dp(8), 0, dp(8), 0);
        activeTab = label("bash", PRIMARY, 14, true);
        addTab(bar, activeTab, "bash", v -> selectTab("bash"));
        addTab(bar, label("htop", MUTED, 14, false), "htop", v -> showHtop());
        addTab(bar, label("jaringan", MUTED, 14, false), "jaringan", v -> showNetwork());
        Button plus = actionButton("+", PRIMARY, v -> createSession());
        bar.addView(plus, new LinearLayout.LayoutParams(dp(54), dp(42)));
        return bar;
    }

    private View resourceAlertCard() {
        final ResourceAlertState alert = ResourceAlertState.unknown();
        final LinearLayout card = row(Color.rgb(53, 52, 55));
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(9), dp(10), dp(9));
        TextView message = label(getString(com.alfa.device_ctrl.R.string.resource_alert_title) + "\n" + alert.displayMessage(), ERROR, 12, true);
        message.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        message.setLineSpacing(0, 1.06f);
        card.addView(message, new LinearLayout.LayoutParams(0, -2, 1));
        Button dismiss = actionButton(getString(com.alfa.device_ctrl.R.string.dismiss), MUTED, v -> card.setVisibility(View.GONE));
        card.addView(dismiss, new LinearLayout.LayoutParams(dp(78), dp(44)));
        killProcessButton = actionButton(getString(com.alfa.device_ctrl.R.string.kill_process), ERROR, v -> showProcessPicker());
        killProcessButton.setEnabled(false);
        killProcessButton.setAlpha(0.45f);
        card.addView(killProcessButton, new LinearLayout.LayoutParams(dp(132), dp(44)));
        return card;
    }

    private View monitorWindow() {
        LinearLayout window = column(Color.BLACK);
        window.setBackground(panelBackground(PANEL, dp(12)));
        LinearLayout header = row(PANEL_HIGH);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), 0, dp(8), 0);
        TextView title = label("htop", MUTED, 12, false);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(34), 1));
        TextView state = label("TELEMETRY: UNKNOWN", MUTED, 9, false);
        state.setTypeface(Typeface.MONOSPACE);
        header.addView(state, new LinearLayout.LayoutParams(-2, dp(34)));
        window.addView(header);

        LinearLayout body = column(Color.BLACK);
        body.setPadding(dp(14), dp(12), dp(14), dp(12));
        monitorText = label("Runtime telemetry menunggu sesi Ubuntu terverifikasi.\nCPU: UNKNOWN    MEM: UNKNOWN    SWAP: UNKNOWN\nTidak ada angka mockup yang ditampilkan.", MUTED, 12, false);
        monitorText.setTypeface(Typeface.MONOSPACE);
        monitorText.setLineSpacing(0, 1.15f);
        body.addView(monitorText, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout meter = row(Color.rgb(31, 31, 34));
        meter.setPadding(dp(8), 0, dp(8), 0);
        TextView meterText = label("[ telemetry evidence required ]", MUTED, 10, false);
        meterText.setGravity(Gravity.CENTER_VERTICAL);
        meter.addView(meterText, new LinearLayout.LayoutParams(0, dp(24), 1));
        body.addView(meter, new LinearLayout.LayoutParams(-1, dp(24)));
        window.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        return window;
    }

    private View terminalWindow() {
        LinearLayout window = column(Color.BLACK);
        window.setBackground(panelBackground(PANEL, dp(14)));
        LinearLayout header = row(PANEL_HIGH);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), 0, dp(10), 0);
        terminalTitle = label("runtime: UNKNOWN", PRIMARY, 13, true);
        terminalTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        header.addView(terminalTitle, new LinearLayout.LayoutParams(0, dp(40), 1));
        header.addView(actionButton("×", ERROR, v -> stopSession()), new LinearLayout.LayoutParams(dp(44), dp(42)));
        window.addView(header);

        FrameLayout terminalFrame = new FrameLayout(this);
        terminalView = new TerminalView(this, null);
        terminalView.setTerminalViewClient(new AlfaTerminalViewClient());
        terminalFrame.addView(terminalView, new FrameLayout.LayoutParams(-1, -1));
        status = label(getString(com.alfa.device_ctrl.R.string.session_blocked) + "\n" + getString(com.alfa.device_ctrl.R.string.session_blocked_reason), MUTED, 13, false);
        status.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        status.setLineSpacing(0, 1.12f);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(20), dp(20), dp(20), dp(20));
        status.setBackgroundColor(Color.argb(210, 19, 19, 21));
        terminalFrame.addView(status, new FrameLayout.LayoutParams(-1, -1));
        window.addView(terminalFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout actions = row(Color.BLACK);
        actions.setPadding(dp(6), dp(5), dp(6), dp(5));
        Button install = actionButton("INSTALL UBUNTU", PRIMARY, v -> installUbuntu());
        Button start = actionButton(getString(com.alfa.device_ctrl.R.string.start_verified_session), PRIMARY, v -> startSession());
        LinearLayout.LayoutParams installParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        installParams.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        startParams.setMargins(dp(4), 0, 0, 0);
        actions.addView(install, installParams);
        actions.addView(start, startParams);
        window.addView(actions, new LinearLayout.LayoutParams(-1, dp(56)));
        return window;
    }

    private View shortcutBar() {
        LinearLayout bar = row(PANEL_HIGH);
        bar.setPadding(dp(8), dp(6), dp(8), dp(6));
        addShortcut(bar, "ESC", new byte[] {27});
        Button ctrl = actionButton("CTRL", MUTED, v -> { ctrlLatch = !ctrlLatch; v.setAlpha(ctrlLatch ? 1f : 0.65f); });
        bar.addView(ctrl, new LinearLayout.LayoutParams(0, dp(42), 1));
        addShortcut(bar, "ALT", new byte[] {27});
        addShortcut(bar, "TAB", new byte[] {9});
        addShortcut(bar, "←", new byte[] {27, '[', 'D'});
        addShortcut(bar, "↓", new byte[] {27, '[', 'B'});
        addShortcut(bar, "↑", new byte[] {27, '[', 'A'});
        addShortcut(bar, "→", new byte[] {27, '[', 'C'});
        return bar;
    }

    private View dockBar() {
        LinearLayout dock = row(PANEL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8), dp(4), dp(8), dp(4));
        addDock(dock, "⌘", v -> showRuntimeTools());
        addDock(dock, "▣", v -> showTerminalState());
        addDock(dock, "□", v -> showRuntimeFiles());
        addDock(dock, "⚙", v -> showPolicy());
        return dock;
    }

    private boolean sessionReady() {
        return sessionManager != null && sessionManager.isRunning() && sessionManager.isPromptReady();
    }

    private void createSession() {
        if (sessionReady()) {
            showStatus("SESSION_STATUS=ALREADY_READY\nSesi aktif dipakai oleh terminal utama");
        } else if (sessionManager != null && sessionManager.isRunning()) {
            showStatus("SESSION_STATUS=WAITING\nPTY aktif, menunggu prompt runtime terverifikasi");
        } else {
            startSession();
        }
    }

    private void showHtop() {
        activeTab.setText("htop");
        if (!sessionReady()) {
            showStatus("TAB=htop\nSTATUS=WAITING\nSesi Ubuntu terverifikasi diperlukan untuk telemetry live");
            return;
        }
        monitorText.setText("htop\nMengambil CPU, MEM, SWAP, dan proses dari runtime Ubuntu...");
        String telemetryCommand = "cpu_line=$(head -n 1 /proc/stat); set -- $cpu_line; u1=$2; n1=$3; s1=$4; i1=$5; w1=$6; q1=$7; sq1=$8; st1=$9; t1=$((u1+n1+s1+i1+w1+q1+sq1+st1)); id1=$((i1+w1)); sleep 1; cpu_line=$(head -n 1 /proc/stat); set -- $cpu_line; u2=$2; n2=$3; s2=$4; i2=$5; w2=$6; q2=$7; sq2=$8; st2=$9; t2=$((u2+n2+s2+i2+w2+q2+sq2+st2)); id2=$((i2+w2)); dt=$((t2-t1)); di=$((id2-id1)); used=$((dt-di)); if [ $dt -gt 0 ]; then cpu=$((used*100/dt)); else cpu=0; fi; mt=$(awk '/MemTotal:/{print $2}' /proc/meminfo); ma=$(awk '/MemAvailable:/{print $2}' /proc/meminfo); st=$(awk '/SwapTotal:/{print $2}' /proc/meminfo); sf=$(awk '/SwapFree:/{print $2}' /proc/meminfo); mt=${mt:-0}; ma=${ma:-0}; st=${st:-0}; sf=${sf:-0}; mu=$((mt-ma)); su=$((st-sf)); if [ $mt -gt 0 ]; then mp=$((mu*100/mt)); else mp=0; fi; if [ $st -gt 0 ]; then sp=$((su*100/st)); else sp=0; fi; printf 'TELEMETRY_EVIDENCE=PASS\\nCPU=%s%%\\nMEM=%s/%s kB (%s%%)\\nSWAP=%s/%s kB (%s%%)\\n' $cpu $mu $mt $mp $su $st $sp; printf 'PROCESS_EVIDENCE=PASS\\n'; ps -eo pid=,user=,stat=,pcpu=,pmem=,comm= --sort=-pcpu | head -n 12";
        sessionManager.runRuntimeCommand(telemetryCommand, (output, code) -> runOnUiThread(() -> {
            processEvidence = code == 0 && output.contains("PROCESS_EVIDENCE=PASS");
            if (killProcessButton != null) {
                killProcessButton.setEnabled(processEvidence);
                killProcessButton.setAlpha(processEvidence ? 1f : 0.45f);
            }
            monitorText.setText(code == 0 && output.contains("TELEMETRY_EVIDENCE=PASS") ? output : "htop\nSTATUS=BLOCKED\ntelemetry evidence gagal\n" + output);
        }));
    }

    private void showNetwork() {
        activeTab.setText("jaringan");
        if (!sessionReady()) {
            showStatus("TAB=jaringan\nSTATUS=WAITING\nSesi Ubuntu terverifikasi diperlukan untuk network evidence");
            return;
        }
        monitorText.setText("jaringan\nMengambil interface dan route dari runtime Ubuntu...");
        String networkCommand = "iface_count=$(awk 'NR>2 && $1 !~ /^lo:/ {c++} END{print c+0}' /proc/net/dev); route_count=$(awk 'NR>1 && $2==\"00000000\" && $1!=\"lo\" {c++} END{print c+0}' /proc/net/route); if [ \"$iface_count\" -gt 0 ]; then printf 'NETWORK_EVIDENCE=PASS\\nINTERFACES\\n'; awk 'NR>2 {print}' /proc/net/dev; printf '\\nROUTES\\n'; cat /proc/net/route; printf '\\nDEFAULT_ROUTE_COUNT=%s\\n' $route_count; else printf 'NETWORK_EVIDENCE=BLOCKED\\nREASON=no-nonloopback-interface-in-runtime\\n'; fi";
        sessionManager.runRuntimeCommand(networkCommand, (output, code) -> runOnUiThread(() -> {
            monitorText.setText(code == 0 && output.contains("NETWORK_EVIDENCE=PASS") ? output : "jaringan\nSTATUS=BLOCKED\n" + output);
        }));
    }

    private void showProcessPicker() {
        if (!sessionReady()) {
            blocked("hentikan proses", "sesi PTY belum siap; process evidence belum tersedia");
            return;
        }
        if (!processEvidence) {
            blocked("hentikan proses", "process evidence live belum tersedia");
            return;
        }
        sessionManager.runRuntimeCommand("ps -eo pid=,comm= --sort=-pcpu | head -n 20", (output, code) -> runOnUiThread(() -> {
            if (code != 0 || output.trim().isEmpty()) {
                processEvidence = false;
                if (killProcessButton != null) { killProcessButton.setEnabled(false); killProcessButton.setAlpha(0.45f); }
                blocked("hentikan proses", "process evidence snapshot gagal");
                return;
            }
            ArrayList<String> pids = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();
            for (String line : output.split("\\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                String[] parts = trimmed.split("\\\\s+", 2);
                if (parts.length == 0 || !parts[0].matches("[0-9]+")) continue;
                pids.add(parts[0]);
                labels.add(trimmed);
            }
            if (pids.isEmpty()) {
                blocked("hentikan proses", "process evidence tidak berisi PID valid");
                return;
            }
            new AlertDialog.Builder(this).setTitle("Pilih proses dalam runtime Ubuntu").setItems(labels.toArray(new String[0]), (dialog, which) -> killSelectedProcess(pids.get(which))).setNegativeButton("Batal", null).show();
        }));
    }

    private void killSelectedProcess(String pid) {
        if (!pid.matches("[0-9]+") || !sessionReady() || !processEvidence) {
            blocked("hentikan proses", "capability/process evidence tidak tersedia");
            return;
        }
        String command = "pid=" + pid + "; if [ \\\"$pid\\\" -le 1 ] || [ \\\"$pid\\\" -eq \\\"$$\\\" ]; then printf 'PROCESS_ACTION=BLOCKED\\nREASON=protected-runtime-process\\n'; else if [ -r \\\"/proc/$pid/stat\\\" ]; then kill -TERM \\\"$pid\\\" 2>/dev/null; rc=$?; printf 'PROCESS_SCOPE=selected-rootless-runtime\\nPROCESS_PID=%s\\nPROCESS_ACTION=%s\\n' \\\"$pid\\\" $rc; else printf 'PROCESS_ACTION=BLOCKED\\nREASON=pid-no-longer-present\\n'; fi; fi";
        sessionManager.runRuntimeCommand(command, (output, code) -> runOnUiThread(() -> {
            processEvidence = false;
            if (killProcessButton != null) { killProcessButton.setEnabled(false); killProcessButton.setAlpha(0.45f); }
            showStatus("PROCESS_OPERATION_EXIT=" + code + "\n" + output);
        }));
    }

    private void showRuntimeTools() {
        if (!sessionReady()) {
            showStatus("TOOLS_STATUS=WAITING\nAktif setelah prompt runtime terverifikasi");
            return;
        }
        sessionManager.runRuntimeCommand("printf 'runtime-tools\\n'; command -v sh; command -v ps; command -v ip 2>/dev/null || true", (output, code) -> runOnUiThread(() -> showStatus("TOOLS_STATUS=" + code + "\n" + output)));
    }

    private void showTerminalState() {
        activeTab.setText("bash");
        showStatus(sessionReady() ? "TERMINAL_STATUS=READY\nPTY prompt terverifikasi; input dikirim ke runtime Ubuntu" : "TERMINAL_STATUS=WAITING\nTekan Mulai sesi runtime terverifikasi setelah runtime READY");
    }

    private void showRuntimeFiles() {
        if (!sessionReady()) {
            showStatus("FILES_STATUS=WAITING\nFile view hanya membaca root runtime terpilih setelah sesi READY");
            return;
        }
        sessionManager.runRuntimeCommand("pwd; ls -la", (output, code) -> runOnUiThread(() -> showStatus("FILES_STATUS=" + code + "\n" + output)));
    }

    private void showPolicy() {
        showStatus("POLICY=interactive-runtime.v1\nSCOPE=full-user-access-inside-selected-rootless-runtime\nANDROID_ROOT=NOT_GRANTED\nOTHER_APP_DATA=NOT_GRANTED");
    }

    private void showStatus(String text) {
        status.setVisibility(View.VISIBLE);
        status.setText(text);
    }

    private void installUbuntu() {
        if (sessionManager != null && sessionManager.isRunning()) { blocked("installer", "hentikan session aktif terlebih dahulu"); return; }
        status.setVisibility(View.VISIBLE);
        status.setText("INSTALL_STATUS=STARTING\nchecksum dan extraction berjalan di staging atomic");
        new Thread(() -> {
            RuntimeInstaller.Result result;
            try {
                File vault = new File(getFilesDir(), "runtime-vault");
                File nativeLibraryDir = new File(getApplicationInfo().nativeLibraryDir);
                File packagedEngine = new File(nativeLibraryDir, "libproot.so");
                RuntimeInstaller installer = new RuntimeInstaller(vault, message -> runOnUiThread(() -> status.setText("INSTALL_STATUS=" + message)), packagedEngine, nativeLibraryDir);
                result = installer.install("ubuntu", null, PROOT_SHA256, new URL(UBUNTU_URL), UBUNTU_SHA256, true);
            } catch (Exception error) {
                result = RuntimeInstaller.Result.fail("ubuntu", error.getClass().getSimpleName() + ":" + error.getMessage(), null);
            }
            RuntimeInstaller.Result finalResult = result;
            runOnUiThread(() -> {
                status.setVisibility(View.VISIBLE);
                status.setText(finalResult.success ? "INSTALL_STATUS=READY\nRUNTIME=ubuntu\nEvidence runtime-ready.v1 verified" : "INSTALL_STATUS=FAILED\n" + finalResult.message);
            });
        }).start();
    }

    private void startSession() {
        if (sessionManager != null && sessionManager.isRunning()) {
            sessionManager.attachTo(terminalView);
            return;
        }
        File files = getFilesDir();
        File runtime = new File(new File(files, "runtime-vault"), "runtimes/ubuntu");
        File sessionCwd = new File(files, "session-cwd");
        if (!sessionCwd.exists() && !sessionCwd.mkdirs()) { blocked("sesi terminal", "session cwd tidak dapat dibuat"); return; }
        InteractiveSessionContract contract = new InteractiveSessionContract(
                "session-" + shortId(), "request-" + shortId(), "run-" + shortId(), "ubuntu",
                new File(runtime, "READY.evidence"), new File(getApplicationInfo().nativeLibraryDir, "libproot.so"),
                new File(runtime, "rootfs"), sessionCwd,
                new String[] {
                        "HOME=/root",
                        "TERM=xterm-256color",
                        "PS1=alfa:ubuntu:\\w\\$ ",
                        "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                        "PROOT_TMP_DIR=" + new File(runtime, "proot_tmp").getAbsolutePath()
                });
        sessionManager = new RuntimeSessionManager(contract, this);
        if (sessionManager.start(80, 24, 8, 16)) {
            sessionManager.attachTo(terminalView);
            terminalTitle.setText("session: " + sessionManager.currentSession().mSessionName);
        } else blocked("sesi terminal", "runtime evidence belum lengkap");
    }

    private void stopSession() {
        if (sessionManager != null) sessionManager.stop();
        else blocked("terminal", "tidak ada session aktif");
    }

    private void addShortcut(LinearLayout parent, String text, byte[] bytes) {
        Button b = actionButton(text, MUTED, v -> send(bytes));
        parent.addView(b, new LinearLayout.LayoutParams(0, dp(42), 1));
    }

    private void send(byte[] bytes) {
        TerminalSession s = sessionManager == null ? null : sessionManager.currentSession();
        if (s == null || !s.isRunning()) { blocked("input", "session PTY belum aktif"); return; }
        if (ctrlLatch && bytes.length == 1 && bytes[0] >= 0x40 && bytes[0] <= 0x7f) bytes[0] = (byte) (bytes[0] & 0x1f);
        s.write(bytes, 0, bytes.length);
        ctrlLatch = false;
    }

    private void selectTab(String tab) { activeTab.setText(tab); showStatus("TAB=" + tab + "\nSTATUS=READY\nTerminal utama aktif"); }
    private void blocked(String target, String reason) { showStatus("STATUS=BLOCKED\n" + target + ": " + reason); }
    private String shortId() { return UUID.randomUUID().toString().replace("-", "").substring(0, 12); }

    private void addTab(LinearLayout parent, TextView text, String title, View.OnClickListener click) {
        text.setText(title);
        text.setGravity(Gravity.CENTER);
        text.setOnClickListener(click);
        parent.addView(text, new LinearLayout.LayoutParams(0, dp(42), 1));
    }

    private void addDock(LinearLayout parent, String text, View.OnClickListener click) {
        Button b = actionButton(text, MUTED, click);
        parent.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1));
    }

    private Button actionButton(String text, int color, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(color);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(40));
        b.setMinWidth(0);
        b.setPadding(dp(8), 0, dp(8), 0);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setBackground(controlBackground(color));
        b.setStateListAnimator(null);
        b.setOnClickListener(click);
        return b;
    }

    private TextView label(String text, int color, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        return t;
    }

    private LinearLayout row(int color) { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setBackgroundColor(color); return l; }
    private LinearLayout column(int color) { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setBackgroundColor(color); return l; }
    private GradientDrawable panelBackground(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); d.setStroke(dp(1), Color.argb(70, 255, 255, 255)); return d; }
    private GradientDrawable controlBackground(int textColor) {
        int fill = textColor == PRIMARY ? Color.rgb(50, 73, 111) : PANEL_HIGH;
        int stroke = textColor == ERROR ? Color.rgb(104, 70, 70) : Color.argb(110, 220, 225, 240);
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(7));
        d.setStroke(dp(1), stroke);
        return d;
    }
    private View space(int height) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(height)));
        return spacer;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() { if (sessionManager != null) sessionManager.stop(); super.onDestroy(); }
    @Override public void onState(String state) {
        if (status == null) return;
        status.setText("SESSION_STATUS=" + state);
        if ("READY".equals(state) || "RUNNING".equals(state)) {
            status.setVisibility(View.GONE);
            if (terminalTitle != null && sessionManager != null && sessionManager.currentSession() != null) {
                terminalTitle.setText("session: " + sessionManager.currentSession().mSessionName);
            }
        }
        else status.setVisibility(View.VISIBLE);
    }
    @Override public void onTextChanged() { if (terminalView != null) terminalView.invalidate(); }
    @Override public void onSessionFinished(int exitStatus) {
        if (status != null) {
            status.setVisibility(View.VISIBLE);
            status.setText("SESSION_STATUS=FINISHED\nexit_status=" + exitStatus);
        }
    }
}
