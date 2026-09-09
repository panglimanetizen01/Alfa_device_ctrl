package com.alfa.device_ctrl;

/** Canonical first acceptance runtime and its immutable rootfs artifact. */
public final class RuntimeProfile {
    public static final String ID = "debian";
    public static final String DISPLAY_NAME = "Debian";
    public static final String ROOTFS_URL = "https://github.com/debuerreotype/docker-debian-artifacts/raw/fb7215b47dab72bdbdd59204a7b7914311431d90/bookworm/rootfs.tar.xz";
    public static final String ROOTFS_SHA256 = "202ecca447dbf1b3ac1b1e983d9363381ac6a34f8e22d7d786125d06754ebb76";
    public static final boolean ROOTFS_GZIP = false;

    private RuntimeProfile() { }
}
