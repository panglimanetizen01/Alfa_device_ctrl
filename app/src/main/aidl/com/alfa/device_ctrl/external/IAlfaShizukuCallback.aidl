package com.alfa.device_ctrl.external;

interface IAlfaShizukuCallback {
    void onStarted(long pid, int uid);
    void onStdout(String chunk);
    void onStderr(String chunk);
    void onExit(int exitCode);
    void onError(String message);
}
