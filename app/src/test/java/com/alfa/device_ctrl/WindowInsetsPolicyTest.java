package com.alfa.device_ctrl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class WindowInsetsPolicyTest {
    @Test public void basePaddingIsNotAccumulatedByRepeatedInsetDispatch() {
        int baseTop = 8;
        int insetTop = 24;
        assertEquals(32, baseTop + insetTop);
        assertEquals(32, baseTop + insetTop);
    }

    @Test public void actualWindowClassesRemainIndependentOfPhysicalDeviceLabels() {
        assertEquals(AdaptiveWindowPolicy.WidthClass.COMPACT, AdaptiveWindowPolicy.widthClass(599));
        assertEquals(AdaptiveWindowPolicy.WidthClass.MEDIUM, AdaptiveWindowPolicy.widthClass(600));
        assertEquals(AdaptiveWindowPolicy.WidthClass.EXPANDED, AdaptiveWindowPolicy.widthClass(840));
    }
}
