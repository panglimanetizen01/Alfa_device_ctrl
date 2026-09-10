package com.alfa.device_ctrl;

/** Resolves the user-selected runtime against the canonical registry. */
public final class RuntimeSelection {
    public static final String DEFAULT_RUNTIME_ID = "debian";

    private RuntimeSelection() { }

    public static String resolveId(String requestedId) {
        if (requestedId == null || requestedId.trim().isEmpty()) return DEFAULT_RUNTIME_ID;
        String normalized = requestedId.trim();
        return RuntimeRegistry.get(normalized) == null ? DEFAULT_RUNTIME_ID : normalized;
    }

    public static RuntimeProfile profile(String requestedId) {
        return RuntimeRegistry.get(resolveId(requestedId));
    }
}
