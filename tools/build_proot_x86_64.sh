#!/usr/bin/env bash
set -euo pipefail
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
PROOT_COMMIT="7266fb3e8516535682f5a9c8f3a7e70f6506eddb"
TALLOC_VERSION="2.4.3"
TALLOC_SHA256="dc46c40b9f46bb34dd97fe41f548b0e8b247b77a918576733c528e83abd854dd"
NDK_VERSION="28.0.13004108"
OUT_DIR="$ROOT/app/build/generated/jniLibs/x86_64"
EVIDENCE_DIR="${RUNNER_TEMP:-$ROOT/app/build/generated/runtime-artifact-evidence}"
WORK="$ROOT/.build/proot-x86_64/$PROOT_COMMIT"
TALLOC_WORK="$ROOT/.build/talloc-$TALLOC_VERSION"
mkdir -p "$EVIDENCE_DIR"
exec > >(tee "$EVIDENCE_DIR/build_proot_x86_64.log") 2>&1
: "${ANDROID_SDK_ROOT:=${ANDROID_HOME:-}}"
[ -n "$ANDROID_SDK_ROOT" ] || { echo 'PROOT_STATUS=BLOCKED'; echo 'PROOT_REASON=ANDROID_SDK_ROOT_MISSING'; exit 20; }
NDK="$ANDROID_SDK_ROOT/ndk/$NDK_VERSION"
[ -d "$NDK" ] || { echo 'PROOT_STATUS=BLOCKED'; echo "PROOT_REASON=NDK_MISSING:$NDK_VERSION"; exit 20; }
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
CC="$TOOLCHAIN/clang"; AR="$TOOLCHAIN/llvm-ar"; STRIP="$TOOLCHAIN/llvm-strip"
[ -x "$CC" ] || { echo 'PROOT_STATUS=BLOCKED'; echo 'PROOT_REASON=CLANG_MISSING'; exit 20; }
[ -x "$AR" ] || { echo 'PROOT_STATUS=BLOCKED'; echo 'PROOT_REASON=LLVM_AR_MISSING'; exit 20; }
[ -x "$STRIP" ] || { echo 'PROOT_STATUS=BLOCKED'; echo 'PROOT_REASON=LLVM_STRIP_MISSING'; exit 20; }
mkdir -p "$ROOT/.build" "$OUT_DIR"
TALLOC_ARCHIVE="$ROOT/.build/talloc-${TALLOC_VERSION}.tar.gz"
if [ ! -f "$TALLOC_ARCHIVE" ]; then curl -fsSL "https://www.samba.org/ftp/talloc/talloc-${TALLOC_VERSION}.tar.gz" -o "$TALLOC_ARCHIVE"; fi
test "$(sha256sum "$TALLOC_ARCHIVE" | awk '{print $1}')" = "$TALLOC_SHA256"
rm -rf "$TALLOC_WORK"; mkdir -p "$TALLOC_WORK"; tar -xzf "$TALLOC_ARCHIVE" -C "$TALLOC_WORK" --strip-components=1
TALLOC_PREFIX="$TALLOC_WORK/out"; cd "$TALLOC_WORK"
cat > cross-answers.txt <<'EOF'
Checking uname sysname type: "Linux"
Checking uname machine type: "dontcare"
Checking uname release type: "dontcare"
Checking uname version type: "dontcare"
Checking simple C program: OK
building library support: OK
Checking for large file support: OK
Checking for -D_FILE_OFFSET_BITS=64: OK
Checking for WORDS_BIGENDIAN: OK
Checking for C99 vsnprintf: OK
Checking for HAVE_SECURE_MKSTEMP: OK
rpath library support: OK
-Wl,--version-script support: FAIL
Checking correct behavior of strtoll: OK
Checking correct behavior of strptime: OK
Checking for HAVE_IFACE_GETIFADDRS: OK
Checking for HAVE_IFACE_IFCONF: OK
Checking for HAVE_IFACE_IFREQ: OK
Checking getconf LFS_CFLAGS: OK
Checking getconf large file support flags work: OK
Checking for large file support without additional flags: OK
Checking for working strptime: OK
Checking for HAVE_SHARED_MMAP: OK
Checking for HAVE_MREMAP: OK
Checking for HAVE_INCOHERENT_MMAP: OK
EOF
./configure --prefix="$TALLOC_PREFIX" --disable-rpath --disable-python --cross-compile --cross-answers=cross-answers.txt CC="$CC --target=x86_64-linux-android26" AR="$AR" RANLIB="$TOOLCHAIN/llvm-ranlib"
make -j2
make install
mkdir -p "$TALLOC_PREFIX/lib"
TALLOC_OBJECT="$(find bin/default -type f -name '*.o' -print | grep -E '/talloc([.]c)?([.]|$)' | sed -n '1p')"
if [ -s "$TALLOC_PREFIX/lib/libtalloc.a" ]; then
    :
elif [ -s bin/default/libtalloc.a ]; then
    cp bin/default/libtalloc.a "$TALLOC_PREFIX/lib/libtalloc.a"
elif [ -n "$TALLOC_OBJECT" ] && [ -s "$TALLOC_OBJECT" ]; then
    echo "TALLOC_STATIC_OBJECT=$TALLOC_OBJECT"
    rm -f "$TALLOC_PREFIX/lib/libtalloc.a"
    "$AR" rcs "$TALLOC_PREFIX/lib/libtalloc.a" "$TALLOC_OBJECT"
