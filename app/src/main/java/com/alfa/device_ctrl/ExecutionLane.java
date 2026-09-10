package com.alfa.device_ctrl;

/**
 * Execution paths used around Alfa. External lanes remain operationally outside
 * the Android UI/session implementation and are observed only through evidence.
 */
public enum ExecutionLane {
    TERMUX(true),
    TERMUX_API(true),
    SHIZUKU_RISH(true),
    RUNTIME(false);

    private final boolean externalToApp;

    ExecutionLane(boolean externalToApp) {
        this.externalToApp = externalToApp;
    }

    public boolean isExternalToApp() {
        return externalToApp;
    }
}
