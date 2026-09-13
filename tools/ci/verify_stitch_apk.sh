#!/usr/bin/env bash
set -euo pipefail

APK="${1:?usage: verify_stitch_apk.sh <apk> [blacklist] }"
BLACKLIST="${2:-tools/ci/stitch-legacy-ui-blacklist.txt}"

[[ -f "$APK" && -s "$APK" ]]
[[ -f "$BLACKLIST" && -s "$BLACKLIST" ]]

python3 - "$APK" "$BLACKLIST" <<'PY'
import hashlib
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
