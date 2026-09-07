#!/usr/bin/env bash
set -u
ROOT="$(git rev-parse --show-toplevel)" || exit 1
cd "$ROOT" || exit 1
V="tools/gate2_canonical_source_build_boundary.sh"
EXPECTED="$(git rev-parse HEAD)"

run_expect_red() {
  label="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    echo "FAIL:$label"
    return 1
  fi
  echo "PASS:$label"
}

G2_EXPECT_SOURCE_COMMIT=0000000000000000000000000000000000000000 run_expect_red stale-source "$ROOT/$V"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cp "$V" "$TMP/validator.sh"
mkdir -p "$TMP/repo/artifacts/gates/g1" "$TMP/repo/gradle/wrapper" "$TMP/repo/app"
cd "$TMP/repo" || exit 1
git init -q -b master
git config user.email test@example.invalid
git config user.name G2-test
printf '%s\n' 'include(":app")' > settings.gradle
printf '%s\n' 'plugins { id "com.android.application" }' > build.gradle
printf '%s\n' '#!/bin/sh' 'exit 0' > gradlew
chmod +x gradlew
printf '%s\n' 'distributionUrl=https://services.gradle.org/distributions/gradle-9.2.1-bin.zip' > gradle/wrapper/gradle-wrapper.properties
printf '%s\n' 'android {' "    defaultConfig {" "        applicationId 'com.alfa.device_ctrl'" '    }' '}' > app/build.gradle
printf '%s\n' 'schema_version=g1-foundation-contract.v2' 'gate=G1' 'gate_status=GREEN' 'source_commit=PLACEHOLDER' > artifacts/gates/g1/foundation-contract.txt
git add . && git commit -q -m test
C="$(git rev-parse HEAD)"
sed -i "s/PLACEHOLDER/$C/" artifacts/gates/g1/foundation-contract.txt

git add artifacts/gates/g1/foundation-contract.txt && git commit -q -m evidence
C="$(git rev-parse HEAD)"

# Missing origin is allowed by the validator, but repository identity is not.
git remote add origin https://github.com/panglimanetizen01/Alfa_device_ctrl.git
G2_EXPECT_SOURCE_COMMIT=0000000000000000000000000000000000000000 run_expect_red temp-stale "$TMP/validator.sh"

printf '%s\n' 'PASS:G2 negative provenance checks complete'
