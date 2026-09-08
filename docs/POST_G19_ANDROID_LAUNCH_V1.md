# Post-G19 Android Launch Handoff V1

## Purpose

G19 opens the APK/UI phase but does not authorize an Android runtime session by itself. The Android runtime must receive a current-run Gate 6 bootstrap artifact before an interactive session may launch.

## Contract

1. A producer supplies `gate6-bootstrap.v1` for one explicit pipeline run.
2. Android imports that artifact through the Storage Access Framework using `ACTION_OPEN_DOCUMENT`.
3. Android accepts it only when:
   - `schema_version=gate6-bootstrap.v1`
   - `gate=gate6`
   - `gate_status=PASS`
   - `authorization_status=AUTHORIZED`
   - pipeline run, source commit, Gate 4 contract hash, profile hash, implementation commit, decision ID, and request ID are present and valid.
4. Android atomically publishes `runtime-vault/gate7-launch.properties` as `gate7-launch.v1`.
5. `InteractiveSessionContract` consumes that Gate 7 contract. A missing or malformed contract remains fail-closed.
6. PRoot execution still requires independently verified `READY.evidence`, packaged `libproot.so`, runtime rootfs, host cwd, and safe PRoot environment.

## Boundary

This handoff does not claim APK installation, host File Manager visibility, or runtime success. Those remain device-level acceptance evidence.

## Rationale

Android's Storage Access Framework provides user-mediated access to selected documents without broad storage permissions. The handoff therefore uses a `content://` document selected by the user rather than direct filesystem access to the project workspace.
