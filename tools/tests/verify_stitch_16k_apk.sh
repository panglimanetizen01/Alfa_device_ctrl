#!/usr/bin/env bash
set -euo pipefail

APK="${1:?usage: verify_stitch_16k_apk.sh <apk>}"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

unzip -q "$APK" 'lib/**/*.so' -d "$TMP"
READELF="${ANDROID_HOME:?}/ndk/28.0.13004108/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
mapfile -t LIBS < <(find "$TMP/lib" -type f -name '*.so' | sort)
test "${#LIBS[@]}" -gt 0

for so in "${LIBS[@]}"; do
  echo "ELF_CHECK=$so"
  "$READELF" -h "$so" | grep -E 'Class:.*ELF64|Machine:.*AArch64|Type:'
  TYPE=$("$READELF" -h "$so" | awk '$1 == "Type:" {print $2}')
  case "$TYPE" in
    DYN|EXEC)
      mapfile -t LOAD_ALIGNS < <("$READELF" -lW "$so" | awk '$1 == "LOAD" {print $NF}')
      test "${#LOAD_ALIGNS[@]}" -gt 0
      for align in "${LOAD_ALIGNS[@]}"; do
        echo "ELF_LOAD_ALIGN=$align"
        ALIGN_DEC=$(printf '%d' "$align")
        test "$ALIGN_DEC" -ge 16384
      done
      if [ "$TYPE" = "DYN" ]; then
        RELRO=$("$READELF" -lW "$so" | awk '$1 == "GNU_RELRO" {print $1; exit}')
        test "$RELRO" = "GNU_RELRO"
        echo "ELF_RELRO=PASS"
      else
        echo "ELF_RELRO=NOT_APPLICABLE_EXECUTABLE"
      fi
      ;;
    *) echo "UNEXPECTED_ELF_TYPE=$TYPE"; exit 1 ;;
  esac
done

"${ANDROID_HOME}/build-tools/35.0.0/zipalign" -c -P 16 -v 4 "$APK"
echo "APK_NATIVE_ELF_ALIGNMENT_PASS=16KB"
echo "APK_ZIP_ALIGNMENT_PASS=16KB"
