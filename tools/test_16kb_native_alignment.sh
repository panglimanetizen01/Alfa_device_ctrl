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
    python3 - "$file" <<'PY'
import re
import subprocess
import sys
path = sys.argv[1]
text = subprocess.check_output(["readelf", "-lW", path], text=True)
aligns = []
for line in text.splitlines():
    if re.match(r"^\s*LOAD\s", line):
        value = line.split()[-1]
        align = int(value, 16)
        aligns.append(align)
        if align < 0x4000 or align % 0x4000 != 0:
            raise SystemExit(f"ALIGNMENT_FAIL={path}:{value}")
if not aligns:
    raise SystemExit(f"ALIGNMENT_FAIL={path}:no-LOAD-segments")
print(f"ELF_16KB_ALIGNMENT_PASS={path} " + ",".join(hex(v) for v in aligns))
PY
}

check_elf "app/src/main/jniLibs/arm64-v8a/libproot.so"
check_elf "$LIB_DIR/libproot-loader.so"
zipalign -c -P 16 -v 4 "$APK"
printf 'NATIVE_16KB_ALIGNMENT=PASS\n'
printf 'APK=%s\n' "$APK"
