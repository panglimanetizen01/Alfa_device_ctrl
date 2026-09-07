# Multi-Distro Linux Runtime Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert Alfa Device Ctrl from an Ubuntu-oriented runtime implementation with mixed gate evidence into a distribution-neutral Android-hosted Linux userspace foundation whose G1–G19 verification is strictly sequential and whose first post-gate acceptance target is Debian.

**Architecture:** The Android APK owns the runtime lifecycle. A packaged ARM64 PRoot engine executes verified Linux rootfs guests selected by data-driven runtime descriptors. Gate evidence is source-commit-bound and fail-closed; distro acceptance is a separate milestone after G19.

**Tech Stack:** Android Java, Gradle/Android SDK, packaged ARM64 PRoot, shell/Python contract validators, JSON runtime registry, GitHub Actions evidence.

**Spec:** `docs/MULTI_DISTRO_RUNTIME_FOUNDATION_V1.md`

## Global Constraints

- The product runtime is the real Android device; UserLAnd is never a product runtime dependency.
- Linux distributions are guest userspaces, not execution engines.
- The runtime engine must remain distribution-neutral.
- Rootfs artifacts require immutable SHA-256 verification before publication.
- Runtime READY is evidence-backed and requires a real guest command.
- G1–G19 are strictly sequential; unresolved evidence stops progression.
- Evidence from another source commit is stale and cannot satisfy a current gate.
- UNKNOWN is never PASS.
- No APK release/install occurs before G1–G19 are GREEN and Debian acceptance is GREEN.
- Acceptance order is Debian → Ubuntu → Alpine → Kali → Generic Rootfs.

---

### Task 1: G1 Foundation Contract

**Files:**
- Create: `docs/MULTI_DISTRO_RUNTIME_FOUNDATION_V1.md`
- Modify: `docs/ARCHITECTURE.md`
- Create: `runtime/runtimes.v1.json`
- Create: `tools/gate1_foundation_v2.sh`
- Test: `tools/gate1_foundation_v2.sh`

**Interfaces:**
- Consumes: architecture document, foundation specification, runtime registry, current Git HEAD.
- Produces: `artifacts/gates/g1/foundation-contract.txt` with exact source commit and G1 status.

- [x] **Step 1: Define the distribution-neutral architecture contract.**
- [x] **Step 2: Define the runtime descriptor/registry contract.**
- [x] **Step 3: Implement the machine-checkable G1 validator.**
- [ ] **Step 4: Execute the validator from the canonical checkout.**
- [ ] **Step 5: Require GitHub Actions evidence for the exact commit.**
- [ ] **Step 6: Mark G1 GREEN only after both canonical and device-side evidence agree.**

---

### Task 2: G2 Canonical Source / Build Boundary

**Files:**
- Modify: existing canonical source/build verification tooling.
- Test: G2 contract test.

- [ ] Write the failing provenance/build-boundary test.
- [ ] Verify it rejects stale source/build identity.
- [ ] Implement the minimal correction.
- [ ] Run canonical CI.
- [ ] Run the device-side G2 verification.

**Stop condition:** Do not touch G3 until G2 is GREEN.

---

### Task 3: G3 Android Host Capability Evidence

**Files:**
- Modify: existing EDE/CDE/Gate3 implementation only where the contract is demonstrably incomplete.
- Test: capability contract tests.

- [ ] Verify each capability's status independently.
- [ ] Ensure aggregate PASS cannot hide capability ERROR/UNKNOWN.
- [ ] Produce source-bound evidence.

**Stop condition:** Do not touch G4 until G3 is GREEN.

---

### Task 4: G4 Environment Contract

**Files:**
- Modify: `tools/environment_contract.sh` and associated tests only when required by the G4 contract.
- Test: G4 regression tests.

- [ ] Validate environment identity normalization.
- [ ] Preserve legitimate UNKNOWN identity.
- [ ] Verify profile capability values remain atomic (`PASS`, `ERROR`, `UNKNOWN`).
- [ ] Produce fresh source-bound G4 evidence.

**Stop condition:** Do not touch G5 until G4 is GREEN.

---

### Task 5: G5 Decision / Authorization

**Files:**
- Existing Gate 5 decision/authorization tooling and selftest.
- Test: positive and negative authorization cases.

- [ ] Re-run positive path against current G4.
- [ ] Re-run all negative mutations against current provenance.
- [ ] Reject stale/missing/mutated evidence.

**Stop condition:** Do not touch G6 until G5 is GREEN.

---

### Task 6: G6 Runtime Bootstrap

**Files:** existing runtime bootstrap/session implementation.

- [ ] Verify current-run identity propagation.
- [ ] Verify runtime bootstrap consumes only current Gate 4/5 artifacts.
- [ ] Produce fresh G6 evidence.

**Stop condition:** Do not touch G7 until G6 is GREEN.

---

### Task 7: G7 Task Construction

**Files:** existing G7 implementation/spec/tests.