else
    echo 'PROOT_STATUS=BLOCKED'
    echo 'PROOT_REASON=TALLOC_STATIC_OBJECT_MISSING_AFTER_INSTALL'
    echo 'TALLOC_OBJECT_CANDIDATES:'
    find bin/default -type f -name '*.o' -print | sort || true
    exit 20
fi
test -s "$TALLOC_PREFIX/lib/libtalloc.a"
if [ ! -d "$WORK/.git" ]; then git clone --depth=1 https://github.com/termux/proot.git "$WORK"; fi
git -C "$WORK" fetch --depth=1 origin "$PROOT_COMMIT"; git -C "$WORK" sparse-checkout disable 2>/dev/null || true; git -C "$WORK" checkout --detach "$PROOT_COMMIT"
git -C "$WORK" cat-file -e "$PROOT_COMMIT:src/cli/cli.h"
SYSROOT="$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot"
export PKG_CONFIG_PATH="$TALLOC_PREFIX/lib/pkgconfig"; export CFLAGS="--target=x86_64-linux-android26 --sysroot=$SYSROOT -I$TALLOC_PREFIX/include"; export LDFLAGS="--target=x86_64-linux-android26 --sysroot=$SYSROOT -L$TALLOC_PREFIX/lib"; export CC="$CC"; export AR="$AR"; export RANLIB="$TOOLCHAIN/llvm-ranlib"; export STRIP="$STRIP"
make -C "$WORK/src" clean
CANONICAL_INCLUDE="$WORK/.canonical-include"
mkdir -p "$CANONICAL_INCLUDE/cli"
git -C "$WORK" show "$PROOT_COMMIT:src/cli/cli.h" > "$CANONICAL_INCLUDE/cli/cli.h"
test -s "$CANONICAL_INCLUDE/cli/cli.h"
test "$(git -C "$WORK" hash-object "$CANONICAL_INCLUDE/cli/cli.h")" = "$(git -C "$WORK" rev-parse "$PROOT_COMMIT:src/cli/cli.h")"
# The main PRoot engine needs bzero/strcmp/memset compatibility declarations on Android.
export CFLAGS="$CFLAGS -I$CANONICAL_INCLUDE -I$WORK/src -include strings.h"
export CPPFLAGS="$CFLAGS -D_GNU_SOURCE -include string.h"
ls -l "$CANONICAL_INCLUDE/cli/cli.h"
# Build both standalone loader variants first with a clean flag set. The upstream proot target embeds loader/loader-wrapped.o and loader/loader-m32-wrapped.o and otherwise compiles their source with the engine compatibility headers.
LOADER_CFLAGS="--target=x86_64-linux-android26 --sysroot=$SYSROOT -I$CANONICAL_INCLUDE -I$WORK/src"
LOADER_CPPFLAGS=""
make -C "$WORK/src" PROOT_WITH_LIBANDROID_SHMEM=true CC="$CC --target=x86_64-linux-android26 --sysroot=$SYSROOT" LD="$CC --target=x86_64-linux-android26 --sysroot=$SYSROOT" AR="$AR" RANLIB="$TOOLCHAIN/llvm-ranlib" STRIP="$STRIP" CFLAGS="$LOADER_CFLAGS" CPPFLAGS="$LOADER_CPPFLAGS"
# Now build the engine. The exported LDFLAGS stays an environment value so GNU make can append the upstream PRoot link requirements (-ltalloc and Android shmem) from its GNUmakefile.
make -C "$WORK/src" PROOT_WITH_LIBANDROID_SHMEM=true CC="$CC --target=x86_64-linux-android26 --sysroot=$SYSROOT" LD="$CC --target=x86_64-linux-android26 --sysroot=$SYSROOT" AR="$AR" RANLIB="$TOOLCHAIN/llvm-ranlib" STRIP="$STRIP" CFLAGS="$CFLAGS" CPPFLAGS="$CPPFLAGS"
install -m 0755 "$WORK/src/proot" "$OUT_DIR/libproot.so"; install -m 0755 "$WORK/src/loader/loader" "$OUT_DIR/libproot-loader.so"
file "$OUT_DIR/libproot.so" "$OUT_DIR/libproot-loader.so"
readelf -h "$OUT_DIR/libproot.so" | grep -E 'Class:|Machine:'; readelf -h "$OUT_DIR/libproot-loader.so" | grep -E 'Class:|Machine:'
ENGINE_SHA256="$(sha256sum "$OUT_DIR/libproot.so" | awk '{print $1}')"; LOADER_SHA256="$(sha256sum "$OUT_DIR/libproot-loader.so" | awk '{print $1}')"
printf 'ABI=x86_64\nPROOT_SHA256=%s\nLOADER_SHA256=%s\n' "$ENGINE_SHA256" "$LOADER_SHA256"
printf 'PROOT_STATUS=UNTRUSTED_ARTIFACTS_REQUIRES_REVIEW\nPROOT_REASON=ENGINE_SHA_REQUIRES_REVIEW\nPROOT_PATH=%s\n' "$OUT_DIR/libproot.so"
