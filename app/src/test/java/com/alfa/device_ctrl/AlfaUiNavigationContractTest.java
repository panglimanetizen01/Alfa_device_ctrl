package com.alfa.device_ctrl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AlfaUiNavigationContractTest {
    @Test public void runtimeScreenBackReturnsToTerminal() {
        assertEquals(AlfaUiNavigation.Screen.TERMINAL,
                AlfaUiNavigation.backFrom(AlfaUiNavigation.Screen.RUNTIME));
    }

    @Test public void settingsBackReturnsToPreviousScreen() {
        assertEquals(AlfaUiNavigation.Screen.TERMINAL,
                AlfaUiNavigation.backFrom(AlfaUiNavigation.Screen.SETTINGS));
        assertEquals(AlfaUiNavigation.Screen.RUNTIME,
                AlfaUiNavigation.backFrom(AlfaUiNavigation.Screen.SETTINGS, AlfaUiNavigation.Screen.RUNTIME));
    }

    @Test public void secondaryScreensHaveExplicitBackTargets() {
        assertEquals(AlfaUiNavigation.Screen.TERMINAL,
                AlfaUiNavigation.backFrom(AlfaUiNavigation.Screen.NETWORK));
        assertEquals(AlfaUiNavigation.Screen.TERMINAL,
                AlfaUiNavigation.backFrom(AlfaUiNavigation.Screen.DIAGNOSTICS));
        assertEquals(AlfaUiNavigation.Screen.TERMINAL,
                AlfaUiNavigation.backFrom(AlfaUiNavigation.Screen.LANES));
    }
}
