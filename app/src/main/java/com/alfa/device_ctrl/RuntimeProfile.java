package com.alfa.device_ctrl;

/** Immutable metadata describing one installable Linux runtime. */
public final class RuntimeProfile {
    private final String id;
    private final String displayName;
    private final String architecture;
    private final String rootfsUrl;
    private final String rootfsSha256;
    private final boolean rootfsGzip;

    public RuntimeProfile(
            String id,
            String displayName,
            String architecture,
            String rootfsUrl,
            String rootfsSha256,
            boolean rootfsGzip) {
        this.id = require(id, "id");
        this.displayName = require(displayName, "displayName");
        this.architecture = require(architecture, "architecture");
        this.rootfsUrl = require(rootfsUrl, "rootfsUrl");
        this.rootfsSha256 = require(rootfsSha256, "rootfsSha256");
        this.rootfsGzip = rootfsGzip;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String architecture() { return architecture; }
    public String rootfsUrl() { return rootfsUrl; }
    public String rootfsSha256() { return rootfsSha256; }
    public boolean rootfsGzip() { return rootfsGzip; }

    private static String require(String value, String name) {
        if (value == null || value.trim().isEmpty() || value.indexOf('\0') >= 0) throw new IllegalArgumentException(name + " is invalid");
        return value;
    }
}