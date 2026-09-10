package com.alfa.device_ctrl;

/** Pure actual-window policy for the native runtime/monitor/terminal vertical stack. */
public final class AdaptiveRuntimeLayoutPolicy {
    public static final class Layout {
        public final int runtimeDp;
        public final int monitorDp;
        public final int terminalMinDp;

        Layout(int runtimeDp, int monitorDp, int terminalMinDp) {
            this.runtimeDp = runtimeDp;
            this.monitorDp = monitorDp;
            this.terminalMinDp = terminalMinDp;
        }
    }

    private AdaptiveRuntimeLayoutPolicy() { }

    public static Layout resolve(int availableHeightDp, int runtimeCount) {
        if (availableHeightDp <= 0) return new Layout(112, 80, 140);
        int gaps = 16;
        int usable = Math.max(0, availableHeightDp - gaps);
        int terminalMin = availableHeightDp < 400 ? 140 : (availableHeightDp < 480 ? 160 : 220);
        int monitor;
        int runtime;
        if (usable >= 600) {
            runtime = 248;
            monitor = 158;
        } else {
            monitor = clamp(Math.round(usable * 0.22f), 72, 158);
            runtime = clamp(usable - monitor - terminalMin, 112, 248);
            int overflow = runtime + monitor + terminalMin - usable;
            if (overflow > 0) {
                int reducibleRuntime = Math.max(0, runtime - 112);
                int reduceRuntime = Math.min(reducibleRuntime, overflow);
                runtime -= reduceRuntime;
                overflow -= reduceRuntime;
                if (overflow > 0) monitor = Math.max(72, monitor - overflow);
            }
        }
        return new Layout(runtime, monitor, terminalMin);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
