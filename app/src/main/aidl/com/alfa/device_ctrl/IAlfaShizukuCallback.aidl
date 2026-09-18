package com.alfa.device_ctrl;

interface IAlfaShizukuCallback {
    void onStarted(long pid);
    void onStdout(String chunk);
    void onStderr(String chunk);
    void onExit(int exitCode);
    void onFailure(String reason);
}