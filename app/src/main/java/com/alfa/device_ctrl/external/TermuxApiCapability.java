package com.alfa.device_ctrl.external;

import android.content.Context;
import android.content.pm.PackageManager;

/** Detects Termux:API as a device-API capability; it is not a shell transport. */
public final class TermuxApiCapability {
    public static final String PACKAGE_NAME = "com.termux.api";

    private final Context context;

    public TermuxApiCapability(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isInstalled() {
        try {
            context.getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    public boolean isDeviceApiLaneOnly() {
        return true;
    }
}
