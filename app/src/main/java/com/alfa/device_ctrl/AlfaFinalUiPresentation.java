package com.alfa.device_ctrl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.terminal.TerminalColors;
import com.termux.view.TerminalRenderer;
import com.termux.view.TerminalView;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Native presentation layer with one real navigation/action boundary around the canonical runtime engine. */
public final class AlfaFinalUiPresentation {
    static final int TAG_ID = 0xA1FA001;
    public static final int CYAN = 0xFF38BDF8;
    public static final int TERMINAL = 0xFF0D1117;
    public static final List<Integer> FONT_SIZES_SP = Arrays.asList(11, 13, 15);
    public static final List<Float> LINE_HEIGHTS = Arrays.asList(1.0f, 1.25f, 1.5f);

    private static final String PREF_FONT = "alfa_terminal_font_size";
    private static final String PREF_LINE_HEIGHT = "alfa_terminal_line_height";
    private static final String PREF_THEME = "alfa_terminal_theme";
    private static final String PREF_CURSOR = "alfa_terminal_cursor";

    private final Activity activity;
    private final TerminalView terminal;
    private final FrameLayout panelHost;
    private final LinearLayout terminalCard;
    private final LinearLayout accessory;
    private final LinearLayout bottomNav;
    private final TextView runtimeMeta;
    private final TextView panelStatus;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SharedPreferences preferences;
    private int fontSize;
    private float lineHeight;
    private String theme;
    private int cursorStyle;
    private boolean fullscreen;
    private boolean ctrlLatch;
    private AlfaUiNavigation.Screen screen = AlfaUiNavigation.Screen.TERMINAL;
    private AlfaUiNavigation.Screen previousScreen = AlfaUiNavigation.Screen.TERMINAL;
    private android.window.OnBackInvokedCallback backCallback;
    private boolean backRegistered;
    private String lastRuntimeSignature = "";

    private AlfaFinalUiPresentation(Activity activity, TerminalView terminal) {
        this.activity = activity;
        this.terminal = terminal;
        this.panelHost = new FrameLayout(activity);
        this.terminalCard = card();
        this.accessory = row(AlfaUiTheme.SURFACE_1);
        this.bottomNav = row(AlfaUiTheme.CANVAS);
        this.preferences = activity.getPreferences(Activity.MODE_PRIVATE);
        this.fontSize = preferences.getInt(PREF_FONT, 13);
        this.lineHeight = preferences.getFloat(PREF_LINE_HEIGHT, 1.25f);
        this.theme = preferences.getString(PREF_THEME, "obsidian");
        this.cursorStyle = preferences.getInt(PREF_CURSOR, 2);
        RuntimeProfile profile = selectedProfile();
        String runtimeName = profile == null ? "UNKNOWN" : profile.displayName();
        this.runtimeMeta = text("RUNTIME: " + runtimeName + "  •  PTY: WAITING", AlfaUiTheme.TEXT_MUTED, 11, true);
        this.panelStatus = text("", AlfaUiTheme.TEXT_MUTED, 10, true);
        this.panelStatus.setVisibility(View.GONE);
        this.panelStatus.setPadding(dp(10), dp(6), dp(10), dp(6));
        this.panelStatus.setBackgroundColor(0xCC0D1117);
    }

    public static void apply(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        View raw = activity.findViewById(android.R.id.content);
        if (!(raw instanceof FrameLayout)) return;
        FrameLayout content = (FrameLayout) raw;
        if (content.getTag(TAG_ID) instanceof AlfaFinalUiPresentation) return;
        TerminalView terminal = findTerminal(content);
        if (terminal == null) return;
        detach(terminal);
        AlfaFinalUiPresentation ui = new AlfaFinalUiPresentation(activity, terminal);
        content.removeAllViews();
        content.setBackgroundColor(AlfaUiTheme.CANVAS);
        content.addView(ui.build(), new FrameLayout.LayoutParams(-1, -1));
        content.setTag(TAG_ID, ui);
        ui.configureTerminal();
        ui.installInsets();
        ui.installBackHandling();
        ui.startStateRefresh();
    }

