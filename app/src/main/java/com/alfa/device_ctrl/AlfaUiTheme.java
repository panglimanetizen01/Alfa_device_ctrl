package com.alfa.device_ctrl;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Presentation-only Terminal Obsidian styling derived from the supplied reference corpus. */
public final class AlfaUiTheme {
    public static final int OBSIDIAN_CANVAS = 0xFF101419;
    public static final int OBSIDIAN_SURFACE = 0xFF161B22;
    public static final int OBSIDIAN_SURFACE_ACTIVE = 0xFF1C2229;
    public static final int OBSIDIAN_BORDER = 0xFF22272E;
    public static final int OBSIDIAN_BORDER_FOCUSED = 0xFF374151;
    public static final int OBSIDIAN_TEXT = 0xFFF0F6FC;
    public static final int OBSIDIAN_TEXT_MUTED = 0xFF8B949E;
    public static final int OBSIDIAN_READY = 0xFF10B981;
    public static final int OBSIDIAN_VERIFYING = 0xFFF59E0B;
    public static final int OBSIDIAN_ERROR = 0xFFEF4444;
    public static final int OBSIDIAN_TELEMETRY = 0xFF38BDF8;
    public static final int OBSIDIAN_UNKNOWN = 0xFF64748B;
    public static final int TOUCH_TARGET_DP = 48;
    public static final int CANVAS = OBSIDIAN_CANVAS;
    public static final int SURFACE_1 = OBSIDIAN_SURFACE;
    public static final int SURFACE_2 = OBSIDIAN_SURFACE_ACTIVE;
    public static final int BORDER = OBSIDIAN_BORDER;
    public static final int BORDER_FOCUSED = OBSIDIAN_BORDER_FOCUSED;
    public static final int TEXT = OBSIDIAN_TEXT;
    public static final int TEXT_MUTED = OBSIDIAN_TEXT_MUTED;
    public static final int READY = OBSIDIAN_READY;
    public static final int WARNING = OBSIDIAN_VERIFYING;
    public static final int ERROR = OBSIDIAN_ERROR;
    public static final int TELEMETRY = OBSIDIAN_TELEMETRY;
    public static final int CYAN = OBSIDIAN_TELEMETRY;
    public static final int UNKNOWN = OBSIDIAN_UNKNOWN;
    private static final int LEGACY_PRIMARY = 0xFFABC7FF;
    private static final int LEGACY_MUTED = 0xFFC1C6D5;
    private static final int LEGACY_ERROR = 0xFFFFB4AB;
    private static final int LEGACY_CANVAS = 0xFF131315;
    private static final int LEGACY_SURFACE = 0xFF201F21;
    private static final int LEGACY_SURFACE_HIGH = 0xFF2A2A2C;
    private static final int RADIUS_DP = 6;
    private AlfaUiTheme() { }
    public static int statusColor(String status) {
        if (status == null) return OBSIDIAN_UNKNOWN;
        String normalized = status.trim().toUpperCase();
        if ("READY".equals(normalized) || "ACTIVE".equals(normalized) || "ONLINE".equals(normalized)) return OBSIDIAN_READY;
        if ("VERIFYING".equals(normalized) || "WARN".equals(normalized) || "WARNING".equals(normalized) || "PARTIAL".equals(normalized)) return OBSIDIAN_VERIFYING;
        if ("FAILED".equals(normalized) || "HALT".equals(normalized) || "DENIED".equals(normalized) || "BLOCKED".equals(normalized)) return OBSIDIAN_ERROR;
        if ("TELEMETRY".equals(normalized) || "SOCKET".equals(normalized) || "INFO".equals(normalized)) return OBSIDIAN_TELEMETRY;
        return OBSIDIAN_UNKNOWN;
    }
    public static boolean isLegacyPalette(int color) { return color == LEGACY_PRIMARY || color == LEGACY_MUTED || color == LEGACY_ERROR; }
    public static void apply(Activity activity) {
        View root = activity.findViewById(android.R.id.content); if (root == null) return; root.setBackgroundColor(OBSIDIAN_CANVAS); float density = activity.getResources().getDisplayMetrics().density; applyTree(root, density); root.post(() -> adaptRuntimeDashboard(root, density));
    }
    private static void applyTree(View view, float density) {
        if (view == null || view.getClass().getName().contains("TerminalView")) return; int min = Math.round(TOUCH_TARGET_DP * density); if (view.isClickable() || view instanceof Button) { view.setMinimumHeight(Math.max(view.getMinimumHeight(), min)); view.setMinimumWidth(Math.max(view.getMinimumWidth(), min)); }
        if (view instanceof Button) styleButton((Button) view, density, min); else if (view instanceof TextView) styleText((TextView) view, min); else if (view instanceof ViewGroup) styleContainer(view, density);
        if (view instanceof ViewGroup) { ViewGroup group = (ViewGroup) view; for (int i = 0; i < group.getChildCount(); i++) applyTree(group.getChildAt(i), density); }
    }
    private static void styleButton(Button button, float density, int min) {
        int original = button.getTextColors() == null ? OBSIDIAN_TEXT : button.getTextColors().getDefaultColor(); String label = button.getText() == null ? "" : button.getText().toString().trim().toUpperCase(); int textColor = semanticTextColor(label, normalizeTextColor(original)); int accent = textColor == OBSIDIAN_ERROR ? OBSIDIAN_ERROR : OBSIDIAN_READY; boolean filled = "OPEN".equals(label) || label.startsWith("SYSTEM ONLINE");
        button.setTextSize(10); button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); button.setTextColor(filled ? OBSIDIAN_CANVAS : textColor); button.setMinimumHeight(Math.max(button.getMinimumHeight(), min)); button.setMinimumWidth(Math.max(button.getMinimumWidth(), min)); button.setPadding(dp(8, density), dp(4, density), dp(8, density), dp(4, density)); button.setAllCaps(false); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { button.setElevation(0f); button.setStateListAnimator(null); } button.setBackground(controlBackground(accent, density, filled));
    }
    private static void styleText(TextView text, int min) {
        int original = text.getCurrentTextColor(); String label = text.getText() == null ? "" : text.getText().toString().trim().toUpperCase(); text.setTextColor(semanticTextColor(label, normalizeTextColor(original))); if (text.isClickable()) text.setMinimumHeight(Math.max(text.getMinimumHeight(), min)); Typeface current = text.getTypeface(); if (current != null) text.setTypeface(Typeface.create(current, current.isBold() ? Typeface.BOLD : Typeface.NORMAL)); if (text.getTextSize() <= 13 * text.getResources().getDisplayMetrics().scaledDensity) text.setLetterSpacing(0.03f);
    }
    private static int semanticTextColor(String label, int fallback) { if ("READY".equals(label) || "ACTIVE".equals(label) || "ONLINE".equals(label)) return OBSIDIAN_READY; if ("VERIFYING".equals(label) || "WARN".equals(label) || "WARNING".equals(label) || "PARTIAL".equals(label)) return OBSIDIAN_VERIFYING; if ("FAILED".equals(label) || "HALT".equals(label) || "DENIED".equals(label) || "BLOCKED".equals(label)) return OBSIDIAN_ERROR; if ("TELEMETRY".equals(label) || "SOCKET".equals(label) || "INFO".equals(label)) return OBSIDIAN_TELEMETRY; return fallback; }
    private static void styleContainer(View view, float density) { if (!(view.getBackground() instanceof ColorDrawable)) return; int color = ((ColorDrawable) view.getBackground()).getColor(); int normalized = normalizeSurfaceColor(color); if (normalized == OBSIDIAN_CANVAS) view.setBackgroundColor(OBSIDIAN_CANVAS); else view.setBackground(surfaceBackground(normalized, density)); }
    private static int normalizeTextColor(int color) { if (color == LEGACY_PRIMARY) return OBSIDIAN_READY; if (color == LEGACY_ERROR) return OBSIDIAN_ERROR; if (color == LEGACY_MUTED) return OBSIDIAN_TEXT_MUTED; if (color == Color.TRANSPARENT) return OBSIDIAN_TEXT; return color; }
    private static int normalizeSurfaceColor(int color) { if (color == LEGACY_CANVAS) return OBSIDIAN_CANVAS; if (color == LEGACY_SURFACE) return OBSIDIAN_SURFACE; if (color == LEGACY_SURFACE_HIGH) return OBSIDIAN_SURFACE_ACTIVE; if (color == Color.BLACK) return 0xFF0A0E13; if ((color >>> 24) != 0xFF) return color; return color; }
    private static GradientDrawable surfaceBackground(int color, float density) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(RADIUS_DP, density)); drawable.setStroke(Math.max(1, dp(1, density)), OBSIDIAN_BORDER); return drawable; }
    private static GradientDrawable controlBackground(int accent, float density, boolean filled) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(filled ? accent : OBSIDIAN_SURFACE_ACTIVE); drawable.setCornerRadius(dp(4, density)); drawable.setStroke(Math.max(1, dp(1, density)), accent); return drawable; }
    private static void adaptRuntimeDashboard(View root, float density) { LinearLayout dashboard = findRuntimeDashboard(root); if (dashboard == null || dashboard.getHeight() <= 0) return; int runtimeCount = RuntimeRegistry.all().size(); if (runtimeCount <= 0) return; int padding = dashboard.getPaddingTop() + dashboard.getPaddingBottom(); int separatorHeight = Math.max(1, dp(3, density)); int separators = Math.max(0, runtimeCount - 1); int available = Math.max(0, dashboard.getHeight() - padding - separators * separatorHeight); int cardHeight = Math.max(dp(48, density), available / runtimeCount); int cards = 0; for (int i = 0; i < dashboard.getChildCount(); i++) { View child = dashboard.getChildAt(i); ViewGroup.LayoutParams lp = child.getLayoutParams(); if (!(lp instanceof LinearLayout.LayoutParams)) continue; LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp; if (child instanceof LinearLayout && cards < runtimeCount) { params.height = cardHeight; params.weight = 0; child.setLayoutParams(params); cards++; } else { params.height = separatorHeight; params.weight = 0; child.setLayoutParams(params); } } dashboard.requestLayout(); }
    private static LinearLayout findRuntimeDashboard(View root) { if (!(root instanceof ViewGroup)) return null; ViewGroup group = (ViewGroup) root; int expected = RuntimeRegistry.all().size(); if (expected > 0 && group.getChildCount() >= expected * 2 - 1) { int cards = 0; for (int i = 0; i < group.getChildCount(); i += 2) if (group.getChildAt(i) instanceof LinearLayout) cards++; if (cards == expected) return (LinearLayout) group; } for (int i = 0; i < group.getChildCount(); i++) { LinearLayout found = findRuntimeDashboard(group.getChildAt(i)); if (found != null) return found; } return null; }
    private static int dp(int value, float density) { return Math.round(value * density); }
}
