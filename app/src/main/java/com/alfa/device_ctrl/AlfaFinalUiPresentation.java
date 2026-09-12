package com.alfa.device_ctrl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.termux.terminal.TerminalEmulator;
import com.termux.view.TerminalView;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Native presentation layer matching the approved Stitch Terminal Obsidian interaction model. */
public final class AlfaFinalUiPresentation {
    static final int TAG_ID = 0xA1FA001;
    public static final List<Integer> FONT_SIZES_SP = Arrays.asList(11, 13, 15);
    public static final List<Float> LINE_HEIGHTS = Arrays.asList(1.0f, 1.25f, 1.5f);

    private final Activity activity;
    private final TerminalView terminal;
    private final TextView legacyStatus;
    private final View runtimePanel;
    private final View monitorPanel;
    private final Button htopAction;
    private final Button networkAction;
    private final Button runtimeToolsAction;
    private final View oldRuntimeTab;
    private final View oldTerminalTab;
    private final View oldRuntimePlus;
    private final FrameLayout content;
    private final FrameLayout panelHost;
    private final LinearLayout terminalCard;
    private final LinearLayout accessory;
    private final LinearLayout bottomNav;
    private final TextView runtimeMeta;
    private int fontSize = 13;
    private float lineHeight = 1.25f;
    private boolean fullscreen;
    private boolean ctrlLatch;

    private AlfaFinalUiPresentation(Activity activity, FrameLayout content, TerminalView terminal,
                                   TextView legacyStatus, View runtimePanel, View monitorPanel,
                                   Button htopAction, Button networkAction, Button runtimeToolsAction,
                                   View oldRuntimeTab, View oldTerminalTab, View oldRuntimePlus) {
        this.activity = activity;
        this.content = content;
        this.terminal = terminal;
        this.legacyStatus = legacyStatus;
        this.runtimePanel = runtimePanel;
        this.monitorPanel = monitorPanel;
        this.htopAction = htopAction;
        this.networkAction = networkAction;
        this.runtimeToolsAction = runtimeToolsAction;
        this.oldRuntimeTab = oldRuntimeTab;
        this.oldTerminalTab = oldTerminalTab;
        this.oldRuntimePlus = oldRuntimePlus;
        this.panelHost = new FrameLayout(activity);
        this.terminalCard = card();
        this.accessory = row(AlfaUiTheme.SURFACE_1);
        this.bottomNav = row(AlfaUiTheme.CANVAS);
        this.runtimeMeta = text("RUNTIME: UNKNOWN  •  PTY: WAITING", AlfaUiTheme.TEXT_MUTED, 11, true);
    }

    public static void apply(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        View raw = activity.findViewById(android.R.id.content);
        if (!(raw instanceof FrameLayout)) return;
        FrameLayout content = (FrameLayout) raw;
        if (content.getTag(TAG_ID) instanceof AlfaFinalUiPresentation) return;
        TerminalView terminal = findTerminal(content);
        if (terminal == null) return;
        TextView status = findTerminalStatus(terminal);
        View runtimePanel = findTextAncestor(content, "LINUX RUNTIMES");
        View monitorPanel = findTextAncestor(content, "htop / telemetry");
        Button htop = findButton(content, "htop");
        Button network = findButton(content, "jaringan");
        Button runtimeTools = findButton(content, "⌘");
        View runtimeTab = findViewWithText(content, "runtime");
        View terminalTab = findViewWithText(content, "terminal");
        View plus = findButton(content, "+");
        if (status == null) status = new TextView(activity);
        detach(terminal);
        detach(status);
        detach(runtimePanel);
        detach(monitorPanel);
        AlfaFinalUiPresentation ui = new AlfaFinalUiPresentation(activity, content, terminal, status,
                runtimePanel, monitorPanel, htop, network, runtimeTools, runtimeTab, terminalTab, plus);
        content.removeAllViews();
        content.setBackgroundColor(AlfaUiTheme.CANVAS);
        content.addView(ui.build());
        content.setTag(TAG_ID, ui);
        ui.configureTerminal();
        ui.installInsets();
    }

