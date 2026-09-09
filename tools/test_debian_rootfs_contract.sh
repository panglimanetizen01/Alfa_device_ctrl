#!/usr/bin/env bash
# Validate the immutable Debian first-acceptance rootfs artifact without installing it.
set -euo pipefail

URL="https://github.com/debuerreotype/docker-debian-artifacts/raw/fb7215b47dab72bdbdd59204a7b7914311431d90/bookworm/rootfs.tar.xz"
EXPECTED_SHA256="202ecca447dbf1b3ac1b1e983d9363381ac6a34f8e22d7d786125d06754ebb76"
TMP_DIR="${TMPDIR:-/tmp}/alfa-debian-rootfs.$$"
ARCHIVE="$TMP_DIR/rootfs.tar.xz"
trap 'rm -rf "$TMP_DIR"' EXIT HUP INT TERM
mkdir -p "$TMP_DIR"

curl -fL --retry 3 --retry-delay 1 "$URL" -o "$ARCHIVE"
printf '%s  %s\n' "$EXPECTED_SHA256" "$ARCHIVE" | sha256sum -c -
xz -t "$ARCHIVE"

list=$(tar -tJf "$ARCHIVE")
printf '%s\n' "$list" | grep -Eq '^etc/os-release$'
printf '%s\n' "$list" | grep -Eq '^bin/sh$|^usr/bin/sh$'
printf '%s\n' "$list" | grep -Eq '^usr/bin/env$'
printf '%s\n' "$list" | grep -Eq '^usr/$'

mkdir -p "$TMP_DIR/rootfs"
tar -xJf "$ARCHIVE" -C "$TMP_DIR/rootfs" etc/os-release
printf 'ROOTFS_ID='
printf '%s\n' "$(sed -n 's/^ID=//p' "$TMP_DIR/rootfs/etc/os-release" | tr -d '"' | head -n 1)"
test "$(sed -n 's/^ID=//p' "$TMP_DIR/rootfs/etc/os-release" | tr -d '"' | head -n 1)" = debian
printf 'DEBIAN_ROOTFS_CONTRACT=PASS\n'
printf 'SHA256=%s\n' "$EXPECTED_SHA256"
printf 'URL=%s\n' "$URL"