- [ ] Replace propagation-only behavior if contract requires actual task construction.
- [ ] Add evidence proving task semantics, not merely file existence.
- [ ] Verify current-run provenance.

**Stop condition:** Do not touch G8 until G7 is GREEN.

---

### Task 8: G8 Action Construction

**Files:** existing G8 implementation/spec/tests.

- [ ] Prove action formation from G7 output.
- [ ] Reject missing or stale G7 evidence.
- [ ] Produce fresh G8 evidence.

**Stop condition:** Do not touch G9 until G8 is GREEN.

---

### Task 9: G9 Workflow Construction

**Files:** existing G9 implementation/spec/tests.

- [ ] Prove workflow formation from G8 output.
- [ ] Reject propagation-only false PASS.
- [ ] Produce fresh G9 evidence.

**Stop condition:** Do not touch G10 until G9 is GREEN.

---

### Task 10: G10 Orchestrator

**Files:** existing G10 implementation/spec/tests.

- [ ] Prove actual orchestration semantics.
- [ ] Verify current-run identity.
- [ ] Produce fresh G10 evidence.

**Stop condition:** Do not touch G11 until G10 is GREEN.

---

### Task 11: G11 Kernel / Runtime Command Preparation

**Files:** existing G11 implementation/spec/tests.

- [ ] Prove kernel/runtime command preparation is substantive.
- [ ] Produce fresh evidence.

**Stop condition:** Do not touch G12 until G11 is GREEN.

---

### Task 12: G12 Command Request

**Files:** existing G12 implementation/spec/tests.

- [ ] Prove request creation from G11 output.
- [ ] Bind request to current run/source/contract identity.

**Stop condition:** Do not touch G13 until G12 is GREEN.

---

### Task 13: G13 Concrete `pwd` Request

**Files:** existing G13 implementation/spec/tests.

- [ ] Produce a concrete `pwd` request artifact.
- [ ] Keep execution responsibility in G17.
- [ ] Ensure evidence clearly says REQUEST, not EXECUTED.

**Stop condition:** Do not touch G14 until G13 is GREEN.

---

### Task 14: G14 Validation

**Files:** existing G14 implementation/spec/tests.

- [ ] Require valid G13 input.
- [ ] Validate exact command contract.
- [ ] Reject missing/stale provenance.

**Stop condition:** Do not touch G15 until G14 is GREEN.

---

### Task 15: G15 Policy

**Files:** existing G15 implementation/spec/tests.

- [ ] Prove policy evaluation.
- [ ] Reject policy bypass and stale evidence.

**Stop condition:** Do not touch G16 until G15 is GREEN.

---

### Task 16: G16 Authorization

**Files:** existing G16 implementation/spec/tests.

- [ ] Bind authorization to current Gate 5 authorization and all upstream identities.
- [ ] Reject cross-run authorization.

**Stop condition:** Do not touch G17 until G16 is GREEN.

---

### Task 17: G17 Real Command Execution

**Files:** existing G17 runtime execution implementation/spec/tests.

- [ ] Execute `pwd` in the real Linux guest runtime.
- [ ] Capture return code and stdout/stderr.
- [ ] Prove execution path is the guest runtime, not host shell.
- [ ] Produce immutable execution evidence.

**Stop condition:** Do not touch G18 until G17 is GREEN.

---

### Task 18: G18 Result Normalization

**Files:** existing G18 implementation/spec/tests.

- [ ] Normalize actual execution evidence without losing provenance.
- [ ] Reject malformed/missing execution results.

**Stop condition:** Do not touch G19 until G18 is GREEN.

---

### Task 19: G19 Evidence Consumption

**Files:** existing G19 implementation/spec/tests.

- [ ] Consume only a complete G18 chain.
- [ ] Verify all source/run/contract identities.
- [ ] Emit final acceptance evidence.

**Stop condition:** Only after G19 GREEN may Linux Userspace Acceptance begin.

---

### Task 20: Debian Linux Userspace Acceptance

**Files:**
- Modify: runtime registry/descriptor with the verified Debian artifact.
- Modify: generic runtime installer/session code only if device evidence exposes a root cause.
- Test: device acceptance script/evidence collector.

- [ ] Select a current Debian ARM64 rootfs from an attributable primary source.
- [ ] Verify artifact checksum and architecture.
- [ ] Install through the generic runtime engine.
- [ ] Execute real guest commands: `id`, `uname`, `pwd`, `cat /etc/os-release`, `/bin/sh`.
- [ ] Capture source/APK/rootfs/runtime/session provenance.
- [ ] Mark Debian GREEN only from real-device evidence.

---

### Task 21: Ubuntu / Alpine / Kali / Generic Acceptance

- [ ] Repeat the exact acceptance protocol for Ubuntu.
- [ ] Repeat for Alpine.
- [ ] Repeat for Kali.
- [ ] Repeat for a generic compatible rootfs.
- [ ] Do not add distro-specific execution code merely to pass an individual distro.