    private View build() {
        LinearLayout root = column(AlfaUiTheme.CANVAS);
        root.setPadding(dp(12), dp(8), dp(12), 0);
        root.addView(header(), new LinearLayout.LayoutParams(-1, dp(60)));
        root.addView(runtimeMeta(), new LinearLayout.LayoutParams(-1, dp(44)));
        FrameLayout body = new FrameLayout(activity);
        body.setClipChildren(false);
        terminalCard.addView(terminalHeader(), new LinearLayout.LayoutParams(-1, dp(48)));
        FrameLayout terminalFrame = new FrameLayout(activity);
        terminalFrame.setBackgroundColor(AlfaUiTheme.OBSIDIAN_CANVAS);
        terminalFrame.addView(terminal, new FrameLayout.LayoutParams(-1, -1));
        terminalFrame.addView(legacyStatus, overlayParams());
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
        Button runtime = button("RUNTIME", AlfaUiTheme.CYAN, v -> showPanel(runtimePanel));
        runtime.setContentDescription("Buka Linux runtime dashboard");
        bar.addView(runtime, new LinearLayout.LayoutParams(dp(92), dp(48)));
        return bar;
    }

    private View terminalHeader() {
        LinearLayout bar = row(AlfaUiTheme.SURFACE_2);
        TextView title = text("UBUNTU 24.04.4 ARM64  •  /dev/pts/0", AlfaUiTheme.TEXT, 11, true);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
        addHeaderAction(bar, "—", "Minimize terminal", v -> showPanel(runtimePanel));
        addHeaderAction(bar, "↗", "Fullscreen terminal", v -> setFullscreen(!fullscreen));
        addHeaderAction(bar, "□", "Split terminal (requires independent session)", v -> blocked("SPLIT", "independent second PTY evidence belum tersedia"));
        addHeaderAction(bar, "×", "Hentikan sesi terminal", v -> oldTerminalTabClick());
        return bar;
    }

    private View accessoryBar() {
        accessory.setPadding(dp(4), dp(4), dp(4), dp(4));
        String[] labels = {"ESC", "TAB →|", "CTRL", "ALT", "^C", "|", "/", "-", "↑", "↓", "PASTE"};
        for (String label : labels) {
            Button b = button(label, "CTRL".equals(label) && ctrlLatch ? AlfaUiTheme.CANVAS : AlfaUiTheme.TEXT, v -> accessoryAction(label));
            b.setContentDescription("PTY modifier " + label);
            accessory.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        return accessory;
    }

    private View bottomBar() {
        bottomNav.setGravity(Gravity.CENTER);
        addNav(bottomNav, "Nodes", v -> showPanel(runtimePanel));
        addNav(bottomNav, "Alfa RF", v -> click(networkAction));
        addNav(bottomNav, "Lanes", v -> click(runtimeToolsAction));
        addNav(bottomNav, "Diagnostics", v -> click(htopAction));
        return bottomNav;
    }

    private void accessoryAction(String label) {
        if (terminal.mEmulator == null) return;
        if ("ESC".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, 27, false, false);
        else if ("TAB →|".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, 9, false, false);
        else if ("CTRL".equals(label)) ctrlLatch = !ctrlLatch;
        else if ("ALT".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, 27, false, true);
        else if ("^C".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, 3, false, false);
        else if ("|".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, '|', ctrlLatch, false);
        else if ("/".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, '/', ctrlLatch, false);
        else if ("-".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, '-', ctrlLatch, false);
        else if ("↑".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, 0x1b, false, false);
        else if ("↓".equals(label)) terminal.inputCodePoint(TerminalView.KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD, 0x1b, false, false);
        else if ("PASTE".equals(label)) pasteClipboard();
        if (!"CTRL".equals(label)) ctrlLatch = false;
    }

    private void pasteClipboard() {
        ClipboardManager manager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip()) return;
        ClipData data = manager.getPrimaryClip();
        if (data != null && data.getItemCount() > 0 && terminal.mEmulator != null) {
            CharSequence text = data.getItemAt(0).coerceToText(activity);
            if (text != null) terminal.mEmulator.paste(text.toString());
        }
    }

    private void configureTerminal() {
        terminal.setFocusableInTouchMode(true);
        terminal.setBackgroundColor(AlfaUiTheme.TERMINAL);
        terminal.setTerminalViewClient(new AlfaTerminalViewClient(terminal, view -> {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }));
        terminal.setTextSize(fontSize);
        terminal.setTypeface(Typeface.MONOSPACE);
        applyTerminalPalette();
    }

