# Stitch Multi-Runtime Forensic Baseline — 2026-09-15

## Canonical source

- Repository: `panglimanetizen01/Alfa_device_ctrl`
- Branch: `work/stitch-dut-canonical-hardening-v2`
- Baseline commit after approved plan and first contract correction: `6c6f8f54fa2f93a12d14d00b97b094cc31642007`
- Open PR: #79

## Stitch reference identity

Canonical artifact: `stitch/stitch_alfa_device_control_v1.0.0.zip`

Expected SHA-256:
`7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d`

Expected corpus:
- 159 `code.html`
- 160 PNG
- 7 Markdown
- 326 total files

These values are enforced by the repository forensic workflow. The visual workflow currently extracts reference screenshots but does not itself render the APK or perform APK-vs-reference image comparison.

## Launcher evidence

`app/src/main/AndroidManifest.xml` declares `StitchOperationalActivity` as the exported `MAIN`/`LAUNCHER` activity. `MainActivity` is not the declared operational launcher.

## Current operational architecture

- Operational UI owner: `StitchOperationalActivity`
- Runtime source of truth: `RuntimeRegistry`
- Runtime state model: `RuntimeUiState` / `StitchV1RuntimeStateMachine`
- Session execution: `RuntimeSessionManager` / `InteractiveSessionContract`
- Terminal surface: Termux `TerminalView` / `TerminalSession`
- Runtime provisioning: `RuntimeInstaller`
- PRoot/native launch boundary: application native library + runtime vault/rootfs

## Current build contract observations

`app/build.gradle` packages generated native libraries from `$buildDir/generated/jniLibs` and generated assets from `$buildDir/generated/assets`. `prepareProotLoader` runs before `preBuild`. `prepareGate7LaunchContract` is required for packaging and generated assets, and validates runtime provenance against `runtime/runtimes.v1.json`.

The APK is therefore not a plain Java-only build: generated native/runtime artifacts are part of the packaging chain and must be included in the final provenance audit.

## Current known failure corrected in this baseline

The preceding head failed `RuntimeKeepAliveOwnershipContractTest.serviceSourceUsesMultiOwnerLifecycle` because the test asserted source formatting rather than semantic ownership behavior. The production implementation uses a multi-owner `Set<RuntimeSessionManager>`, removes the manager, checks whether the set is empty, and stops the service only for the final owner. The corrected test uses whitespace-tolerant patterns while retaining the ownership invariants.

This commit changes only the test for that defect plus this forensic documentation/plan history; no production runtime ownership behavior is intentionally changed.

## Evidence status

### Proven / current evidence

- Canonical launcher is `StitchOperationalActivity`.
- Runtime registry architecture exists and is referenced by the operational surface.
- Real `TerminalView`/`TerminalSession` objects are present in the operational Activity.
- Runtime execution is routed through `RuntimeSessionManager` and `InteractiveSessionContract`.
- Four runtime identities are represented in the runtime registry/catalog architecture.
- Stitch reference corpus identity/counts are locked by forensic CI.
- Existing visual forensic workflow extracts reference images and hashes them.
- Existing CI has explicit APK readiness, native/ELF, runtime, UI, and non-Debian forensic workflows.

### NOT PROVEN at this baseline

- A successful APK build from commit `6c6f8f54fa2f93a12d14d00b97b094cc31642007`.
- Exact final APK SHA-256 for this commit.
- Exact packaged APK provenance for this commit.
- Installed APK identity on a DUT for this commit.
- DUT launch/execution for this commit.
- APK screenshot vs Stitch screenshot visual equivalence.
- Complete 159-state rendered UI equivalence.
- End-to-end verification of every live UI action.
- Independent real PTY/shell/stdin/stdout/stderr evidence for all four distributions.
- Independent Ubuntu, Alpine, and Kali DUT execution evidence.
- Packet-level DNS/TCP evidence through the Android→PRoot→kernel boundary.
- Complete proof that every currently present legacy resource/build path is dead.

## Gate rule

No final PASS is permitted while any required item above remains NOT PROVEN. In particular, source-level correctness, unit-test success, CI success, or APK build success cannot substitute for exact APK installation and real DUT evidence.
