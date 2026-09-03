#!/usr/bin/env bash
# Alfa Device Ctrl workspace environment.
# Master source and all tool caches remain outside the runtime home.

ALFA_MASTER_ROOT="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd)"
ALFA_SDK_ROOT="/storage/emulated/0/UserLAnd_home_migration_20260824_120427/.local/android-sdk"
ALFA_XDG_ROOT="$ALFA_MASTER_ROOT/.xdg"

mkdir -p "$ALFA_MASTER_ROOT/.user-home" \
  "$ALFA_MASTER_ROOT/.android-user-home" \
  "$ALFA_MASTER_ROOT/.gradle-user-home" \
  "$ALFA_XDG_ROOT/config" "$ALFA_XDG_ROOT/cache" "$ALFA_XDG_ROOT/data" \
  "$ALFA_XDG_ROOT/state" "$ALFA_XDG_ROOT/runtime" "$ALFA_MASTER_ROOT/.tmp"
chmod 700 "$ALFA_XDG_ROOT/runtime" 2>/dev/null || true

export ALFA_MASTER_ROOT
export HOME="$ALFA_MASTER_ROOT/.user-home"
export ANDROID_HOME="$ALFA_SDK_ROOT"
export ANDROID_SDK_ROOT="$ALFA_SDK_ROOT"
export ANDROID_USER_HOME="$ALFA_MASTER_ROOT/.android-user-home"
export GRADLE_USER_HOME="$ALFA_MASTER_ROOT/.gradle-user-home"
export XDG_CONFIG_HOME="$ALFA_XDG_ROOT/config"
export XDG_CACHE_HOME="$ALFA_XDG_ROOT/cache"
export XDG_DATA_HOME="$ALFA_XDG_ROOT/data"
export XDG_STATE_HOME="$ALFA_XDG_ROOT/state"
export XDG_RUNTIME_DIR="$ALFA_XDG_ROOT/runtime"
export TMPDIR="$ALFA_MASTER_ROOT/.tmp"
export TMP="$TMPDIR"
export TEMP="$TMPDIR"
export HISTFILE=/dev/null
case " ${JAVA_TOOL_OPTIONS:-} " in
  *" -Duser.home=$HOME "*) ;;
  *) export JAVA_TOOL_OPTIONS="-Duser.home=$HOME${JAVA_TOOL_OPTIONS:+ $JAVA_TOOL_OPTIONS}" ;;
esac
case ":${PATH:-}:" in
  *":$ALFA_SDK_ROOT/platform-tools:"*) ;;
  *) export PATH="$ALFA_SDK_ROOT/platform-tools:$ALFA_SDK_ROOT/cmdline-tools/latest/bin:$ALFA_SDK_ROOT/build-tools/35.0.1:${PATH:-/usr/local/sbin:/usr/local/bin:/usr/sbin:/sbin:/bin}" ;;
esac
