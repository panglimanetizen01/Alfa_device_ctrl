package com.alfa.device_ctrl;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AdaptiveRuntimeLayoutPolicyTest {
    @Test public void compactWindowPreservesTerminalSpace() {
        AdaptiveRuntimeLayoutPolicy.Layout compact = AdaptiveRuntimeLayoutPolicy.resolve(420, 4);
        assertTrue(compact.runtimeDp < 248);
        assertTrue(compact.monitorDp < 158);
        assertTrue(compact.terminalMinDp >= 160);
        assertTrue(compact.runtimeDp + compact.monitorDp + compact.terminalMinDp <= 404);
    }

    @Test public void veryShortWindowStillFitsAllSections() {
        AdaptiveRuntimeLayoutPolicy.Layout compact = AdaptiveRuntimeLayoutPolicy.resolve(350, 4);
        assertTrue(compact.terminalMinDp >= 140);
        assertTrue(compact.runtimeDp + compact.monitorDp + compact.terminalMinDp <= 334);
    }

    @Test public void tenRuntimeProfilesCannotBreakCompactFit() {
        AdaptiveRuntimeLayoutPolicy.Layout compact = AdaptiveRuntimeLayoutPolicy.resolve(350, 10);
        assertTrue(compact.runtimeDp + compact.monitorDp + compact.terminalMinDp <= 334);
        assertTrue(compact.runtimeDp >= 112);
        assertTrue(compact.monitorDp >= 72);
        assertTrue(compact.terminalMinDp >= 140);
    }

    @Test public void expandedWindowKeepsPreferredRuntimeAndMonitorSizes() {
        AdaptiveRuntimeLayoutPolicy.Layout expanded = AdaptiveRuntimeLayoutPolicy.resolve(1100, 4);
        assertTrue(expanded.runtimeDp >= 220 && expanded.runtimeDp <= 280);
        assertTrue(expanded.monitorDp >= 140 && expanded.monitorDp <= 180);
        assertTrue(expanded.terminalMinDp >= 220);
    }
}
