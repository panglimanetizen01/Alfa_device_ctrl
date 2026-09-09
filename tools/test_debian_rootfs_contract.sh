#!/usr/bin/env bash
# Validate the immutable Debian first-acceptance rootfs artifact without installing it.
set -euo pipefail

URL="https://github.com/debuerreotype/docker-debian-artifacts/raw/14d91d295c23da6cc04d4bfe8b3d74a8a6c54e5c/bookworm/oci/blobs/rootfs.tar.gz"
EXPECTED_SHA256="445be8da0a7289e4b5d70a5c779ad63d484e76aa14fe2ad45893da9eb077e4e8"
TMP_DIR="${TMPDIR:-/tmp}/alfa-debian-rootfs.$$"
ARCHIVE="$TMP_DIR/rootfs.tar.gz"
trap 'rm -rf "$TMP_DIR"' EXIT HUP INT TERM
mkdir -p "$TMP_DIR"

curl -fL --retry 3 --retry-delay 1 "$URL" -o "$ARCHIVE"
printf '%s  %s\n' "$EXPECTED_SHA256" "$ARCHIVE" | sha256sum -c -
gzip -t "$ARCHIVE"

list=$(tar -tzf "$ARCHIVE")
printf '%s\n' "$list" | grep -Eq '^etc/os-release$'
printf '%s\n' "$list" | grep -Eq '^bin/sh$|^usr/bin/sh$'
printf '%s\n' "$list" | grep -Eq '^usr/bin/env$'
printf '%s\n' "$list" | grep -Eq '^usr/$'

mkdir -p "$TMP_DIR/rootfs"
tar -xzf "$ARCHIVE" -C "$TMP_DIR/rootfs" etc/os-release
printf 'ROOTFS_ID='
printf '%s\n' "$(sed -n 's/^ID=//p' "$TMP_DIR/rootfs/etc/os-release" | tr -d '"' | head -n 1)"
test "$(sed -n 's/^ID=//p' "$TMP_DIR/rootfs/etc/os-release" | tr -d '"' | head -n 1)" = debian
printf 'DEBIAN_ROOTFS_CONTRACT=PASS\n'
printf 'SHA256=%s\n' "$EXPECTED_SHA256"
printf 'URL=%s\n' "$URL"
