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
}
