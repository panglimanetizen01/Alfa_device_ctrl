#!/usr/bin/env bash
set -euo pipefail

APK="${1:?usage: verify_stitch_apk.sh <apk> [blacklist] }"
BLACKLIST="${2:-tools/ci/stitch-legacy-ui-blacklist.txt}"

[[ -f "$APK" && -s "$APK" ]]
[[ -f "$BLACKLIST" && -s "$BLACKLIST" ]]

python3 - "$APK" "$BLACKLIST" <<'PY'
import hashlib
import struct
import sys
import zipfile
from pathlib import Path

apk = Path(sys.argv[1])
blacklist_path = Path(sys.argv[2])
blacklist = [line.rstrip("\n") for line in blacklist_path.read_text("utf-8").splitlines() if line and not line.startswith("#")]
if not blacklist:
    raise SystemExit("EMPTY_EXPLICIT_BLACKLIST")
if any("*" in item or "?" in item for item in blacklist):
    raise SystemExit("WILDCARD_BLACKLIST_FORBIDDEN")

required = [
    "SYSTEM ONLINE",
    "LINUX RUNTIMES",
    "TERMINAL",
    "RUNTIMES",
    "AlfaStitchOperationalPanels",
    "StitchOperationalActivity",
]

with zipfile.ZipFile(apk) as z:
    bad = z.testzip()
    if bad:
        raise SystemExit(f"CORRUPT_APK_ENTRY={bad}")
    names = z.namelist()
    dex = [n for n in names if n.startswith("classes") and n.endswith(".dex")]
    if not dex:
        raise SystemExit("NO_DEX_FILES")

    hits = []
    required_hits = {item: [] for item in required}
    for name in names:
        data = z.read(name)
        haystacks = [data]
        for enc in ("utf-8", "utf-16-le", "utf-16-be"):
            try:
                haystacks.append(data.decode(enc).encode("utf-8"))
            except UnicodeDecodeError:
                pass
        for item in blacklist:
            needle = item.encode("utf-8")
            if any(needle in h for h in haystacks):
                hits.append((name, item))
        for item in required:
            if any(item.encode("utf-8") in h for h in haystacks):
                required_hits[item].append(name)

    if hits:
        for name, item in hits:
            print(f"LEGACY_UI_RESIDUE entry={name} literal={item}")
        raise SystemExit("APK_LEGACY_UI_RESIDUE=FAIL")

    missing = [item for item, locations in required_hits.items() if not locations]
    if missing:
        raise SystemExit("MISSING_STITCH_UI_CONTRACT=" + ",".join(missing))

    print("=== APK ZIP FORENSIC STRUCTURE ===")
    print(f"APK_SIZE_BYTES={apk.stat().st_size}")
    print(f"APK_ENTRY_COUNT={len(names)}")
    with apk.open("rb") as raw:
        for info in z.infolist():
            if not info.filename.endswith(".so"):
                continue
            raw.seek(info.header_offset)
            header = raw.read(30)
            if len(header) != 30 or header[:4] != b"PK\x03\x04":
                raise SystemExit(f"APK_ZIP_LOCAL_HEADER_INVALID entry={info.filename}")
            fields = struct.unpack("<4s5H3I2H", header)
            name_len = fields[9]
            extra_len = fields[10]
            payload = info.header_offset + 30 + name_len + extra_len
            print(f"APK_ZIP_SO_ENTRY={info.filename}")
            print(f"APK_ZIP_SO_HEADER_OFFSET={info.header_offset}")
            print(f"APK_ZIP_SO_PAYLOAD_OFFSET={payload}")
            print(f"APK_ZIP_SO_OFFSET_MOD_16384={payload % 16384}")
            print(f"APK_ZIP_SO_COMPRESSION_METHOD={info.compress_type}")
            print(f"APK_ZIP_SO_COMPRESSED_SIZE={info.compress_size}")
            print(f"APK_ZIP_SO_UNCOMPRESSED_SIZE={info.file_size}")

sha = hashlib.sha256(apk.read_bytes()).hexdigest()
print(f"APK_SHA256={sha}")
print(f"APK_ENTRIES={len(names)}")
print(f"APK_DEX_COUNT={len(dex)}")
print("APK_DEEP_RESIDUE_SCAN=PASS")
print("STITCH_UI_BINARY_CONTRACT=PASS")
PY

ZIPALIGN="${ANDROID_HOME:?}/build-tools/35.0.0/zipalign"
ZIPALIGN_OUT="$(mktemp)"
set +e
"$ZIPALIGN" -c -P 16 -v 4 "$APK" >"$ZIPALIGN_OUT" 2>&1
ZIPALIGN_RC=$?
set -e
printf '%s\n' "=== ZIPALIGN DIAGNOSTIC ==="
printf '%s\n' "ZIPALIGN_PATH=$ZIPALIGN"
printf '%s\n' "ZIPALIGN_SHA256=$(sha256sum "$ZIPALIGN" | awk '{print $1}')"
printf '%s\n' "ZIPALIGN_DIAGNOSTIC_RC=$ZIPALIGN_RC"
printf '%s\n' "ZIPALIGN_DIAGNOSTIC_OUTPUT_BEGIN"
cat "$ZIPALIGN_OUT"
printf '%s\n' "ZIPALIGN_DIAGNOSTIC_OUTPUT_END"
rm -f "$ZIPALIGN_OUT"
