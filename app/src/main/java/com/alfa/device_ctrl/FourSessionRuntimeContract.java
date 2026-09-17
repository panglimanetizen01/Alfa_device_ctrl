package com.alfa.device_ctrl;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical four-session binding: one independent session identity per registered runtime.
 * This is deliberately pure state; process/PTY ownership remains in RuntimeSessionManager.
 */
public final class FourSessionRuntimeContract {
    public static final int SESSION_COUNT = 4;
    private static final List<String> SESSION_IDS = Collections.unmodifiableList(
            Arrays.asList("SESSION_A", "SESSION_B", "SESSION_C", "SESSION_D"));
    private static final List<String> RUNTIME_IDS = Collections.unmodifiableList(
            Arrays.asList("debian", "ubuntu", "alpine", "kali"));

    private final Map<String, String> sessionToRuntime;
    private final Map<String, String> sessionCwdNames;

    public FourSessionRuntimeContract() {
        LinkedHashMap<String, String> mapping = new LinkedHashMap<>();
        LinkedHashMap<String, String> cwd = new LinkedHashMap<>();
        for (int i = 0; i < SESSION_COUNT; i++) {
            String session = SESSION_IDS.get(i);
            String runtime = RUNTIME_IDS.get(i);
            mapping.put(session, runtime);
            cwd.put(session, session.toLowerCase(java.util.Locale.ROOT));
        }
        sessionToRuntime = Collections.unmodifiableMap(mapping);
        sessionCwdNames = Collections.unmodifiableMap(cwd);
    }

    public List<String> sessionIds() { return SESSION_IDS; }
    public List<String> runtimeIds() { return RUNTIME_IDS; }
    public String runtimeFor(String sessionId) { return require(sessionToRuntime, sessionId); }
    public String cwdNameFor(String sessionId) { return require(sessionCwdNames, sessionId); }

    public boolean isCanonicalPair(String sessionId, String runtimeId) {
        return runtimeId != null && runtimeId.equals(sessionToRuntime.get(sessionId));
    }

    public boolean isIsolatedFrom(String leftSessionId, String rightSessionId) {
        if (leftSessionId == null || rightSessionId == null || leftSessionId.equals(rightSessionId)) return false;
        return !cwdNameFor(leftSessionId).equals(cwdNameFor(rightSessionId));
    }

    public String[] expectedSessionCwds() {
        return new String[]{"session-a", "session-b", "session-c", "session-d"};
    }

    private static String require(Map<String, String> map, String key) {
        String value = map.get(key);
        if (value == null) throw new IllegalArgumentException("unknown-session:" + key);
        return value;
    }
}
