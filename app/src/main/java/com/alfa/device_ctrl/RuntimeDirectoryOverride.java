package com.alfa.device_ctrl;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    static boolean isLexicallySharedStoragePath(String value) {
        if (value == null) return false;
        String path = value.trim();
        if (path.isEmpty() || path.indexOf('\0') >= 0 || path.contains("/../") || path.endsWith("/..")) return false;
        return path.equals("/sdcard") || path.startsWith("/sdcard/")
                || path.equals("/storage/emulated/0") || path.startsWith("/storage/emulated/0/");
    }

    public static String canonicalDirectory(String value) {
        if (!isLexicallySharedStoragePath(value)) throw new IllegalArgumentException("host-directory-outside-shared-storage");
        try {
            String supplied = value.trim();
            Path candidate = Paths.get(supplied).toAbsolutePath().normalize();
            Path sharedRoot = supplied.equals("/sdcard") || supplied.startsWith("/sdcard/")
                    ? Paths.get("/sdcard").toAbsolutePath().normalize()
                    : Paths.get("/storage/emulated/0").toAbsolutePath().normalize();
            if (!candidate.startsWith(sharedRoot)) throw new IllegalArgumentException("host-directory-outside-shared-storage");
            File file = candidate.toFile().getCanonicalFile();
            if (!file.isDirectory() || !file.canRead()) throw new IllegalArgumentException("host-directory-unavailable");
            String path = file.getAbsolutePath();
            if (!isLexicallySharedStoragePath(path)) throw new IllegalArgumentException("host-directory-outside-shared-storage");
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
