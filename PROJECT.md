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

`0b2d4f79cc01b2fdb9051fffc539ab9ab42ae187`

This baseline includes the merged G17 Android instrumentation execution proof, the canonical Alfa MCP server, the launcher manifest correction, and cleanup of tracked generated Gradle/build state. G18/G19 artifacts and branches may exist in repository history, but they MUST NOT be described as part of `master` until their commits are actually merged into `master` and verified there.

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
