# Alfa Device Ctrl — G2 Canonical Source / Build Boundary Contract v1

## Status
LOCKED verification contract.

## Responsibility
G2 proves that the project being verified is the canonical repository state and that the build boundary is attributable to that exact source revision.

G2 is a provenance/integrity gate. It does not prove Linux guest execution and it must not build or install an APK.

## Required invariants

1. Repository identity is `panglimanetizen01/Alfa_device_ctrl`.
2. Verification runs from the canonical `master` checkout.
3. `HEAD` is a complete 40-character Git commit ID.
4. The working tree is clean apart from ignored generated G2 evidence.
5. Git object integrity passes `git fsck --full`.
6. `HEAD` resolves to the canonical `origin/master` revision when the remote is available.
7. Required build boundary files exist: `settings.gradle`, `build.gradle`, `gradlew`, `gradle/wrapper/gradle-wrapper.properties`, `app/build.gradle`.
8. The Gradle Wrapper distribution URL is explicit and pinned to the repository's declared version.
9. The Android application identity is declared by `applicationId` and is not inferred from generated output.
10. The source tree identity is reproducible from the Git revision using a deterministic path/content hash.
11. Evidence records the exact source commit and deterministic source-tree hash.
12. Historical evidence from another source commit is rejected.
13. UNKNOWN or missing provenance is not PASS.
14. APK build/install is outside G2 and forbidden before G19.

## Gradle Wrapper executable semantics

The canonical executable requirement is evaluated from the Git index/tree mode, not from the mounted working-tree POSIX mode. `gradlew` MUST be recorded by Git as mode `100755`.

This distinction is mandatory for Android shared/external-storage development environments: the working-tree mount may expose a tracked file without a usable POSIX execute bit even though the canonical Git tree correctly records `100755`. Such a mount-level permission difference is not a source-provenance failure and must not make G2 RED.

A Git mode other than `100755` is a canonical source defect and MUST make G2 RED.

## Build transformation boundary

A CI build may require a deterministic, declared runner-only transformation for externally built runtime components. Such a transformation is not part of the canonical source tree and must never be silently presented as canonical source.

If runner transformation is used, CI evidence MUST record:

- source commit
- transformation mode
- exact changed paths
- external source commit(s)
- generated artifact SHA-256
- toolchain identity

Unexpected changed paths fail the boundary.

## Evidence

G2 emits:

`artifacts/gates/g2/canonical-source-build-boundary.txt`

Required fields:

- `schema_version=g2-canonical-source-build-boundary.v1`
- `gate=G2`
- `gate_status=GREEN`
- `source_commit=<40 hex chars>`
- `repository=panglimanetizen01/Alfa_device_ctrl`
- `branch=master`
- `source_tree_sha256=<64 hex chars>`
- `git_object_integrity=PASS`
- `worktree_clean=PASS`
- `build_boundary=PASS`
- `apk_release_before_g19=FORBIDDEN`

The evidence is local verification evidence and must remain ignored by Git.

## Negative requirements

The G2 validator must fail for:

- stale source commit supplied to the validator;
- missing required build-boundary files;
- invalid Gradle Wrapper configuration;
- dirty unignored worktree;
- Git object corruption/failure;
- missing or malformed source identity;
- repository identity mismatch;
- canonical `gradlew` Git mode not equal to `100755`.

## Gate dependency

G2 may be a PASS candidate only when G1 is GREEN for the same source commit. G3 is blocked until G2 is GREEN.
