#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
APP="$ROOT/app/src/main/java/com/alfa/device_ctrl"
BLACKLIST="$ROOT/tools/ci/stitch-legacy-ui-blacklist.txt"

# Regression contract: the retired presentation layer must not be wired into the application.
! grep -Fq 'AlfaFinalUiPresentation' "$APP/AlfaApplication.java"

# The explicit blacklist is the single measurable UI-residue policy input.
test -s "$BLACKLIST"
legacy_header='ALFA DEVICE CTRL'
legacy_os='ALFA'' OS'
legacy_os_id='ALFA''_OS'
legacy_session='ALFA PTY  •''  INTERACTIVE SESSION'
legacy_badge='● ALFA DEVICE CTRL'
grep -Fx "$legacy_header" "$BLACKLIST"
grep -Fx "$legacy_os" "$BLACKLIST"
grep -Fx "$legacy_os_id" "$BLACKLIST"
grep -Fx "$legacy_session" "$BLACKLIST"
grep -Fx "$legacy_badge" "$BLACKLIST"

echo 'STITCH_UI_BOUNDARY_TDD=PASS'
