package com.alfa.device_ctrl;

/** Canonical first acceptance runtime and its immutable rootfs artifact. */
public final class RuntimeProfile {
    public static final String ID = "debian";
    public static final String DISPLAY_NAME = "Debian";
    public static final String ROOTFS_URL = "https://github.com/debuerreotype/docker-debian-artifacts/raw/14d91d295c23da6cc04d4bfe8b3d74a8a6c54e5c/bookworm/oci/blobs/rootfs.tar.gz";
    public static final String ROOTFS_SHA256 = "c6cbf97176c58c741329cd787e932a1e47931b35f5dc0f23db3e6e82924fef0f";
    public static final boolean ROOTFS_GZIP = true;

    private RuntimeProfile() { }
}
