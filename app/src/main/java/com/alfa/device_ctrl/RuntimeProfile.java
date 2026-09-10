package com.alfa.device_ctrl;

/** Immutable metadata describing one installable Linux runtime. */
public final class RuntimeProfile {
    /** Legacy first-acceptance constants retained until production callers migrate to the registry. */
    public static final String ID = "debian";
    public static final String DISPLAY_NAME = "Debian";
    public static final String ROOTFS_URL = "https://github.com/debuerreotype/docker-debian-artifacts/raw/14d91d295c23da6cc04d4bfe8b3d74a8a6c54e5c/bookworm/oci/blobs/rootfs.tar.gz";
    public static final String ROOTFS_SHA256 = "c6cbf97176c58c741329cd787e932a1e47931b35f5dc0f23db3e6e82924fef0f";
    public static final boolean ROOTFS_GZIP = true;

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
        this.id = id;
        this.displayName = displayName;
        this.architecture = architecture;
        this.rootfsUrl = rootfsUrl;
        this.rootfsSha256 = rootfsSha256;
        this.rootfsGzip = rootfsGzip;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String architecture() { return architecture; }
    public String rootfsUrl() { return rootfsUrl; }
    public String rootfsSha256() { return rootfsSha256; }
    public boolean rootfsGzip() { return rootfsGzip; }
}
