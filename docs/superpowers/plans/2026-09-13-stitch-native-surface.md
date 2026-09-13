# Stitch v1 Native Surface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the supplied Stitch v1 artifact into a real native Alfa Device Ctrl surface without changing Alfa runtime/network/security architecture.

**Architecture:** Keep `AlfaFinalUiPresentation` as the terminal-first shell and make `AlfaStitchOperationalPanels` a real structured native surface layer. Keep execution in `RuntimeSessionManager`, network evidence in `AlfaNetworkPanel`, and use capability gates for unsupported multi-PTY/overlay/kernel features.

**Tech Stack:** Android Views, Java, existing Termux terminal renderer/session engine, JUnit/source contracts, Android instrumentation tests, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-13-stitch-native-surface-design.md`

## Global Constraints
- Stitch source artifact: `stitch/stitch_alfa_device_control_v1.0.0.zip` SHA-256 `7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d`.
- Exactly 159 supplied `code.html` states remain bound to the catalog.
- No reference HTML/JavaScript is executed by Alfa.
- Runtime/session behavior remains owned by `RuntimeSessionManager`.
- Network evidence remains read-only and evidence-backed.
- Host storage visibility remains SAF/device-layer evidence.
- Minimum interactive target is 48dp.
- No fake green telemetry or unsupported kernel/network/security claims.

---

### Task 1: Expand forensic asset/state extraction

**Files:** `tools/stitch_v1_forensic.py`, `.github/workflows/stitch-forensic.yml`

- [ ] Add image source paths, referenced image filenames, dimensions, and per-file SHA-256 to the deterministic manifest.
- [ ] Preserve exact ZIP SHA and 159/160/7/326 assertions.
- [ ] Publish the manifest as a CI artifact.
- [ ] Execute the forensic script and verify exit 0 with exact counts.

### Task 2: Add failing native surface contract tests

**Files:** `app/src/test/java/com/alfa/device_ctrl/StitchV1NativeSurfaceContractTest.java`

- [ ] Require session rows, spawn/stop/switch controls, appearance presets, typography steppers, split controls, floating capability state, storage/project surfaces, and diagnostics sections.
- [ ] Verify capability gates remain explicit.
- [ ] Run the targeted tests and preserve the failure as root-cause evidence.

### Task 3: Implement native design primitives and Stitch operational surfaces

**Files:** `app/src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java`, with `AlfaFinalUiPresentation.java` and `AlfaUiTheme.java` only if required for integration/tokens.

- [ ] Implement reusable native card/header/chip/row/dialog builders using the locked Obsidian tokens.
- [ ] Replace text-only security/storage/audit/project/session/split/floating/appearance/settings/diagnostic surfaces with structured native controls.
- [ ] Implement truthful session controls and capability gating without fabricating PTYs.
- [ ] Implement appearance presets, font/line-height controls, cursor, scrollback and viewport controls using existing persistence/terminal boundaries.
- [ ] Implement storage/project dialogs through existing SAF/storage boundaries.
- [ ] Implement split/floating layouts only within proven backend capability.
- [ ] Preserve 48dp targets and accessibility descriptions.
- [ ] Run targeted tests, then commit.

### Task 4: Integrate network/security evidence without behavior changes

**Files:** `app/src/main/java/com/alfa/device_ctrl/AlfaNetworkPanel.java`, `app/src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java`, focused tests.

- [ ] Test DNS, routes, interface, proxy, VPN transport and validated state as evidence-backed values.
- [ ] Implement presentation-only wiring using `ConnectivityManager`, `NetworkCapabilities`, and `LinkProperties`.
- [ ] Keep privileged mutation and unsupported kernel claims blocked.
- [ ] Run focused tests, then commit.

### Task 5: Clean accidental marker files and stabilize CI

**Files:** `tools/.stitch-forensic-trigger*`, `.github/workflows/stitch-forensic.yml`

- [ ] Fetch exact current blob SHAs for accidental marker files.
- [ ] Remove them in one controlled commit.
- [ ] Ensure forensic CI is deterministic and scoped.

### Task 6: Full verification and provenance gate

- [ ] Run full JVM/unit suite in CI.
- [ ] Run Android instrumentation suite in CI.
- [ ] Verify APK provenance and Stitch ZIP SHA binding.
- [ ] Verify branch diff against `master` is scoped.
- [ ] Verify fresh relevant GitHub Actions runs are green.
- [ ] Only after CI evidence, synchronize local Termux.
- [ ] Run fresh local build/test.
- [ ] Install fresh APK on DUT and verify terminal, runtime/session, network evidence, settings and capability-gated surfaces.
- [ ] Declare PASS GREEN LOCKED only after all gates have fresh evidence.
