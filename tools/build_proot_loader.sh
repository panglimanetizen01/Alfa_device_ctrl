#!/usr/bin/env bash
set -euo pipefail
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
PROOT_COMMIT="7266fb3e8516535682f5a9c8f3a7e70f6506eddb"
NDK_VERSION="28.0.13004108"
TRUSTED_LOADER_SHA256_ARM64="1e0341759bb0776dbfe6afbad7dbd51b0eb3fc38d7ff1db321b8c036e1617e33"
TRUSTED_LOADER_SHA256_X86_64="bf3a874eb863a148c89ee453d78e2a17cfb1feba25f58a2d4cb5a94ba0c70a58"
WORK="$ROOT/.build/proot-loader/$PROOT_COMMIT"
: "${ANDROID_SDK_ROOT:=${ANDROID_HOME:-}}"
[ -n "$ANDROID_SDK_ROOT" ] || { echo 'LOADER_STATUS=BLOCKED'; echo 'LOADER_REASON=ANDROID_SDK_ROOT_MISSING'; exit 20; }
NDK="$ANDROID_SDK_ROOT/ndk/$NDK_VERSION"
[ -d "$NDK" ] || { echo 'LOADER_STATUS=BLOCKED'; echo "LOADER_REASON=NDK_MISSING:$NDK_VERSION"; exit 20; }
mkdir -p "$ROOT/.build"
if [ ! -d "$WORK/.git" ]; then rm -rf "$WORK"; git clone --filter=blob:none https://github.com/termux/proot.git "$WORK"; fi
git -C "$WORK" fetch --depth=1 origin "$PROOT_COMMIT"
git -C "$WORK" checkout --detach "$PROOT_COMMIT"
SYSROOT="$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot"
case "$(uname -m)" in
  aarch64|arm64) CLANG="${ALFA_NATIVE_CLANG:-$(command -v clang)}"; STRIP="${ALFA_NATIVE_STRIP:-$(command -v llvm-strip)}";;
  x86_64) TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"; CLANG="$TOOLCHAIN/clang"; STRIP="$TOOLCHAIN/llvm-strip";;
  *) echo 'LOADER_STATUS=BLOCKED'; echo "LOADER_REASON=UNSUPPORTED_BUILD_HOST:$(uname -m)"; exit 20;;
esac
[ -x "$CLANG" ] || { echo 'LOADER_STATUS=BLOCKED'; echo 'LOADER_REASON=NATIVE_CLANG_MISSING'; exit 20; }
[ -x "$STRIP" ] || { echo 'LOADER_STATUS=BLOCKED'; echo 'LOADER_REASON=NATIVE_LLVM_STRIP_MISSING'; exit 20; }
build_loader(){
  local abi target expected out cc loader_ldflags actual
  abi="$1"
  target="$2"
  expected="$3"
  out="$4"
  cc="$CLANG --target=$target --sysroot=$SYSROOT"
  loader_ldflags='-static -nostdlib -Wl,--build-id=none,--image-base=0x2000000000,-z,noexecstack'
  mkdir -p "$(dirname "$out")"
  make -C "$WORK/src" clean CC="$cc" LD="$cc" STRIP="$STRIP" LOADER_LDFLAGS="$loader_ldflags" loader/loader
  install -m 0755 "$WORK/src/loader/loader" "$out"
  file "$out"
  readelf -h "$out" | grep -E 'Class:|Machine:'
  actual="$(sha256sum "$out" | awk '{print $1}')"
  printf 'ABI=%s\nLOADER_SHA256=%s\n' "$abi" "$actual"
  if [ "$actual" != "$expected" ]; then echo 'LOADER_STATUS=BLOCKED'; echo "LOADER_REASON=TRUSTED_SHA256_MISMATCH:abi=$abi:expected=$expected:actual=$actual"; exit 21; fi
}
build_loader "arm64-v8a" "aarch64-linux-android26" "$TRUSTED_LOADER_SHA256_ARM64" "$ROOT/app/build/generated/jniLibs/arm64-v8a/libproot-loader.so"
build_loader "x86_64" "x86_64-linux-android26" "$TRUSTED_LOADER_SHA256_X86_64" "$ROOT/app/build/generated/jniLibs/x86_64/libproot-loader.so"
printf 'LOADER_STATUS=PASS\nPROOT_COMMIT=%s\nNDK_VERSION=%s\nBUILD_HOST=%s\n' "$PROOT_COMMIT" "$NDK_VERSION" "$(uname -m)"