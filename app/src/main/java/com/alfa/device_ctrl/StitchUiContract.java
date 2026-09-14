package com.alfa.device_ctrl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Canonical semantic labels shared by the Stitch operational presentation. */
public final class StitchUiContract {
    private StitchUiContract() {}

    public static final String BRAND = "ALFA DEVICE CTRL";
    public static final String SUBTITLE = "Linux Runtime Control";
    public static final String SYSTEM_ONLINE = "SYSTEM ONLINE";
    public static final String[] NAV_LABELS = {
            "TERMINAL", "RUNTIMES", "MONITOR", "SECURITY", "SETTINGS"
    };
    public static final String[] TELEMETRY_LABELS = {
            "CPU", "MEM", "STORAGE", "NET"
    };
    public static final List<String> RUNTIME_ACTIONS = Collections.unmodifiableList(Arrays.asList(
            "CHECK", "OPEN", "INSTALL"
    ));
    public static final List<String> TERMINAL_ACTIONS = Collections.unmodifiableList(Arrays.asList(
            "RESTART SESSION", "CLEAR", "COMMAND"
    ));
    public static final List<String> DISTRO_IDS = Collections.unmodifiableList(Arrays.asList(
            "debian", "ubuntu", "alpine", "kali"
    ));
}
