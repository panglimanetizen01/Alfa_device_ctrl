# Alfa Device Ctrl — Runtime Security Model

## Scope

Alfa provides an interactive Linux compatibility runtime on Android. The runtime uses PRoot rather than an Android-rooted container or a kernel security sandbox.

This document records the security boundary so that compatibility behavior is not mistaken for isolation.

## Enforced controls

1. `HostStorageDeviceProofActivity` is internal-only (`android:exported=false`). It is no longer reachable through a custom external intent action.
2. The runtime does not bind the host `/sys` tree. `/dev` and `/proc` remain because the current PTY and process-evidence contracts depend on them.
3. Before a runtime PTY is created, Alfa calls Android's public `android.system.Os.prctl(PR_SET_NO_NEW_PRIVS, 1, ...)`. The kernel carries this bit across fork/clone/exec, so the PRoot process and its descendants inherit the boundary.
4. PRoot remains pinned to a specific upstream commit and its generated loader remains SHA-verified by the build pipeline.
5. Runtime rootfs downloads are HTTPS and SHA-256 verified before acceptance; Gate 7 binds the packaged runtime to current pipeline/source provenance.

## Deliberate residual capabilities

### PRoot root identity

`-0` provides guest-visible uid/gid 0. It does not grant Android root. PRoot is a compatibility layer based on ptrace and host-kernel resources, not a security sandbox.

### `/dev` and `/proc`

These are deliberately retained for PTY/device-compatible behavior and process evidence. They are a reduced surface compared with the previous `/dev + /proc + /sys` binding, not a claim of complete host isolation.

### Network

The application declares `INTERNET` and the runtime currently retains network capability for package-management and network-evidence use cases. No claim of network egress isolation is made. A future offline profile must use a separately verified enforcement mechanism; UI state or documentation alone is not an enforcement boundary.

### Rootfs authenticity

SHA-256 verification proves that the downloaded bytes equal the committed expected artifact digest. It does not provide an independent vendor signature. Release provenance therefore binds APK digest, source commit, pipeline provenance, runtime registry digest, and runtime artifact digests. This is an integrity/provenance control, not a replacement for signed vendor metadata.

## Acceptance requirements

Security acceptance requires both static CI evidence and dynamic DUT evidence. The DUT matrix must verify:

- exported-component access is denied from an external UID;
- `PR_GET_NO_NEW_PRIVS` reports enabled for the runtime process and its descendants;
- `/sys` is not exposed through Alfa's explicit PRoot bindings;
- `/dev` and `/proc` exposure is measured and recorded;
- DNS/TCP/HTTPS egress behavior is measured and classified against the intended network policy;
- PTY creation, prompt readiness, `pwd`, and terminal interaction still pass;
- timeout/process-group cleanup leaves no surviving runtime command descendants;
- APK provenance matches the tested source commit and artifact digest.

A static green build must never be presented as proof of these device-runtime properties.