    private View build() {
        LinearLayout root = column(AlfaUiTheme.CANVAS);
        root.setPadding(dp(12), dp(8), dp(12), 0);
        root.addView(header(), new LinearLayout.LayoutParams(-1, dp(60)));
        root.addView(runtimeMeta(), new LinearLayout.LayoutParams(-1, dp(44)));
        FrameLayout body = new FrameLayout(activity);
        terminalCard.addView(terminalHeader(), new LinearLayout.LayoutParams(-1, dp(48)));
        FrameLayout terminalFrame = new FrameLayout(activity);
        terminalFrame.setBackgroundColor(TERMINAL);
        terminalFrame.addView(terminal, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM | Gravity.START);
        terminalFrame.addView(panelStatus, statusParams);
        terminalCard.addView(terminalFrame, new LinearLayout.LayoutParams(-1, 0, 1));
        body.addView(terminalCard, new FrameLayout.LayoutParams(-1, -1));
        panelHost.setVisibility(View.GONE);
        body.addView(panelHost, new FrameLayout.LayoutParams(-1, -1));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(accessoryBar(), new LinearLayout.LayoutParams(-1, dp(56)));
        root.addView(bottomBar(), new LinearLayout.LayoutParams(-1, dp(62)));
        return root;
    }

    private View header() {
        LinearLayout bar = row(AlfaUiTheme.CANVAS);
        TextView brand = text("● ALFA::CTRL", AlfaUiTheme.READY, 18, true);
        brand.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        bar.addView(brand, new LinearLayout.LayoutParams(0, -1, 1));
        Button settings = button("⚙", AlfaUiTheme.TEXT_MUTED, v -> showAppearanceSettings());
        settings.setContentDescription("Pengaturan terminal dan tampilan");
        bar.addView(settings, size48());
        return bar;
    }

    private View runtimeMeta() {
        LinearLayout bar = row(AlfaUiTheme.SURFACE_1);
        bar.setPadding(dp(12), 0, dp(8), 0);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(runtimeMeta, new LinearLayout.LayoutParams(0, -1, 1));
        Button runtime = button("RUNTIME", CYAN, v -> showScreen(AlfaUiNavigation.Screen.RUNTIME));
        runtime.setContentDescription("Buka Linux runtime dashboard");
        bar.addView(runtime, new LinearLayout.LayoutParams(dp(92), dp(48)));
        return bar;
    }

    private View terminalHeader() {
        LinearLayout bar = row(AlfaUiTheme.SURFACE_2);
        TextView title = text("ALFA PTY  •  INTERACTIVE SESSION", AlfaUiTheme.TEXT, 11, true);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
        return bar;
    }

    private View accessoryBar() {
        accessory.setPadding(dp(4), dp(4), dp(4), dp(4));
        String[] labels = {"ESC", "TAB", "CTRL", "ALT", "^C", "|", "/", "-", "↑", "↓", "COPY", "PASTE"};
        for (String label : labels) {
            Button b = button(label, AlfaUiTheme.TEXT, v -> accessoryAction(label));
            b.setContentDescription("PTY modifier " + label);
            accessory.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        return accessory;
    }

    private View bottomBar() {
        bottomNav.setGravity(Gravity.CENTER);
        addNav(bottomNav, "Nodes", v -> showScreen(AlfaUiNavigation.Screen.RUNTIME));
        addNav(bottomNav, "Alfa RF", v -> { });
        addNav(bottomNav, "Lanes", v -> showScreen(AlfaUiNavigation.Screen.LANES));
        addNav(bottomNav, "Diagnostics", v -> { });
        return bottomNav;
    }

    private void accessoryAction(String label) {
        if (terminal.mEmulator == null) return;
        if ("ESC".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, 27, false, false);
        else if ("TAB".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, 9, false, false);
        else if ("CTRL".equals(label)) ctrlLatch = !ctrlLatch;
        else if ("ALT".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, 27, false, true);
        else if ("^C".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, 3, false, false);
        else if ("|".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, '|', ctrlLatch, false);
        else if ("/".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, '/', ctrlLatch, false);
        else if ("-".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, '-', ctrlLatch, false);
        else if ("↑".equals(label)) sendEscapeSequence("[A");
        else if ("↓".equals(label)) sendEscapeSequence("[B");
        else if ("COPY".equals(label)) copyAllTranscript();
        else if ("PASTE".equals(label)) pasteClipboard();
        if (!"CTRL".equals(label)) ctrlLatch = false;
    }

    private void sendEscapeSequence(String sequence) {
        if (terminal.mTermSession != null) terminal.mTermSession.write("\033" + sequence);
    }

    private void copyAllTranscript() {
        if (terminal.mEmulator == null || terminal.mEmulator.getScreen() == null) {
            blocked("COPY", "terminal transcript belum tersedia");
            return;
        }
        ClipboardManager manager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null) {
            blocked("COPY", "clipboard service tidak tersedia");
            return;
        }
        String text = terminal.mEmulator.getScreen().getTranscriptTextWithFullLinesJoined();
        if (!TerminalClipboardBridge.copy(new TerminalClipboardBridge.ClipboardPort() {
            @Override public void setText(String value) { manager.setPrimaryClip(ClipData.newPlainText("Alfa terminal", value)); }
            @Override public String getText() { return manager.hasPrimaryClip() && manager.getPrimaryClip() != null ? manager.getPrimaryClip().getItemAt(0).coerceToText(activity).toString() : null; }
        }, text)) {
            blocked("COPY", "clipboard tidak tersedia");
            return;
        }
        showStatus("COPIED=" + text.length() + " chars", AlfaUiTheme.READY);
        if (Build.VERSION.SDK_INT <= 32) Toast.makeText(activity, "Terminal output copied", Toast.LENGTH_SHORT).show();
    }

