#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
APP="$ROOT/app/src/main/java/com/alfa/device_ctrl"
BLACKLIST="$ROOT/tools/ci/stitch-legacy-ui-blacklist.txt"

# Regression contract: the retired presentation layer must not be wired into the application.
! grep -Fq 'AlfaFinalUiPresentation' "$APP/AlfaApplication.java"

# Regression contract: Stitch NODES hierarchy is window -> content -> ScrollView -> list.
grep -Fq 'LinearLayout content=(LinearLayout)win.getChildAt(1);ScrollView scroll=(ScrollView)content.getChildAt(0);LinearLayout list=(LinearLayout)scroll.getChildAt(0);' "$APP/StitchOperationalActivityV2.java"
! grep -Fq 'ScrollView scroll=(ScrollView)win.getChildAt(1);' "$APP/StitchOperationalActivityV2.java"

# The explicit blacklist is the single measurable UI-residue policy input.
test -s "$BLACKLIST"
legacy_os='ALFA'' OS'
legacy_os_id='ALFA''_OS'
legacy_session='ALFA PTY  •''  INTERACTIVE SESSION'
grep -Fx "$legacy_os" "$BLACKLIST"
grep -Fx "$legacy_os_id" "$BLACKLIST"
grep -Fx "$legacy_session" "$BLACKLIST"

echo 'STITCH_UI_BOUNDARY_TDD=PASS'
