#!/usr/bin/env bash
# Validate the immutable Debian first-acceptance rootfs artifact without installing it.
set -euo pipefail

URL="https://github.com/debuerreotype/docker-debian-artifacts/raw/14d91d295c23da6cc04d4bfe8b3d74a8a6c54e5c/bookworm/oci/blobs/rootfs.tar.gz"
EXPECTED_SHA256="c6cbf97176c58c741329cd787e932a1e47931b35f5dc0f23db3e6e82924fef0f"
TMP_DIR="${TMPDIR:-/tmp}/alfa-debian-rootfs.$$"
ARCHIVE="$TMP_DIR/rootfs.tar.gz"
trap 'rm -rf "$TMP_DIR"' EXIT HUP INT TERM
mkdir -p "$TMP_DIR"

curl -fL --retry 3 --retry-delay 1 "$URL" -o "$ARCHIVE"
printf '%s  %s\n' "$EXPECTED_SHA256" "$ARCHIVE" | sha256sum -c -
gzip -t "$ARCHIVE"

list=$(tar -tzf "$ARCHIVE")
grep -Eq '^etc/os-release$' <<< "$list"
grep -Eq '^bin/sh$|^usr/bin/sh$' <<< "$list"
grep -Eq '^usr/bin/env$' <<< "$list"
grep -Eq '^usr/$' <<< "$list"

mkdir -p "$TMP_DIR/rootfs"
tar -xzf "$ARCHIVE" -C "$TMP_DIR/rootfs" etc/os-release
rootfs_id=$(sed -n 's/^ID=//p' "$TMP_DIR/rootfs/etc/os-release" | tr -d '"')
printf 'ROOTFS_ID=%s\n' "$rootfs_id"
test "$rootfs_id" = debian
printf 'DEBIAN_ROOTFS_CONTRACT=PASS\n'
printf 'SHA256=%s\n' "$EXPECTED_SHA256"
printf 'URL=%s\n' "$URL"
