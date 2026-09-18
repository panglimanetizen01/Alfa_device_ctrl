package com.alfa.device_ctrl;

import com.alfa.device_ctrl.IAlfaShizukuCallback;

interface IAlfaShizukuService {
    void execute(String[] command, IAlfaShizukuCallback callback);
    void destroy();
}