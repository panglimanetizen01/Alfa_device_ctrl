package com.alfa.device_ctrl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AdaptiveWindowPolicyTest {
    @Test public void widthClassesUseActualWindowBreakpoints() {
        assertEquals(AdaptiveWindowPolicy.WidthClass.COMPACT, AdaptiveWindowPolicy.widthClass(599));
        assertEquals(AdaptiveWindowPolicy.WidthClass.MEDIUM, AdaptiveWindowPolicy.widthClass(600));
        assertEquals(AdaptiveWindowPolicy.WidthClass.EXPANDED, AdaptiveWindowPolicy.widthClass(840));
    }

    @Test public void heightClassesUseActualWindowHeight() {
        assertEquals(AdaptiveWindowPolicy.HeightClass.COMPACT, AdaptiveWindowPolicy.heightClass(479));
        assertEquals(AdaptiveWindowPolicy.HeightClass.MEDIUM, AdaptiveWindowPolicy.heightClass(480));
        assertEquals(AdaptiveWindowPolicy.HeightClass.EXPANDED, AdaptiveWindowPolicy.heightClass(900));
    }

    @Test public void navigationAdaptsToWindowWidth() {
        assertFalse(AdaptiveWindowPolicy.useTwoPane(599));
        assertTrue(AdaptiveWindowPolicy.useTwoPane(600));
        assertFalse(AdaptiveWindowPolicy.usePersistentNavigation(839));
        assertTrue(AdaptiveWindowPolicy.usePersistentNavigation(840));
        assertEquals(0, AdaptiveWindowPolicy.navigationWidthDp(599));
        assertEquals(96, AdaptiveWindowPolicy.navigationWidthDp(600));
        assertEquals(280, AdaptiveWindowPolicy.navigationWidthDp(840));
    }
}
