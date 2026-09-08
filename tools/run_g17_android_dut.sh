#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)
PACKAGE=${ALFA_ANDROID_PACKAGE:-com.alfa.device_ctrl}
RISH=${ALFA_RISH:-$HOME/.local/opt/rish/rish}
HEAD=$(git -C "$ROOT" rev-parse HEAD)
APP_APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="$ROOT/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
SHARED_ROOT=${ALFA_DUT_SHARED_ROOT:-$HOME/storage/shared}
STAGE="$SHARED_ROOT/.alfa-g17-dut-$HEAD"

printf '%s\n' "DUT_SOURCE_COMMIT=$HEAD"
test -x "$RISH"
test -x "$ROOT/gradlew"
test -d "$SHARED_ROOT"

ALFA_SOURCE_COMMIT="$HEAD" "$ROOT/gradlew" --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest

test -s "$APP_APK"
test -s "$TEST_APK"
printf '%s\n' "APP_APK=$APP_APK" "TEST_APK=$TEST_APK"
printf '%s  %s\n' "$(sha256sum "$APP_APK" | awk '{print $1}')" "$APP_APK"
printf '%s  %s\n' "$(sha256sum "$TEST_APK" | awk '{print $1}')" "$TEST_APK"

rm -rf "$STAGE"
mkdir -p "$STAGE"
cp -f "$APP_APK" "$STAGE/app-debug.apk"
cp -f "$TEST_APK" "$STAGE/app-debug-androidTest.apk"
test -s "$STAGE/app-debug.apk"
test -s "$STAGE/app-debug-androidTest.apk"

RISH_APPLICATION_ID=com.alfa.device_ctrl "$RISH" -c "pm install -r '$STAGE/app-debug.apk'"
RISH_APPLICATION_ID=com.alfa.device_ctrl "$RISH" -c "pm install -r -t '$STAGE/app-debug-androidTest.apk'"

RISH_APPLICATION_ID=com.alfa.device_ctrl "$RISH" -c "cmd package list packages -U '$PACKAGE'"
RISH_APPLICATION_ID=com.alfa.device_ctrl "$RISH" -c "pm path '$PACKAGE'"
RISH_APPLICATION_ID=com.alfa.device_ctrl "$RISH" -c "pm list instrumentation | grep 'target=$PACKAGE'"

ALFA_ANDROID_PACKAGE="$PACKAGE" "$ROOT/tools/test_gate17_live.sh"

rm -rf "$STAGE"
printf '%s\n' 'G17_DUT_STAGING_CLEANUP=PASS'