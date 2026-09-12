package com.alfa.device_ctrl;

/** Pure startup decision: never auto-start a terminal across an unproven execution boundary. */
final class RuntimeStartupPolicy {
    private RuntimeStartupPolicy() { }

    static boolean shouldAutoStart(boolean uiVisible, boolean sessionRunning, boolean gate7Authorized, boolean runtimeReady) {
        return uiVisible && !sessionRunning && gate7Authorized && runtimeReady;
    }
}
