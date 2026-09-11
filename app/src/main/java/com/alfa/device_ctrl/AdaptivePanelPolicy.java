package com.alfa.device_ctrl;

/** Pure allocation policy for the three existing vertical shell panes. */
public final class AdaptivePanelPolicy {
    private AdaptivePanelPolicy() { }

    public static float runtimeWeight(AdaptiveWindowPolicy.WidthClass width, AdaptiveWindowPolicy.HeightClass height) {
        if (height == AdaptiveWindowPolicy.HeightClass.COMPACT) return 0.22f;
        if (width == AdaptiveWindowPolicy.WidthClass.EXPANDED) return 0.24f;
        return 0.26f;
    }

    public static float monitorWeight(AdaptiveWindowPolicy.WidthClass width, AdaptiveWindowPolicy.HeightClass height) {
        if (height == AdaptiveWindowPolicy.HeightClass.COMPACT) return 0.14f;
        if (width == AdaptiveWindowPolicy.WidthClass.EXPANDED) return 0.16f;
        return 0.18f;
    }
}
