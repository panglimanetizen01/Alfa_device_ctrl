package com.alfa.device_ctrl;

/** Deterministic navigation state machine for the single-activity Stitch operational shell. */
public final class AlfaUiNavigation {
    public enum Screen {
        TERMINAL,
        RUNTIME,
        NETWORK,
        LANES,
        DIAGNOSTICS,
        STORAGE,
        SECURITY,
        AUDIT,
        PROJECT,
        SESSIONS,
        SPLIT,
        FLOATING,
        APPEARANCE,
        SETTINGS
    }

    private AlfaUiNavigation() { }

    public static Screen backFrom(Screen current) {
        return backFrom(current, Screen.TERMINAL);
    }

    public static Screen backFrom(Screen current, Screen previous) {
        if (current == null) return Screen.TERMINAL;
        if (current == Screen.SETTINGS && previous != null) return previous;
        return current == Screen.TERMINAL ? Screen.TERMINAL : Screen.TERMINAL;
    }
}
