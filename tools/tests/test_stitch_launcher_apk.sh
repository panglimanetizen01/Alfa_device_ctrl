#!/usr/bin/env bash
set -euo pipefail

APK="${1:?usage: $0 <apk> [aapt2]}"
AAPT2="${2:-${ANDROID_HOME:-}/build-tools/35.0.0/aapt2}"

[ -f "$APK" ] || { echo "APK_NOT_FOUND=$APK"; exit 1; }
[ -x "$AAPT2" ] || { echo "AAPT2_NOT_FOUND=$AAPT2"; exit 1; }

BADGING="$($AAPT2 dump badging "$APK")"
printf '%s\n' "$BADGING" | grep -Fq "package: name='com.alfa.device_ctrl'"

LAUNCH_LINE="$(printf '%s\n' "$BADGING" | grep '^launchable-activity:' || true)"
case "$LAUNCH_LINE" in
  *"name='com.alfa.device_ctrl.StitchOperationalActivityV2'"*) ;;
  *)
    echo "STITCH_LAUNCHER=FAIL"
    echo "ACTUAL_LAUNCHABLE=${LAUNCH_LINE:-NONE}"
    exit 1
    ;;
esac

XMLTREE="$($AAPT2 dump xmltree "$APK" --file AndroidManifest.xml)"
export XMLTREE
python3 - <<'PY'
import os
import re

xml = os.environ["XMLTREE"]
lines = xml.splitlines()
blocks = []
current = []
for line in lines:
    if re.match(r"^\s*E: activity(?: |$)", line):
        if current:
            blocks.append("\n".join(current))
        current = [line]
    elif current:
        current.append(line)
if current:
    blocks.append("\n".join(current))

needle = "com.alfa.device_ctrl.StitchOperationalActivityV2"
stitch = next((b for b in blocks if needle in b), None)
if stitch is None:
    print("STITCH_ACTIVITY_IN_FINAL_MANIFEST=FAIL")
    raise SystemExit(1)

if not re.search(r"android:exported.*(?:0xffffffff|true)", stitch):
    print("STITCH_EXPORTED=FAIL")
    print(stitch)
    raise SystemExit(1)

if "android.intent.action.MAIN" not in stitch:
    print("STITCH_MAIN_ACTION=FAIL")
    raise SystemExit(1)

if "android.intent.category.LAUNCHER" not in stitch:
    print("STITCH_LAUNCHER_CATEGORY=FAIL")
    raise SystemExit(1)

print("STITCH_ACTIVITY_IN_FINAL_MANIFEST=PASS")
print("STITCH_EXPORTED=PASS")
print("STITCH_MAIN_LAUNCHER_MANIFEST=PASS")
PY

echo "STITCH_LAUNCHER=PASS"
