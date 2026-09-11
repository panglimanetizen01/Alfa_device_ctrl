package com.alfa.device_ctrl;

/** Pure native-Views window policy based on the actual app window, not physical device type. */
public final class AdaptiveWindowPolicy {
    public enum WidthClass { COMPACT, MEDIUM, EXPANDED }
    public enum HeightClass { COMPACT, MEDIUM, EXPANDED }

    private AdaptiveWindowPolicy() { }

    public static WidthClass widthClass(int widthDp) {
        if (widthDp < 600) return WidthClass.COMPACT;
        if (widthDp < 840) return WidthClass.MEDIUM;
        return WidthClass.EXPANDED;
    }

    public static HeightClass heightClass(int heightDp) {
        if (heightDp < 480) return HeightClass.COMPACT;
        if (heightDp < 900) return HeightClass.MEDIUM;
        return HeightClass.EXPANDED;
    }

    public static boolean useTwoPane(int widthDp) {
        return widthClass(widthDp) != WidthClass.COMPACT;
    }

    public static boolean usePersistentNavigation(int widthDp) {
        return widthClass(widthDp) == WidthClass.EXPANDED;
    }

    public static int navigationWidthDp(int widthDp) {
        if (usePersistentNavigation(widthDp)) return 280;
        if (useTwoPane(widthDp)) return 96;
        return 0;
    }
}
