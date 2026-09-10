package com.alfa.device_ctrl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Canonical immutable registry of supported initial Linux runtimes. */
public final class RuntimeRegistry {
    private static final List<RuntimeProfile> PROFILES = Collections.unmodifiableList(Arrays.asList(
            new RuntimeProfile("debian","Debian","bookworm","aarch64","https://github.com/debuerreotype/docker-debian-artifacts/raw/14d91d295c23da6cc04d4bfe8b3d74a8a6c54e5c/bookworm/oci/blobs/rootfs.tar.gz","c6cbf97176c58c741329cd787e932a1e47931b35f5dc0f23db3e6e82924fef0f",true,"tar.gz","/bin/sh","apt","alfa:debian:",new String[]{"HOME=/root","TERM=xterm-256color"},new String[]{"bin","etc","usr","usr/bin/env","bin/sh"},new String[]{"rootless-proot","pty","storage-bridge","network-evidence","process-evidence"}),
            new RuntimeProfile("ubuntu","Ubuntu","24.04.4","aarch64","https://cdimages.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.4-base-arm64.tar.gz","04207713ece899c3740823d33690441ad3a7f0ded1101aca744e2b0f37ac7ff2",true,"tar.gz","/bin/sh","apt","alfa:ubuntu:",new String[]{"HOME=/root","TERM=xterm-256color"},new String[]{"bin","etc","usr","usr/bin/env","bin/sh"},new String[]{"rootless-proot","pty","storage-bridge","network-evidence","process-evidence"}),
            new RuntimeProfile("alpine","Alpine","3.24.1","aarch64","https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/alpine-minirootfs-3.24.1-aarch64.tar.gz","f55a90f69052c5bd6f92cb09a8f47065970830b194c917a006fb94028e721259",true,"tar.gz","/bin/sh","apk","alfa:alpine:",new String[]{"HOME=/root","TERM=xterm-256color"},new String[]{"bin","etc","usr","usr/bin/env","bin/sh"},new String[]{"rootless-proot","pty","storage-bridge","network-evidence","process-evidence"}),
            new RuntimeProfile("kali","Kali Linux","current","aarch64","https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-arm64.tar.xz","d6403a5da175df325611d23af4b92330856059c45454eced7f4cdf3ca6df2e4e",false,"tar.xz","/bin/sh","apt","alfa:kali:",new String[]{"HOME=/root","TERM=xterm-256color"},new String[]{"bin","etc","usr","usr/bin/env","bin/sh"},new String[]{"rootless-proot","pty","storage-bridge","network-evidence","process-evidence","kernel-features-limited"})));

    private RuntimeRegistry() { }
    public static List<RuntimeProfile> all(){return PROFILES;}
    public static RuntimeProfile get(String id){if(id==null)return null;for(RuntimeProfile profile:PROFILES)if(profile.id().equals(id))return profile;return null;}
}