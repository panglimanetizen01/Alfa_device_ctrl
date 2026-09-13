#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
APP="$ROOT/app/src/main/java/com/alfa/device_ctrl"
BLACKLIST="$ROOT/tools/ci/stitch-legacy-ui-blacklist.txt"

# Regression contract: the retired presentation layer must not be wired into the application.
! grep -Fq 'AlfaFinalUiPresentation' "$APP/AlfaApplication.java"

# The explicit blacklist is the single measurable UI-residue policy input.
test -s "$BLACKLIST"
grep -Fx 'ALFA::CTRL' "$BLACKLIST"
grep -Fx 'ALFA OS' "$BLACKLIST"
grep -Fx 'ALFA_OS' "$BLACKLIST"
grep -Fx 'ALFA PTY  •  INTERACTIVE SESSION' "$BLACKLIST"
grep -Fx '● ALFA::CTRL' "$BLACKLIST"

echo 'STITCH_UI_BOUNDARY_TDD=PASS'