    private void applyTerminalPalette() {
        Properties p = new Properties();
        p.setProperty("foreground", "#F0F6FC");
        p.setProperty("background", "#0D1117");
        p.setProperty("cursor", "#10B981");
        terminal.applyColorScheme(p);
    }

    private void installInsets() {
        if (Build.VERSION.SDK_INT < 23) return;
        activity.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        View decor = activity.getWindow().getDecorView();
        decor.setOnApplyWindowInsetsListener((view, insets) -> {
            int ime = 0;
            if (Build.VERSION.SDK_INT >= 30) ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
            boolean visible = Build.VERSION.SDK_INT >= 30 && insets.isVisible(WindowInsets.Type.ime());
            bottomNav.setVisibility(visible ? View.GONE : View.VISIBLE);
            terminalCard.setPadding(0, 0, 0, visible ? ime : 0);
            accessory.setTranslationY(visible ? -ime : 0);
            return insets;
        });
        decor.requestApplyInsets();
    }

    private void showAppearanceSettings() {
        LinearLayout box = column(AlfaUiTheme.SURFACE_1);
        box.setPadding(dp(16), dp(10), dp(16), dp(10));
        box.addView(text("TEXT & FONT SCALING", AlfaUiTheme.TEXT, 12, true));
        addChoiceRow(box, "11sp", 11); addChoiceRow(box, "13sp", 13); addChoiceRow(box, "15sp", 15);
        box.addView(text("LINE HEIGHT", AlfaUiTheme.TEXT, 12, true));
        addLineChoice(box, "1.0x", 1.0f); addLineChoice(box, "1.25x", 1.25f); addLineChoice(box, "1.5x", 1.5f);
        box.addView(text("TERMINAL PTY FONT", AlfaUiTheme.TEXT, 12, true));
        box.addView(text("JetBrains Mono / system monospace fallback", AlfaUiTheme.TEXT_MUTED, 11, false));
        box.addView(text("THEME & COLOR PRESETS", AlfaUiTheme.TEXT, 12, true));
        Button obsidian = button("Terminal Obsidian", AlfaUiTheme.READY, v -> applyTerminalPalette());
        box.addView(obsidian, full48());
        Button amber = button("Classic Terminal Amber", AlfaUiTheme.WARNING, v -> applyAmberPalette());
        box.addView(amber, full48());
        Button high = button("High Contrast", AlfaUiTheme.TEXT, v -> applyHighContrastPalette());
        box.addView(high, full48());
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Terminal Appearance").setView(box).setPositiveButton("DONE", null).create();
        dialog.show();
    }

    private void addChoiceRow(LinearLayout box, String label, int size) {
        Button b = button(label, size == fontSize ? AlfaUiTheme.READY : AlfaUiTheme.TEXT, v -> { fontSize = size; terminal.setTextSize(size); });
        box.addView(b, full48());
    }

    private void addLineChoice(LinearLayout box, String label, float value) {
        Button b = button(label, value == lineHeight ? AlfaUiTheme.READY : AlfaUiTheme.TEXT, v -> { lineHeight = value; blocked("LINE_HEIGHT", "renderer line-height multiplier will be applied by the terminal renderer contract"); });
        box.addView(b, full48());
    }

    private void applyAmberPalette() {
        Properties p = new Properties(); p.setProperty("foreground", "#F59E0B"); p.setProperty("background", "#0D1117"); p.setProperty("cursor", "#F59E0B"); terminal.applyColorScheme(p);
    }

    private void applyHighContrastPalette() {
        Properties p = new Properties(); p.setProperty("foreground", "#FFFFFF"); p.setProperty("background", "#000000"); p.setProperty("cursor", "#FFFFFF"); terminal.applyColorScheme(p);
    }

    private void showPanel(View panel) {
        if (panel == null) { blocked("PANEL", "panel source belum tersedia"); return; }
        panelHost.removeAllViews();
        detach(panel);
        panelHost.addView(panel, new FrameLayout.LayoutParams(-1, -1));
        panelHost.setVisibility(View.VISIBLE);
    }

    private void blocked(String operation, String reason) {
        if (legacyStatus == null) return;
        legacyStatus.setVisibility(View.VISIBLE);
        legacyStatus.setText(operation + "=BLOCKED\n" + reason);
        legacyStatus.setTextColor(AlfaUiTheme.WARNING);
    }

