package com.alfa.device_ctrl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ActualWindowMetricsTest {
    @Test public void dpConversionUsesWindowDensity() {
        assertEquals(600, Math.round(1200 / 2f));
        assertEquals(900, Math.round(1800 / 2f));
    }

    @Test public void adaptivePolicyIsDrivenByWindowDpBoundaries() {
        assertEquals(AdaptiveWindowPolicy.WidthClass.COMPACT, AdaptiveWindowPolicy.widthClass(599));
        assertEquals(AdaptiveWindowPolicy.WidthClass.MEDIUM, AdaptiveWindowPolicy.widthClass(600));
        assertEquals(AdaptiveWindowPolicy.WidthClass.EXPANDED, AdaptiveWindowPolicy.widthClass(840));
        assertEquals(AdaptiveWindowPolicy.HeightClass.COMPACT, AdaptiveWindowPolicy.heightClass(479));
        assertEquals(AdaptiveWindowPolicy.HeightClass.MEDIUM, AdaptiveWindowPolicy.heightClass(480));
        assertEquals(AdaptiveWindowPolicy.HeightClass.EXPANDED, AdaptiveWindowPolicy.heightClass(900));
    }

    @Test public void twoPaneRequiresActualWindowWidth() {
        assertFalse(AdaptiveWindowPolicy.useTwoPane(599));
        assertTrue(AdaptiveWindowPolicy.useTwoPane(600));
    }
}
