# Alfa Device Ctrl

## Canonical Source

The canonical source of truth for project source code is the GitHub repository:

`panglimanetizen01/Alfa_device_ctrl`

The Android/Termux working copy is the local execution workspace and MUST be synchronized from the canonical repository before source-level verification.

## Canonical Android Workspace

Project files, source, tests, documentation, patches, diagnostics, build artifacts, and other persistent project outputs on the device MUST remain under:

`/sdcard/Alfa_device_ctrl_HOST/Alfa_device_ctrl`

The Android shared-storage workspace is user-facing project storage. It is not assumed to be an executable staging area.

## Runtime Boundary

- `/sdcard/Alfa_device_ctrl_HOST/Alfa_device_ctrl` is the canonical project workspace.
- `/home/userland` is NOT a project workspace.
- Ubuntu UserLAnd is an execution environment only.
- Runtime executables MUST use the app-private/runtime-controlled staging boundary required by the active gate contracts.
- Shared storage is used for project data and user-facing I/O, not as a direct executable staging area.
- No untracked duplicate or mirror project tree may be treated as authoritative.

## Repository Status Baseline

The current `master` baseline is:

`0b7f3d45858f30ab5f21c69f0087b50f0ed5d5e2`

This baseline is the current forensic APK-readiness baseline. It includes the merged lifecycle/provenance and build-path hardening from PR46, rootfs archive symlink confinement with regression coverage, canonical runtime-artifact binding in the string installer API, and one dedicated APK readiness workflow using AGP 8.10 / Gradle 8.11.1 / API 36 / Build Tools 35 / NDK 28. The older overlapping APK CI workflow and redundant canonical-hardening workflow were removed. G18/G19 and later contract artifacts remain authoritative only when their exact source commit and execution evidence are verified.

## Evidence Rules

- Historical evidence is not fresh runtime evidence.
- A gate is PASS only when its contract is satisfied by evidence bound to the exact source commit under test.
- CI PASS proves the reported CI run for its exact commit; it does not prove a fresh local/device execution unless the workflow explicitly performs and records that execution.
- Runtime claims MUST identify their source commit and execution environment.
- Unknown, stale, malformed, or unverified evidence MUST NOT be promoted to PASS.

## Documentation State

This file describes repository/workspace policy and canonical state. Detailed gate specifications and runtime contracts live under `docs/`.

The historical statement `Implementation: NOT STARTED` is obsolete and MUST NOT be used as the current project status. Current readiness is determined by the active gate evidence and exact commit lineage.

## Project Principle

Alfa Device Ctrl is the active native Android device-control platform. Earlier Alfa Linux Platform work may inform requirements and tests, but previous implementation trees are not authoritative for this project.
