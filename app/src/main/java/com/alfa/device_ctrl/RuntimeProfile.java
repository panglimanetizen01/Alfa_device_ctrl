package com.alfa.device_ctrl;

/** Canonical first acceptance runtime and its immutable rootfs artifact. */
public final class RuntimeProfile {
    public static final String ID = "debian";
    public static final String DISPLAY_NAME = "Debian";
    public static final String ROOTFS_URL = "https://github.com/debuerreotype/docker-debian-artifacts/raw/14d91d295c23da6cc04d4bfe8b3d74a8a6c54e5c/bookworm/oci/blobs/rootfs.tar.gz";
    public static final String ROOTFS_SHA256 = "445be8da0a7289e4b5d70a5c779ad63d484e76aa14fe2ad45893da9eb077e4e8";
    public static final boolean ROOTFS_GZIP = true;

    private RuntimeProfile() { }
}
