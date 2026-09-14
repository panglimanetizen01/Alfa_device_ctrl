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
  *"name='com.alfa.device_ctrl.StitchOperationalActivity'"*) ;;
  *)
    echo "STITCH_LAUNCHER=FAIL"
    echo "ACTUAL_LAUNCHABLE=${LAUNCH_LINE:-NONE}"
    exit 1
    ;;
esac

XMLTREE="$($AAPT2 dump xmltree "$APK" --file AndroidManifest.xml)"
ACTIVITY_BLOCK="$(printf '%s\n' "$XMLTREE" | awk '
  /E: activity / {
    if (block != "") print block
    block=$0 "\n"
    next
  }
  block != "" { block=block $0 "\n" }
  END { if (block != "") print block }
')"

STITCH_BLOCK="$(printf '%s\n' "$ACTIVITY_BLOCK" | awk '
  /android:name.*com\.alfa\.device_ctrl\.StitchOperationalActivity/ { print block; found=1; next }
  found { print; if ($0 ~ /^    E: activity /) exit }
  { block=block $0 "\n" }
')"

if ! printf '%s\n' "$STITCH_BLOCK" | grep -Eq 'android:exported.*(0xffffffff|true)'; then
  echo "STITCH_EXPORTED=FAIL"
  echo "$STITCH_BLOCK"
  exit 1
fi

printf '%s\n' "$XMLTREE" | grep -Fq 'android:name="android.intent.action.MAIN"' || {
  echo "MAIN_ACTION=FAIL"; exit 1;
}
printf '%s\n' "$XMLTREE" | grep -Fq 'android:name="android.intent.category.LAUNCHER"' || {
  echo "LAUNCHER_CATEGORY=FAIL"; exit 1;
}

echo "STITCH_LAUNCHER=PASS"
echo "STITCH_EXPORTED=PASS"
echo "STITCH_MAIN_LAUNCHER_MANIFEST=PASS"
