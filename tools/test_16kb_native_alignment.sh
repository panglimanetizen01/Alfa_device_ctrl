#!/usr/bin/env bash
# Verify ELF LOAD alignment and APK zip alignment for 16 KB Android page-size support.
set -euo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
LIB_DIR="${2:-app/build/generated/jniLibs/arm64-v8a}"

check_elf() {
    local file="$1"
    test -f "$file"
    readelf -h "$file" | grep -q 'Class:.*ELF64'
    readelf -h "$file" | grep -q 'Machine:.*AArch64'
    local bad
    bad="$(readelf -lW "$file" | awk '/^[[:space:]]*LOAD[[:space:]]/ {print $NF}' | grep -v '^0x4000$' || true)"
    if [ -n "$bad" ]; then
        echo "ALIGNMENT_FAIL=$file"
        printf '%s\n' "$bad"
        return 1
    fi
}

check_elf "app/src/main/jniLibs/arm64-v8a/libproot.so"
check_elf "$LIB_DIR/libproot-loader.so"
zipalign -c -P 16 -v 4 "$APK"
printf 'NATIVE_16KB_ALIGNMENT=PASS\n'
printf 'APK=%s\n' "$APK"
