package com.alfa.device_ctrl.external;

import com.alfa.device_ctrl.external.IAlfaShizukuCallback;

interface IAlfaShizukuService {
    void execute(in String[] command, String workDir, IAlfaShizukuCallback callback);
    void writeStdin(String input);
    void terminate();
}
