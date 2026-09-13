# Stitch v1 Native Surface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the supplied Stitch v1 artifact into a real native Alfa Device Ctrl surface without changing Alfa runtime/network/security architecture.

**Architecture:** Keep `AlfaFinalUiPresentation` as the terminal-first shell and make `AlfaStitchOperationalPanels` a real structured native surface layer. Keep execution in `RuntimeSessionManager`, network evidence in `AlfaNetworkPanel`, and use capability gates for unsupported multi-PTY/overlay/kernel features.

**Tech Stack:** Android Views, Java, existing Termux terminal renderer/session engine, JUnit/Robolectric-style source contracts, Android instrumentation tests, GitHub Actions.

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

**Files:**
- Modify: `tools/stitch_v1_forensic.py`
- Modify: `.github/workflows/stitch-forensic.yml`
- Test: `tools/stitch_v1_forensic.py` execution against the canonical ZIP

**Interfaces:**
- Produces deterministic JSON containing state IDs, headings, controls, image references, CSS tokens, font references, and per-asset SHA-256 values.
- Preserves existing exact ZIP SHA/count assertions.

- [ ] Step 1: Add extraction fields for image source paths, referenced image filenames, dimensions when available, and per-file SHA-256.
- [ ] Step 2: Add assertions that the source ZIP is unchanged and the expected 159/160/7/326 counts remain exact.
- [ ] Step 3: Extend the CI artifact output with the deterministic manifest.
- [ ] Step 4: Run the forensic script against the canonical ZIP and verify exit 0 and exact counts.
- [ ] Step 5: Commit the forensic tooling change.

### Task 2: Add failing native surface contract tests

**Files:**
- Modify: `app/src/test/java/com/alfa/device_ctrl/AlfaFinalUiPresentationContractTest.java`
- Create: `app/src/test/java/com/alfa/device_ctrl/StitchV1NativeSurfaceContractTest.java`

**Interfaces:**
- Tests inspect source-level contracts for real structured Stitch controls and capability-gated surfaces.

- [ ] Step 1: Add tests requiring session rows, spawn/stop controls, appearance preset controls, typography steppers, split controls, floating capability state, storage policy dialogs, and diagnostics evidence sections.
- [ ] Step 2: Run the targeted tests and verify they fail because the current panels are text-only.
- [ ] Step 3: Keep the failure output as the root-cause evidence.
- [ ] Step 4: Commit the failing tests before production UI implementation.

### Task 3: Implement native design primitives and Stitch operational surfaces

**Files:**
- Modify: `app/src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java`
- Modify: `app/src/main/java/com/alfa/device_ctrl/AlfaFinalUiPresentation.java` only where navigation/host integration is required.
- Modify: `app/src/main/java/com/alfa/device_ctrl/AlfaUiTheme.java` only where a missing locked token is required.

**Interfaces:**
- `AlfaStitchOperationalPanels.build(...)` remains the single entry point from the shell.
- Structured controls invoke only existing Alfa capabilities or explicit capability gates.

- [ ] Step 1: Implement reusable native card/header/chip/row/dialog builders using the locked Obsidian token set.
- [ ] Step 2: Replace security/storage/audit/project/session/split/floating/appearance/settings/diagnostic text consoles with structured native surfaces.
- [ ] Step 3: Implement session rows, active-session indicators, spawn/stop/switch affordances, and truthful max/backend capability messaging without fabricating PTYs.
- [ ] Step 4: Implement appearance presets, font-size and line-height steppers, cursor controls, scrollback and viewport settings using the existing persistence/terminal renderer boundary.
- [ ] Step 5: Implement storage-policy and project-browser dialogs through existing SAF/storage boundaries.
- [ ] Step 6: Implement split/floating surfaces as capability-gated native layouts; only create actual second PTY or overlay behavior when the existing backend proves it.
- [ ] Step 7: Add explicit accessibility descriptions to icon-only controls and preserve 48dp targets.
- [ ] Step 8: Run targeted unit tests and fix only failures attributable to this task.
- [ ] Step 9: Commit the native surface implementation.

### Task 4: Integrate network/security evidence without behavior changes

**Files:**
- Modify: `app/src/main/java/com/alfa/device_ctrl/AlfaNetworkPanel.java` only for missing Stitch presentation fields.
- Modify: `app/src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java`.
- Test: existing network/security contract tests plus new focused tests.

**Interfaces:**
- Android network evidence comes from `ConnectivityManager`, `NetworkCapabilities`, and `LinkProperties`.
- Runtime network evidence is shown only when produced by the selected runtime.

- [ ] Step 1: Add failing tests for DNS, routes, interface, proxy, VPN transport, validated state and capability labels being evidence-backed.
- [ ] Step 2: Run targeted tests and verify the failure.
- [ ] Step 3: Implement only presentation/evidence wiring; no privileged network mutation.
- [ ] Step 4: Run targeted tests and verify pass.
- [ ] Step 5: Commit.

### Task 5: Clean accidental forensic marker files and stabilize CI

**Files:**
- Delete: `tools/.stitch-forensic-trigger*` marker files proven to be accidental.
- Modify: `.github/workflows/stitch-forensic.yml`.

- [ ] Step 1: Fetch exact current blob SHAs for every accidental marker file.
- [ ] Step 2: Remove them in one controlled commit.
- [ ] Step 3: Ensure the forensic workflow runs only the canonical artifact audit and native verification jobs.
- [ ] Step 4: Commit.

### Task 6: Full verification and provenance gate

**Files:**
- Modify only if verification exposes a concrete defect.

- [ ] Step 1: Run full JVM/unit test suite in CI.
- [ ] Step 2: Run Android instrumentation tests in CI.
- [ ] Step 3: Verify APK provenance and Stitch ZIP SHA binding.
- [ ] Step 4: Verify the branch diff against `master` contains only scoped Stitch/native work and required forensic cleanup.
- [ ] Step 5: Verify all relevant GitHub Actions jobs are green from fresh runs.
- [ ] Step 6: Only after fresh CI evidence, synchronize to local Termux.
- [ ] Step 7: Run canonical local build/test commands with fresh evidence.
- [ ] Step 8: Install fresh APK on DUT and verify terminal, runtime/session, network evidence, settings, and capability-gated surfaces.
- [ ] Step 9: Record final evidence and declare PASS GREEN LOCKED only if every required gate is green.
