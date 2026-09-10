# Multi-Distro Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the Debian singleton runtime architecture with a verified multi-distro registry and selected-runtime flow.

**Architecture:** Immutable `RuntimeProfile` values are registered by `RuntimeRegistry`; UI, installer and session code consume a selected profile. Artifact verification and PRoot verification remain fail-closed.

**Tech Stack:** Android Java, Gradle/JUnit, PRoot, Android storage/session APIs.

**Spec:** `docs/superpowers/specs/2026-09-10-multi-distro-runtime-design.md`

## Global Constraints
- Android + Termux only for local work.
- Canonical repository: `panglimanetizen01/Alfa_device_ctrl`.
- No destructive Git operations.
- No hardcoded Debian dependency in production runtime selection.
- No distro is marked installable without pinned artifact identity.
- PRoot loader identity remains mandatory.
- Every Termux command requires the exact research prefix.

### Task 1: Runtime contract and registry
**Files:** `RuntimeProfile.java`, new `RuntimeRegistry.java`, registry tests.
- Write failing tests for immutable fields, unique IDs, lookup, and profile enumeration.
- Implement the value object and registry.
- Keep Debian as the first migrated profile.
- Run targeted tests and commit.

### Task 2: Migrate installer/session APIs
**Files:** `RuntimeInstaller.java`, `RuntimeSessionManager.java`, `InteractiveSessionContract.java`, related tests.
- Write failing tests proving selected runtime id/rootfs/prompt flow through installation and session startup.
- Replace static `RuntimeProfile` references with profile arguments.
- Preserve loader checksum verification.
- Run targeted tests and full unit tests.

### Task 3: Add pinned Ubuntu and Kali profiles
**Files:** registry/profile tests and profile metadata.
- Pin Ubuntu Base 24.04.4 ARM64 to Canonical SHA-256.
- Pin Kali NetHunter current minimal ARM64 rootfs to Kali SHA-256.
- Add capability declarations reflecting Rootless limitations.
- Verify profile metadata tests.

### Task 4: Add Alpine only after identity pinning
- Capture official Alpine 3.24.1 aarch64 Mini RootFS SHA-256.
- Add it as installable only after the identity is independently verified.

### Task 5: Runtime selector UI
**Files:** `MainActivity.java` and UI tests.
- Add actual runtime cards/selection.
- Selected profile controls install, status, terminal, htop/network/process and session prompt.
- No cosmetic-only distro names.

### Task 6: End-to-end verification
- Full Gradle test suite.
- Build APK.
- Verify APK SHA and package identity.
- Install on DUT.
- Exercise Debian, Ubuntu, Alpine and Kali where artifact provisioning permits.
- Verify PTY, shell command execution, storage bridge, network and process telemetry.
- Run CI and verify GitHub HEAD equals local HEAD.
- Only then evaluate release readiness.
