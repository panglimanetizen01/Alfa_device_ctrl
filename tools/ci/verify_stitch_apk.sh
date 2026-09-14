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
    "AlfaFinalUiHooks",
    "MainActivity",
]

with zipfile.ZipFile(apk) as z:
    bad = z.testzip()
    if bad:
        raise SystemExit(f"CORRUPT_APK_ENTRY={bad}")
    names = z.namelist()
    dex = [n for n in names if n.startswith("classes") and n.endswith(".dex")]
    if not dex:
        raise SystemExit("NO_DEX_FILES")

    # Deep-scan every regular APK member, not only resources.arsc. Matching is
    # exact-literal and encoding-aware; no wildcard or heuristic matching.
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

sha = hashlib.sha256(apk.read_bytes()).hexdigest()
print(f"APK_SHA256={sha}")
print(f"APK_ENTRIES={len(names)}")
print(f"APK_DEX_COUNT={len(dex)}")
print("APK_DEEP_RESIDUE_SCAN=PASS")
print("STITCH_UI_BINARY_CONTRACT=PASS")
PY

# Diagnostic instrumentation intentionally runs before the workflow's final
# zipalign gate. It does not mutate the APK and does not change packaging.
echo "=== APK ZIP STRUCTURE FORENSIC ==="
unzip -t "$APK"
echo "APK_ZIP_TEST_ARCHIVE=PASS"
unzip -l "$APK"
ENTRY_COUNT="$(unzip -Z1 "$APK" | wc -l | tr -d ' ')"
echo "APK_ZIP_ENTRY_COUNT=$ENTRY_COUNT"

echo "--- SO Entry Analysis ---"
python3 - "$APK" <<'PY'
import struct
import sys
import zipfile

apk = sys.argv[1]
PAGE = 16384
with zipfile.ZipFile(apk) as z, open(apk, "rb") as fh:
    for info in z.infolist():
        if not info.filename.endswith(".so"):
            continue
        fh.seek(info.header_offset)
        header = fh.read(30)
        if len(header) != 30 or header[:4] != b"PK\x03\x04":
            print(f"APK_ZIP_SO_ENTRY={info.filename}")
            print("APK_ZIP_SO_HEADER=INVALID")
            continue
        name_len, extra_len = struct.unpack_from("<HH", header, 26)
        payload_offset = info.header_offset + 30 + name_len + extra_len
        mod = payload_offset % PAGE
        method = info.compress_type
        print(f"APK_ZIP_SO_ENTRY={info.filename}")
        print(f"APK_ZIP_SO_HEADER_OFFSET={info.header_offset}")
        print(f"APK_ZIP_SO_PAYLOAD_OFFSET={payload_offset}")
        print(f"APK_ZIP_SO_OFFSET={payload_offset}")
        print(f"APK_ZIP_SO_OFFSET_MOD_16384={mod}")
        print(f"APK_ZIP_SO_COMPRESSION_METHOD={method}")
        if method == zipfile.ZIP_STORED and mod != 0:
            print(f"VIOLATION_DETECTED={info.filename} payload_offset={payload_offset} mod_16384={mod}")
PY

echo "=== ZIPALIGN DIAGNOSTIC ==="
ZIPALIGN="$ANDROID_HOME/build-tools/35.0.0/zipalign"
if [[ -x "$ZIPALIGN" ]]; then
  OUT="$(mktemp)"
  ERR="$(mktemp)"
  set +e
  "$ZIPALIGN" -c -P 16 -v 4 "$APK" >"$OUT" 2>"$ERR"
  ZIPALIGN_RC=$?
  set -e
  cat "$OUT"
  cat "$ERR" >&2
  echo "ZIPALIGN_DIAGNOSTIC_RC=$ZIPALIGN_RC"
  ZIPALIGN_DIAGNOSTIC_STDERR="$(tr '\n' ' ' < "$ERR" | sed 's/[[:space:]]\+/ /g; s/^ //; s/ $//')"
  echo "ZIPALIGN_DIAGNOSTIC_STDERR=$ZIPALIGN_DIAGNOSTIC_STDERR"
  rm -f "$OUT" "$ERR"
else
  echo "ZIPALIGN_DIAGNOSTIC_TOOL=NOT_AVAILABLE"
  echo "ZIPALIGN_DIAGNOSTIC_RC=NOT_RUN"
fi
