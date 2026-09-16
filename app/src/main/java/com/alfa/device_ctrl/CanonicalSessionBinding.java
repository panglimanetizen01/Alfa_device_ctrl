package com.alfa.device_ctrl;

/** Canonical G2 session-to-runtime binding. */
public final class CanonicalSessionBinding {
    private CanonicalSessionBinding() { }

    public static String runtimeId(String sessionId) {
        if ("SESSION_A".equals(sessionId)) return "debian";
        if ("SESSION_B".equals(sessionId)) return "ubuntu";
        if ("SESSION_C".equals(sessionId)) return "alpine";
        if ("SESSION_D".equals(sessionId)) return "kali";
        throw new IllegalArgumentException("unknown-session:" + sessionId);
    }

    public static String sessionId(String runtimeId) {
        if ("debian".equals(runtimeId)) return "SESSION_A";
        if ("ubuntu".equals(runtimeId)) return "SESSION_B";
        if ("alpine".equals(runtimeId)) return "SESSION_C";
        if ("kali".equals(runtimeId)) return "SESSION_D";
        throw new IllegalArgumentException("unknown-runtime:" + runtimeId);
    }
}