    private void setFullscreen(boolean value) {
        fullscreen = value;
        runtimeMeta.setVisibility(value ? View.GONE : View.VISIBLE);
        accessory.setVisibility(value ? View.GONE : View.VISIBLE);
        bottomNav.setVisibility(value ? View.GONE : View.VISIBLE);
        if (value) activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        else activity.getWindow().getDecorView().setSystemUiVisibility(0);
    }

    private void oldTerminalTabClick() { click(oldTerminalTab); }
    private static void click(View view) { if (view != null) view.performClick(); }

    private static FrameLayout.LayoutParams overlayParams() { return new FrameLayout.LayoutParams(-1, -1); }
    private LinearLayout terminalCard() { return card(); }
    private LinearLayout card() { LinearLayout l = column(AlfaUiTheme.TERMINAL); l.setBackground(round(AlfaUiTheme.SURFACE_1, 12)); return l; }
    private LinearLayout row(int color) { LinearLayout l = new LinearLayout(activity); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); l.setBackgroundColor(color); return l; }
    private LinearLayout column(int color) { LinearLayout l = new LinearLayout(activity); l.setOrientation(LinearLayout.VERTICAL); l.setBackgroundColor(color); return l; }
    private TextView text(String value, int color, int sp, boolean mono) { TextView t = new TextView(activity); t.setText(value); t.setTextColor(color); t.setTextSize(sp); t.setGravity(Gravity.CENTER_VERTICAL); if (mono) t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); return t; }
    private Button button(String value, int color, View.OnClickListener listener) { Button b = new Button(activity); b.setText(value); b.setTextColor(color); b.setTextSize(10); b.setAllCaps(false); b.setMinHeight(dp(48)); b.setMinWidth(dp(48)); b.setPadding(dp(5), 0, dp(5), 0); b.setBackground(round(AlfaUiTheme.SURFACE_2, 5)); b.setOnClickListener(listener); return b; }
    private void addHeaderAction(LinearLayout bar, String value, String description, View.OnClickListener listener) { Button b = button(value, AlfaUiTheme.TEXT, listener); b.setContentDescription(description); bar.addView(b, size48()); }
    private void addNav(LinearLayout bar, String value, View.OnClickListener listener) { Button b = button(value, AlfaUiTheme.TEXT_MUTED, listener); b.setContentDescription(value); bar.addView(b, new LinearLayout.LayoutParams(0, dp(56), 1)); }
    private LinearLayout.LayoutParams size48() { return new LinearLayout.LayoutParams(dp(48), dp(48)); }
    private LinearLayout.LayoutParams full48() { return new LinearLayout.LayoutParams(-1, dp(48)); }
    private LinearLayout.LayoutParams overlayParams2() { return new LinearLayout.LayoutParams(-1, -1); }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(1), AlfaUiTheme.BORDER); return d; }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }

    private static void detach(View view) { if (view == null) return; if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view); }
    private static TerminalView findTerminal(View root) { if (root instanceof TerminalView) return (TerminalView) root; if (root instanceof ViewGroup) { ViewGroup g = (ViewGroup) root; for (int i = 0; i < g.getChildCount(); i++) { TerminalView v = findTerminal(g.getChildAt(i)); if (v != null) return v; } } return null; }
    private static TextView findTerminalStatus(TerminalView terminal) { if (!(terminal.getParent() instanceof ViewGroup)) return null; ViewGroup p = (ViewGroup) terminal.getParent(); for (int i = 0; i < p.getChildCount(); i++) { View v = p.getChildAt(i); if (v instanceof TextView) return (TextView) v; } return null; }
    private static Button findButton(View root, String text) { View v = findViewWithText(root, text); return v instanceof Button ? (Button) v : null; }
    private static View findViewWithText(View root, String text) { if (root instanceof TextView && text.contentEquals(((TextView) root).getText())) return root; if (root instanceof ViewGroup) { ViewGroup g = (ViewGroup) root; for (int i = 0; i < g.getChildCount(); i++) { View v = findViewWithText(g.getChildAt(i), text); if (v != null) return v; } } return null; }
    private static View findTextAncestor(View root, String text) { View match = findViewWithText(root, text); if (match == null) return null; View current = match; for (int i = 0; i < 4 && current.getParent() instanceof View; i++) { current = (View) current.getParent(); if (current instanceof LinearLayout && current.getParent() instanceof ViewGroup) return current; } return match.getParent() instanceof View ? (View) match.getParent() : match; }
}
