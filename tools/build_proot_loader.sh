#!/usr/bin/env bash
# Build the pinned PRoot ARM64 external loader as a reproducible build input.
# The generated ELF is intentionally not stored in Git; the build copies it into
# app/src/main/jniLibs/arm64-v8a/libproot-loader.so before packaging the APK.
set -euo pipefail

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
PROOT_COMMIT="7266fb3e8516535682f5a9c8f3a7e70f6506eddb"
NDK_VERSION="28.0.13004108"
OUT="$ROOT/app/src/main/jniLibs/arm64-v8a/libproot-loader.so"
WORK="$ROOT/.build/proot-loader/$PROOT_COMMIT"

: "${ANDROID_SDK_ROOT:=${ANDROID_HOME:-}}"
[ -n "$ANDROID_SDK_ROOT" ] || { echo 'LOADER_STATUS=BLOCKED'; echo 'LOADER_REASON=ANDROID_SDK_ROOT_MISSING'; exit 20; }
NDK="$ANDROID_SDK_ROOT/ndk/$NDK_VERSION"
[ -d "$NDK" ] || { echo 'LOADER_STATUS=BLOCKED'; echo "LOADER_REASON=NDK_MISSING:$NDK_VERSION"; exit 20; }

mkdir -p "$ROOT/.build" "$ROOT/app/src/main/jniLibs/arm64-v8a"
if [ ! -d "$WORK/.git" ]; then
  rm -rf "$WORK"
  git clone --filter=blob:none https://github.com/termux/proot.git "$WORK"
fi
git -C "$WORK" fetch --depth=1 origin "$PROOT_COMMIT"
git -C "$WORK" checkout --detach "$PROOT_COMMIT"

TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
CC="$TOOLCHAIN/clang --target=aarch64-linux-android26"
make -C "$WORK/src" \
  CC="$CC" \
  LD="$CC" \
  STRIP="$TOOLCHAIN/llvm-strip" \
  loader/loader

install -m 0755 "$WORK/src/loader/loader" "$OUT"
file "$OUT"
readelf -h "$OUT" | grep -E 'Class:|Machine:'
printf 'LOADER_STATUS=PASS\n'
printf 'PROOT_COMMIT=%s\n' "$PROOT_COMMIT"
printf 'NDK_VERSION=%s\n' "$NDK_VERSION"
printf 'LOADER_SHA256=%s\n' "$(sha256sum "$OUT" | awk '{print $1}')"
printf 'LOADER_PATH=%s\n' "$OUT"
