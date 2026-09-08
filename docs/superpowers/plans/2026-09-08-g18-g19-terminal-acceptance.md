# G18-G19 Terminal Acceptance Implementation Plan

**Goal:** Make G18 and G19 strict, provenance-preserving, fail-closed terminal acceptance gates without changing locked G1-G17, and emit an explicit contract opening the local APK/UI build phase.

**Architecture:** G18 normalizes authoritative G17 execution evidence while preserving provenance. G19 validates the complete current-run G18 result, never re-executes the command, emits terminal acceptance evidence, and unlocks the next APK/UI build phase only on valid PASS.

**Tech Stack:** Bash, SHA-256 evidence artifacts, GitHub Actions.

**Global constraints:** GitHub master is canonical; local Termux is verification only. G1-G17 implementation is immutable. Current-run/path-explicit evidence only. Missing, stale, malformed, cross-run, hash-mismatched, or contradictory evidence is BLOCKED. G18/G19 never execute `pwd` or grant execution authority.

### Task 1 — Strengthen G18 provenance
- Preserve G17 execution/request identity, command hash, result hash, authorization linkage, command result, and execution path.
- Add malformed, cross-run, and hash-mismatch contract cases.
- Update G18 specification state.
- Run the strict G18 contract test.

### Task 2 — Implement strict G19 consumer
- Add a dedicated consumer implementation and strict contract test.
- Validate G18 schema, gate identity, current-run identity, provenance, result hash, and PASS state.
- Emit `gate19-artifact.v1`, `consume_status=ACCEPTED`, and `next_phase=APK_BUILD_AND_UI` / `next_phase_status=UNLOCKED` only when valid.
- Add negative cases for malformed, stale, cross-run, provenance mismatch, and non-PASS evidence.

### Task 3 — Canonical CI proof
- Add G19 workflow with exact-source verification and G1-G17 protection.
- Run G18 and G19 strict tests on the exact commit.
- Verify no G1-G17 implementation changes.

### Task 4 — Merge and local handoff
- Verify final GitHub Actions evidence on the final commit.
- Merge only after G18 and G19 are objectively green.
- Only then request one local Termux verification command for the canonical build boundary.
