#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
VERIFIER="$ROOT/tools/ci/verify_stitch_apk.sh"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$TMP/android/build-tools/35.0.0"
cat > "$TMP/android/build-tools/35.0.0/zipalign" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
chmod +x "$TMP/android/build-tools/35.0.0/zipalign"

cat > "$TMP/blacklist.txt" <<'EOF'
KNOWN_LEGACY_LITERAL_THAT_MUST_NOT_EXIST
EOF

python3 - "$TMP" <<'PY'
from pathlib import Path
import struct
import sys
import zipfile

root = Path(sys.argv[1])
valid = root / "valid.apk"
with zipfile.ZipFile(valid, "w", compression=zipfile.ZIP_DEFLATED) as z:
    z.writestr("classes.dex", "SYSTEM ONLINE LINUX RUNTIMES TERMINAL RUNTIMES AlfaStitchOperationalPanels StitchOperationalActivity")
    z.writestr("lib/arm64-v8a/libtermux.so", b"ELF-test-payload")

raw = bytearray(valid.read_bytes())
assert raw[:4] == b"PK\x03\x04"
invalid = root / "invalid.apk"
raw[0:4] = b"BAD!"
invalid.write_bytes(raw)
PY

VALID_OUT="$TMP/valid.out"
set +e
ANDROID_HOME="$TMP/android" bash "$VERIFIER" "$TMP/valid.apk" "$TMP/blacklist.txt" >"$VALID_OUT" 2>&1
VALID_RC=$?
set -e
if [ "$VALID_RC" -ne 0 ]; then
  cat "$VALID_OUT"
  echo "P7_VALID_HEADER_ACCEPTANCE=FAIL"
  exit 1
fi
if grep -Fq 'APK_ZIP_LOCAL_HEADER_INVALID' "$VALID_OUT"; then
  cat "$VALID_OUT"
  echo "P7_VALID_HEADER_ACCEPTANCE=FAIL"
  exit 1
fi

echo "P7_VALID_HEADER_ACCEPTANCE=PASS"

INVALID_OUT="$TMP/invalid.out"
set +e
ANDROID_HOME="$TMP/android" bash "$VERIFIER" "$TMP/invalid.apk" "$TMP/blacklist.txt" >"$INVALID_OUT" 2>&1
INVALID_RC=$?
set -e
if [ "$INVALID_RC" -eq 0 ]; then
  cat "$INVALID_OUT"
  echo "P7_MALFORMED_HEADER_REJECTION=FAIL"
  exit 1
fi

echo "P7_MALFORMED_HEADER_REJECTION=PASS"
echo "P7_ZIP_LOCAL_HEADER_REGRESSION=PASS"
