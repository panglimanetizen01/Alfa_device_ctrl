package com.alfa.device_ctrl;

import java.io.File;

/** One persisted host-directory to guest-path PRoot bind rule. */
public final class RuntimeDirectoryOverride {
    private final String runtimeId;
    private final String hostPath;
    private final String guestPath;

    public RuntimeDirectoryOverride(String runtimeId, String hostPath, String guestPath) {
        this.runtimeId = require(runtimeId, "runtimeId");
        this.hostPath = canonicalDirectory(hostPath);
        this.guestPath = canonicalGuestPath(guestPath);
    }

    public String runtimeId() { return runtimeId; }
    public String hostPath() { return hostPath; }
    public String guestPath() { return guestPath; }
    public boolean appliesTo(String selectedRuntimeId) { return "*".equals(runtimeId) || runtimeId.equals(selectedRuntimeId); }

    public String prootBindArgument() { return hostPath + ":" + guestPath; }

    public static String canonicalDirectory(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("host-path-required");
        try {
            File file = new File(value).getCanonicalFile();
            if (!file.isDirectory() || !file.canRead()) throw new IllegalArgumentException("host-directory-unavailable");
            String path = file.getAbsolutePath();
            if (!(path.equals("/sdcard") || path.startsWith("/sdcard/") || path.equals("/storage/emulated/0") || path.startsWith("/storage/emulated/0/"))) {
                throw new IllegalArgumentException("host-directory-outside-shared-storage");
            }
            return path;
        } catch (Exception error) {
            if (error instanceof IllegalArgumentException) throw (IllegalArgumentException) error;
            throw new IllegalArgumentException("host-directory-invalid", error);
        }
    }

    public static String canonicalGuestPath(String value) {
        if (value == null || value.trim().isEmpty() || value.indexOf('\0') >= 0 || value.indexOf('|') >= 0) throw new IllegalArgumentException("guest-path-invalid");
        String path = value.trim().replaceAll("/+", "/");
        if (!path.startsWith("/") || path.contains("/../") || path.endsWith("/..") || "/".equals(path)) throw new IllegalArgumentException("guest-path-invalid");
        if (!(path.startsWith("/mnt/") || path.startsWith("/workspace/"))) throw new IllegalArgumentException("guest-path-must-be-mountpoint");
        return path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
    }

    public String serialize() { return runtimeId + "|" + hostPath + "|" + guestPath; }

    public static RuntimeDirectoryOverride parse(String line) {
        if (line == null) throw new IllegalArgumentException("override-line-null");
        String[] parts = line.split("\\|", -1);
        if (parts.length != 3) throw new IllegalArgumentException("override-line-invalid");
        return new RuntimeDirectoryOverride(parts[0], parts[1], parts[2]);
    }

    private static String require(String value, String name) {
        if (value == null || value.trim().isEmpty() || value.indexOf('\0') >= 0 || value.indexOf('|') >= 0) throw new IllegalArgumentException(name + "-invalid");
        if (!"*".equals(value) && RuntimeRegistry.get(value) == null) throw new IllegalArgumentException("unsupported-runtime-id");
        return value.trim();
    }
}
