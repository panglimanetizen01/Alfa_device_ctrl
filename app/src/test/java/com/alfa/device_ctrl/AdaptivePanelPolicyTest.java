package com.alfa.device_ctrl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class AdaptivePanelPolicyTest {
    @Test public void compactHeightReducesFixedPaneAllocation() {
        float runtime = AdaptivePanelPolicy.runtimeWeight(AdaptiveWindowPolicy.WidthClass.COMPACT, AdaptiveWindowPolicy.HeightClass.COMPACT);
        float monitor = AdaptivePanelPolicy.monitorWeight(AdaptiveWindowPolicy.WidthClass.COMPACT, AdaptiveWindowPolicy.HeightClass.COMPACT);
        assertEquals(0.22f, runtime, 0.0001f);
        assertEquals(0.14f, monitor, 0.0001f);
        assertTrue(runtime + monitor < 1f);
    }

    @Test public void expandedWidthGetsStableShellAllocation() {
        assertEquals(0.24f, AdaptivePanelPolicy.runtimeWeight(AdaptiveWindowPolicy.WidthClass.EXPANDED, AdaptiveWindowPolicy.HeightClass.EXPANDED), 0.0001f);
        assertEquals(0.16f, AdaptivePanelPolicy.monitorWeight(AdaptiveWindowPolicy.WidthClass.EXPANDED, AdaptiveWindowPolicy.HeightClass.EXPANDED), 0.0001f);
    }

    @Test public void mediumWindowRetainsTerminalAsLargestPane() {
        float runtime = AdaptivePanelPolicy.runtimeWeight(AdaptiveWindowPolicy.WidthClass.MEDIUM, AdaptiveWindowPolicy.HeightClass.MEDIUM);
        float monitor = AdaptivePanelPolicy.monitorWeight(AdaptiveWindowPolicy.WidthClass.MEDIUM, AdaptiveWindowPolicy.HeightClass.MEDIUM);
        assertEquals(0.26f, runtime, 0.0001f);
        assertEquals(0.18f, monitor, 0.0001f);
        assertTrue(1f > runtime + monitor);
    }
}
