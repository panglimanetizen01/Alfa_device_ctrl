package com.alfa.device_ctrl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Canonical immutable registry of supported initial Linux runtimes. */
public final class RuntimeRegistry {
    private static final List<RuntimeProfile> PROFILES = Collections.unmodifiableList(Arrays.asList(
            new RuntimeProfile(
                    "debian",
                    "Debian",
                    "aarch64",
                    "https://github.com/debuerreotype/docker-debian-artifacts/raw/14d91d295c23da6cc04d4bfe8b3d74a8a6c54e5c/bookworm/oci/blobs/rootfs.tar.gz",
                    "c6cbf97176c58c741329cd787e932a1e47931b35f5dc0f23db3e6e82924fef0f",
                    true),
            new RuntimeProfile(
                    "ubuntu",
                    "Ubuntu",
                    "aarch64",
                    "https://cdimages.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.4-base-arm64.tar.gz",
                    "04207713ece899c3740823d33690441ad3a7f0ded1101aca744e2b0f37ac7ff2",
                    true),
            new RuntimeProfile(
                    "alpine",
                    "Alpine",
                    "aarch64",
                    "https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/alpine-minirootfs-3.24.1-aarch64.tar.gz",
                    "f55a90f69052c5bd6f92cb09a8f47065970830b194c917a006fb94028e721259",
                    true),
            new RuntimeProfile(
                    "kali",
                    "Kali Linux",
                    "aarch64",
                    "https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-arm64.tar.xz",
                    "d6403a5da175df325611d23af4b92330856059c45454eced7f4cdf3ca6df2e4e",
                    false)));

    private RuntimeRegistry() { }

    public static List<RuntimeProfile> all() {
        return PROFILES;
    }

    public static RuntimeProfile get(String id) {
        for (RuntimeProfile profile : PROFILES) {
            if (profile.id().equals(id)) {
                return profile;
            }
        }
        return null;
    }
}
