# Stitch v1 Runtime Target Forensic — 2026-09-13

## Scope

Target runtimes: Kali Linux ARM64, Ubuntu 24.04 ARM64, Alpine 3.24.1 ARM64.
Debian is explicitly out of scope for modification and remains the known-good reference path.

## GitHub evidence

Workflow: `non-debian-runtime-forensic.yml`
Run: `34755240400`

- Ubuntu ARM64 runtime: PASS
- Alpine ARM64 runtime: PASS
- Kali ARM64 runtime: PASS
- native PRoot build: PASS in all three jobs
- canonical rootfs download + SHA verification: PASS in all three jobs
- rootfs archive traversal/extraction checks: PASS in all three jobs
- interactive shell/package-manager/network evidence step: PASS in all three jobs
- evidence emission: PASS in all three jobs
- `DEBIAN_TOUCHED=false` remains the workflow contract

## Build failure isolated before this addendum

Native CI run `34755240389` failed at `:app:prepareGate7LaunchContract` because Gradle detected that `:app:mergeDebugAssets` consumed the generated Gate 7 asset without an explicit dependency.

The canonical build file was patched so `mergeDebugAssets` and `mergeReleaseAssets` explicitly depend on `prepareGate7LaunchContract`.

## Verification rule

The runtime forensic PASS above is not an APK/DUT PASS. The native CI must execute against the current implementation HEAD and complete full unit tests, debug APK packaging, source contracts, and provenance before the milestone can be declared GREEN LOCKED.