    private void pasteClipboard() {
        ClipboardManager manager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip() || terminal.mEmulator == null) {
            blocked("PASTE", "clipboard text belum tersedia");
            return;
        }
        ClipData data = manager.getPrimaryClip();
        if (data != null && data.getItemCount() > 0) {
            CharSequence value = data.getItemAt(0).coerceToText(activity);
            if (value != null) terminal.mEmulator.paste(value.toString());
        }
    }

    private void configureTerminal() {
        terminal.setFocusableInTouchMode(true);
        terminal.setBackgroundColor(TERMINAL);
        terminal.setTerminalViewClient(new AlfaTerminalViewClient(terminal, view -> {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }));
        terminal.setTextSize(fontSize);
        terminal.setTypeface(Typeface.MONOSPACE);
        applyRenderer();
        applyTheme(theme);
        applyCursorStyle(false);
    }

    private void applyRenderer() {
        terminal.mRenderer = new TerminalRenderer(fontSize, Typeface.MONOSPACE, lineHeight);
        terminal.updateSize();
        terminal.invalidate();
    }

    private void applyTheme(String value) {
        theme = value == null ? "obsidian" : value;
        Properties p = new Properties();
        if ("amber".equals(theme)) {
            p.setProperty("foreground", "#F59E0B");
            p.setProperty("background", "#0D1117");
            p.setProperty("cursor", "#F59E0B");
        } else if ("contrast".equals(theme)) {
            p.setProperty("foreground", "#FFFFFF");
            p.setProperty("background", "#000000");
            p.setProperty("cursor", "#FFFFFF");
        } else {
            theme = "obsidian";
            p.setProperty("foreground", "#F0F6FC");
            p.setProperty("background", "#0D1117");
            p.setProperty("cursor", "#10B981");
        }
        TerminalColors.COLOR_SCHEME.updateWith(p);
        if (terminal.mEmulator != null) terminal.mEmulator.mColors.reset();
        terminal.invalidate();
        preferences.edit().putString(PREF_THEME, theme).apply();
    }

    private void applyCursorStyle(boolean persist) {
        if (persist) preferences.edit().putInt(PREF_CURSOR, cursorStyle).apply();
        if (terminal.mTermSession != null) terminal.mTermSession.write("\033[" + cursorStyle + " q");
    }

    private void installInsets() {
        if (Build.VERSION.SDK_INT < 23) return;
        activity.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        View decor = activity.getWindow().getDecorView();
        decor.setOnApplyWindowInsetsListener((view, insets) -> {
            int ime = Build.VERSION.SDK_INT >= 30 ? insets.getInsets(WindowInsets.Type.ime()).bottom : 0;
            boolean visible = Build.VERSION.SDK_INT >= 30 && insets.isVisible(WindowInsets.Type.ime());
            bottomNav.setVisibility(visible ? View.GONE : View.VISIBLE);
            terminalCard.setPadding(0, 0, 0, visible ? ime : 0);
            accessory.setTranslationY(visible ? -ime : 0);
            return insets;
        });
        decor.requestApplyInsets();
    }

    private void installBackHandling() {
        if (Build.VERSION.SDK_INT < 33) return;
        backCallback = this::goBack;
        updateBackRegistration();
    }

    private void updateBackRegistration() {
        if (Build.VERSION.SDK_INT < 33 || backCallback == null) return;
        android.window.OnBackInvokedDispatcher dispatcher = activity.getOnBackInvokedDispatcher();
        if (screen != AlfaUiNavigation.Screen.TERMINAL && !backRegistered) {
            dispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
            backRegistered = true;
        } else if (screen == AlfaUiNavigation.Screen.TERMINAL && backRegistered) {
            dispatcher.unregisterOnBackInvokedCallback(backCallback);
            backRegistered = false;
        }
    }

    private void goBack() {
        if (screen == AlfaUiNavigation.Screen.TERMINAL) return;
        showScreen(AlfaUiNavigation.backFrom(screen, previousScreen), false);
    }

    private void showScreen(AlfaUiNavigation.Screen target) { showScreen(target, true); }

    private void showScreen(AlfaUiNavigation.Screen target, boolean rememberPrevious) {
        if (rememberPrevious && target != screen) previousScreen = screen;
        screen = target;
        hideKeyboard();
        panelHost.removeAllViews();
        if (target == AlfaUiNavigation.Screen.TERMINAL) {
            panelHost.setVisibility(View.GONE);
            terminalCard.setVisibility(View.VISIBLE);
        } else {
            terminalCard.setVisibility(View.GONE);
            panelHost.addView(buildScreen(target), new FrameLayout.LayoutParams(-1, -1));
            panelHost.setVisibility(View.VISIBLE);
        }
        updateBackRegistration();
    }

    private View buildScreen(AlfaUiNavigation.Screen target) {
        LinearLayout root = column(AlfaUiTheme.CANVAS);
        LinearLayout header = row(AlfaUiTheme.SURFACE_1);
        Button back = button("←", AlfaUiTheme.CYAN, v -> goBack());
        back.setContentDescription("Kembali ke terminal");
        header.addView(back, size48());
        TextView title = text(screenTitle(target), AlfaUiTheme.READY, 13, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(56)));
        if (target == AlfaUiNavigation.Screen.RUNTIME) root.addView(runtimePanel(), new LinearLayout.LayoutParams(-1, 0, 1));
        else if (target == AlfaUiNavigation.Screen.LANES) root.addView(lanesPanel(), new LinearLayout.LayoutParams(-1, 0, 1));
        else root.addView(text("State panel: " + target.name() + "\nUse the dedicated action overlay or Back to return to terminal.", AlfaUiTheme.TEXT_MUTED, 11, true), new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private String screenTitle(AlfaUiNavigation.Screen target) {
        switch (target) {
            case RUNTIME: return "LINUX RUNTIMES";
            case LANES: return "RUNTIME LANES";
            default: return target.name();
        }
    }

    private View runtimePanel() {
        LinearLayout root = column(AlfaUiTheme.CANVAS);
        TextView summary = text("", AlfaUiTheme.TEXT_MUTED, 10, true);
        summary.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.addView(summary, new LinearLayout.LayoutParams(-1, dp(40)));
        LinearLayout list = column(AlfaUiTheme.CANVAS);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        renderRuntimeList(list, summary);
        return root;
    }

    private void renderRuntimeList(LinearLayout list, TextView summary) {
        list.removeAllViews();
        List<RuntimeProfile> profiles = RuntimeRegistry.all();
        int ready = 0;
        for (RuntimeProfile profile : profiles) {
            RuntimeUiState.Status state = runtimeStatus(profile);
            if (state == RuntimeUiState.Status.READY) ready++;
            list.addView(runtimeCard(profile, state), new LinearLayout.LayoutParams(-1, dp(92)));
        }
        summary.setText("READY=" + ready + "/" + profiles.size() + "  •  canonical registry=" + RuntimeRegistry.CANONICAL_REGISTRY_SHA256.substring(0, 12) + "…");
    }

    private View runtimeCard(RuntimeProfile profile, RuntimeUiState.Status state) {
        LinearLayout card = row(AlfaUiTheme.SURFACE_1);
        card.setPadding(dp(10), dp(5), dp(5), dp(5));
        TextView name = text(profile.displayName() + "\n" + profile.id(), AlfaUiTheme.TEXT, 11, true);
        card.addView(name, new LinearLayout.LayoutParams(0, -1, 1));
        TextView status = text(RuntimeUiState.label(state), state == RuntimeUiState.Status.READY ? AlfaUiTheme.READY : AlfaUiTheme.TEXT_MUTED, 9, true);
        status.setGravity(Gravity.CENTER);
        card.addView(status, new LinearLayout.LayoutParams(dp(82), dp(72)));
        Button verify = button("VERIFY", AlfaUiTheme.TEXT_MUTED, v -> refreshCurrentScreen());
        card.addView(verify, new LinearLayout.LayoutParams(dp(70), dp(48)));
        if (RuntimeUiState.canOpen(state)) {
            Button open = button("OPEN", CYAN, v -> { selectRuntime(profile.id()); invokeHost("startSession"); showScreen(AlfaUiNavigation.Screen.TERMINAL); });
            card.addView(open, new LinearLayout.LayoutParams(dp(68), dp(48)));
        } else {
            Button install = button("INSTALL", CYAN, v -> { selectRuntime(profile.id()); invokeHost("installRuntime"); refreshCurrentScreen(); });
            install.setEnabled(state != RuntimeUiState.Status.VERIFYING);
            card.addView(install, new LinearLayout.LayoutParams(dp(72), dp(48)));
        }
        return card;
    }

    private View lanesPanel() {
        LinearLayout root = column(AlfaUiTheme.CANVAS);
        TextView output = text("LANES_STATUS=WAITING", AlfaUiTheme.TEXT, 10, true);
        output.setGravity(Gravity.TOP | Gravity.START);
        output.setPadding(dp(12), dp(12), dp(12), dp(12));
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(output);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Button probe = button("RUN RUNTIME TOOL PROBE", CYAN, v -> {
            RuntimeSessionManager manager = sessionManager();
            if (manager == null || !manager.isPromptReady()) {
                output.setText("LANES_STATUS=BLOCKED\nREASON=runtime PTY prompt is not ready");
                return;
            }
            manager.runRuntimeCommand("printf 'LANES_EVIDENCE=PASS\\n'; command -v sh; command -v ps; command -v env", (result, code) -> main.post(() -> output.setText("EXIT=" + code + "\n" + result)));
        });
        root.addView(probe, new LinearLayout.LayoutParams(-1, dp(56)));
        return root;
    }

    private RuntimeUiState.Status runtimeStatus(RuntimeProfile profile) {
        File vault = new File(activity.getFilesDir(), "runtime-vault");
        File runtime = new File(new File(vault, "runtimes"), profile.id());
        File ready = new File(runtime, "READY.evidence");
        File rootfs = new File(runtime, "rootfs");
        File engine = new File(activity.getApplicationInfo().nativeLibraryDir, "libproot.so");
        return RuntimeUiState.resolve(profile.id(), runtime, ready, engine, rootfs);
    }

    private void selectRuntime(String runtimeId) { invokeHost("selectRuntime", new Class[]{String.class}, runtimeId); }

    private RuntimeSessionManager sessionManager() {
        try {
            Field field = MainActivity.class.getDeclaredField("sessionManager");
            field.setAccessible(true);
            return (RuntimeSessionManager) field.get(activity);
        } catch (Exception ignored) { return null; }
    }

    private void refreshCurrentScreen() {
        if (screen != AlfaUiNavigation.Screen.TERMINAL) showScreen(screen, false);
    }

    private void startStateRefresh() {
        main.post(new Runnable() {
            @Override public void run() {
                updateRuntimeMeta();
                if (screen == AlfaUiNavigation.Screen.RUNTIME) {
                    String signature = runtimeSignature();
                    if (!signature.equals(lastRuntimeSignature)) {
                        lastRuntimeSignature = signature;
                        refreshCurrentScreen();
                    }
                }
                main.postDelayed(this, 1000L);
            }
        });
    }

    private String runtimeSignature() {
        StringBuilder s = new StringBuilder();
        for (RuntimeProfile profile : RuntimeRegistry.all()) s.append(profile.id()).append('=').append(runtimeStatus(profile).name()).append(';');
        return s.toString();
    }

    private void updateRuntimeMeta() {
        RuntimeProfile profile = selectedProfile();
        RuntimeSessionManager manager = sessionManager();
        String name = profile == null ? "UNKNOWN" : profile.displayName();
        String state = manager == null ? "WAITING" : manager.isPromptReady() ? "READY" : manager.isRunning() ? "PTY" : "WAITING";
        runtimeMeta.setText("RUNTIME: " + name + "  •  PTY: " + state);
    }

    private RuntimeProfile selectedProfile() { return RuntimeSelection.profile(preferences.getString("runtime_id", RuntimeSelection.DEFAULT_RUNTIME_ID)); }

    private void showAppearanceSettings() {
        List<Button> fontButtons = new ArrayList<>();
        List<Button> lineButtons = new ArrayList<>();
        List<Button> cursorButtons = new ArrayList<>();
        List<Button> themeButtons = new ArrayList<>();
        LinearLayout box = column(AlfaUiTheme.SURFACE_1);
        box.setPadding(dp(16), dp(10), dp(16), dp(10));
        box.addView(text("TEXT & FONT SCALING", AlfaUiTheme.TEXT, 12, true));
        addChoiceRow(box, fontButtons, "11sp", 11); addChoiceRow(box, fontButtons, "13sp", 13); addChoiceRow(box, fontButtons, "15sp", 15);
        box.addView(text("LINE HEIGHT", AlfaUiTheme.TEXT, 12, true));
        addLineChoice(box, lineButtons, "1.0x", 1.0f); addLineChoice(box, lineButtons, "1.25x", 1.25f); addLineChoice(box, lineButtons, "1.5x", 1.5f);
        box.addView(text("TERMINAL PTY FONT", AlfaUiTheme.TEXT, 12, true));
        box.addView(text("JetBrains Mono / system monospace fallback", AlfaUiTheme.TEXT_MUTED, 11, false));
        box.addView(text("CURSOR", AlfaUiTheme.TEXT, 12, true));
        addCursorChoice(box, cursorButtons, "Block", 2); addCursorChoice(box, cursorButtons, "Underline", 4); addCursorChoice(box, cursorButtons, "Bar", 6);
        box.addView(text("THEME & COLOR PRESETS", AlfaUiTheme.TEXT, 12, true));
        addThemeChoice(box, themeButtons, "Terminal Obsidian", "obsidian", AlfaUiTheme.READY); addThemeChoice(box, themeButtons, "Classic Terminal Amber", "amber", AlfaUiTheme.WARNING); addThemeChoice(box, themeButtons, "High Contrast", "contrast", AlfaUiTheme.TEXT);
        markSelected(fontButtons, Integer.toString(fontSize)); markSelected(lineButtons, Float.toString(lineHeight) + "x"); markSelected(cursorButtons, cursorStyle == 2 ? "Block" : cursorStyle == 4 ? "Underline" : "Bar"); markSelected(themeButtons, theme);
        ScrollView scroll = new ScrollView(activity); scroll.addView(box);
        new AlertDialog.Builder(activity).setTitle("Terminal Appearance").setView(scroll).setPositiveButton("DONE", null).show();
    }

    private void addChoiceRow(LinearLayout box, List<Button> group, String label, int size) { Button b = button(label, size == fontSize ? AlfaUiTheme.READY : AlfaUiTheme.TEXT, v -> { fontSize = size; preferences.edit().putInt(PREF_FONT, size).apply(); applyRenderer(); markSelected(group, label); }); group.add(b); box.addView(b, full48()); }
    private void addLineChoice(LinearLayout box, List<Button> group, String label, float value) { Button b = button(label, value == lineHeight ? AlfaUiTheme.READY : AlfaUiTheme.TEXT, v -> { lineHeight = value; preferences.edit().putFloat(PREF_LINE_HEIGHT, value).apply(); applyRenderer(); markSelected(group, label); }); group.add(b); box.addView(b, full48()); }
    private void addCursorChoice(LinearLayout box, List<Button> group, String label, int sequenceStyle) { Button b = button(label, sequenceStyle == cursorStyle ? AlfaUiTheme.READY : AlfaUiTheme.TEXT, v -> { cursorStyle = sequenceStyle; applyCursorStyle(true); markSelected(group, label); }); group.add(b); box.addView(b, full48()); }
    private void addThemeChoice(LinearLayout box, List<Button> group, String label, String value, int color) { Button b = button(label, value.equals(theme) ? AlfaUiTheme.READY : color, v -> { applyTheme(value); markSelected(group, value); }); b.setTag(value); group.add(b); box.addView(b, full48()); }

    private void markSelected(List<Button> group, String selected) { for (Button b : group) { String value = b.getTag() == null ? b.getText().toString() : String.valueOf(b.getTag()); b.setTextColor(value.equals(selected) ? AlfaUiTheme.READY : AlfaUiTheme.TEXT); } }

    private void showStatus(String text, int color) { panelStatus.setText(text); panelStatus.setTextColor(color); panelStatus.setVisibility(View.VISIBLE); }
    private void blocked(String operation, String reason) { showStatus(operation + "=BLOCKED\n" + reason, AlfaUiTheme.WARNING); }

    private void setFullscreen(boolean value) { fullscreen = value; runtimeMeta.setVisibility(value ? View.GONE : View.VISIBLE); accessory.setVisibility(value ? View.GONE : View.VISIBLE); bottomNav.setVisibility(value ? View.GONE : View.VISIBLE); activity.getWindow().getDecorView().setSystemUiVisibility(value ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION : 0); }
    private void hideKeyboard() { View target = activity.getCurrentFocus(); if (target == null) target = terminal; InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE); if (imm != null) imm.hideSoftInputFromWindow(target.getWindowToken(), 0); }
    private void invokeHost(String methodName) { invokeHost(methodName, new Class[0]); }
    private void invokeHost(String methodName, Class<?>[] types, Object... args) { try { Method method = MainActivity.class.getDeclaredMethod(methodName, types); method.setAccessible(true); method.invoke(activity, args); } catch (Exception error) { blocked(methodName, error.getClass().getSimpleName() + ":" + error.getMessage()); } }

    private void addHeaderAction(LinearLayout bar, String value, String description, View.OnClickListener listener) { Button b = button(value, AlfaUiTheme.TEXT, listener); b.setContentDescription(description); bar.addView(b, size48()); }
    private void addNav(LinearLayout bar, String value, View.OnClickListener listener) { Button b = button(value, AlfaUiTheme.TEXT_MUTED, listener); b.setContentDescription(value); bar.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1)); }
    private LinearLayout card() { LinearLayout l = column(TERMINAL); l.setBackground(round(AlfaUiTheme.SURFACE_1, 12)); return l; }
    private LinearLayout row(int color) { LinearLayout l = new LinearLayout(activity); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); l.setBackgroundColor(color); return l; }
    private LinearLayout column(int color) { LinearLayout l = new LinearLayout(activity); l.setOrientation(LinearLayout.VERTICAL); l.setBackgroundColor(color); return l; }
    private TextView text(String value, int color, int sp, boolean mono) { TextView t = new TextView(activity); t.setText(value); t.setTextColor(color); t.setTextSize(sp); t.setGravity(Gravity.CENTER_VERTICAL); if (mono) t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); return t; }
    private Button button(String value, int color, View.OnClickListener listener) { Button b = new Button(activity); b.setText(value); b.setTextColor(color); b.setTextSize(10); b.setAllCaps(false); b.setMinHeight(dp(48)); b.setMinWidth(dp(48)); b.setPadding(dp(5), 0, dp(5), 0); b.setBackground(round(AlfaUiTheme.SURFACE_2, 5)); b.setOnClickListener(listener); return b; }
    private LinearLayout.LayoutParams full48() { return new LinearLayout.LayoutParams(-1, dp(48)); }
    private LinearLayout.LayoutParams size48() { return new LinearLayout.LayoutParams(dp(48), dp(48)); }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private static void detach(View view) { if (view == null) return; ViewParent parent = view.getParent(); if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(view); }
    private static TerminalView findTerminal(View root) { if (root instanceof TerminalView) return (TerminalView) root; if (root instanceof ViewGroup) { ViewGroup group = (ViewGroup) root; for (int i = 0; i < group.getChildCount(); i++) { TerminalView result = findTerminal(group.getChildAt(i)); if (result != null) return result; } } return null; }
}