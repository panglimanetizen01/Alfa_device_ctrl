# G17 Real Runtime Execution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current host-directory `pwd` implementation of G17 with an explicit PRoot guest-rootfs execution boundary while preserving exact G16 authorization and fail-closed provenance.

**Architecture:** G17 remains a dedicated downstream execution boundary. It consumes only the current-run G16 authorization artifact, resolves an explicitly supplied PRoot executable and guest rootfs, executes only `/bin/sh -c pwd` under PRoot with `-r` and `-w /root`, and records guest/runtime/process evidence. G18/G19 remain consumers and are not allowed to execute or reinterpret the command.

**Tech Stack:** Bash, PRoot/ptrace, Linux `/proc`, SHA-256 provenance, GitHub Actions, Android arm64 runtime artifacts.

**Spec:** `docs/GATE_17_SPEC_V1.md` and `docs/GATE_17_RESULT_SCHEMA_V1.md`

## Global Constraints

- G1-G16 are locked baselines and must not be modified.
- G17 executes only the exact authorized `pwd` request with `POSIX_PWD` semantics.
- Missing, stale, cross-run, malformed, or hash-mismatched authorization blocks execution.
- PRoot executable and guest rootfs are explicit inputs; no latest/timestamp selection is permitted.
- Host `pwd` output is never accepted as guest execution evidence.
- Objective evidence must distinguish engine, rootfs, process PID, guest cwd, and guest result.
- APK build/install remains forbidden until G19 is green.

---

### Task 1: Prove the existing G17 defect with a failing contract

**Files:**
- Modify: `tools/test_gate17_contract.sh`

**Interfaces:**
- The test must reject an implementation whose `command_result` equals the repository root.
- The test must require explicit runtime engine/rootfs execution inputs.

- [ ] **Step 1: Replace the positive assertion that equates guest output to `$ROOT` with assertions requiring guest runtime evidence.**
- [ ] **Step 2: Run the contract and confirm the current implementation fails the new guest-boundary assertions.**

### Task 2: Implement the minimal PRoot G17 execution boundary

**Files:**
- Modify: `tools/gate17_runtime_execution.sh`
- Modify: `docs/GATE_17_RESULT_SCHEMA_V1.md`

**Interfaces:**
- Invocation: `gate17_runtime_execution.sh RUN_ID G16_AUTH OUTPUT PROOT_EXEC ROOTFS`
- Execution: `PROOT_EXEC -r ROOTFS -w /root /bin/sh -c pwd`
- Output evidence includes `engine_path`, `rootfs_path`, `pid`, `guest_cwd`, `guest_identity`, and `command_result`.

- [ ] **Step 1: Validate executable and rootfs are regular/usable explicit paths.**
- [ ] **Step 2: Preserve all current G16 identity/hash checks.**
- [ ] **Step 3: Execute PRoot with fixed argv and capture real stdout, stderr, and return code.**
- [ ] **Step 4: Capture process identity while the guest command is alive; do not infer PID from output.**
- [ ] **Step 5: Validate guest result is `/root` (or the exact configured guest cwd), not `$ROOT`.**
- [ ] **Step 6: Publish evidence atomically only after all checks pass.**

### Task 3: Strengthen negative and provenance tests

**Files:**
- Modify: `tools/test_gate17_contract.sh`

**Interfaces:**
- Negative cases must cover wrong engine, missing rootfs, stale G16, wrong run, wrong command, wrong semantics, non-DEFERRED state, wrong authority, and artifact hash mismatch.

- [ ] **Step 1: Add deterministic missing-engine and missing-rootfs rejection.**
- [ ] **Step 2: Add a rootfs identity mismatch rejection case.**
- [ ] **Step 3: Verify G1-G16 files remain present and unmodified by the G17 implementation contract.**

### Task 4: Align live verifier with real runtime execution

**Files:**
- Modify: `tools/test_gate17_live.sh`
- Modify: `.github/workflows/g17-runtime-execution-contract.yml`

**Interfaces:**
- Hosted CI may validate contract structure but must not claim Android DUT evidence.
- Live verification must require explicit PRoot/rootfs inputs and record whether the environment is a real runtime-capable target.

- [ ] **Step 1: Remove the host `$ROOT` positive-execution assertion.**
- [ ] **Step 2: Require PRoot/rootfs evidence before accepting execution.**
- [ ] **Step 3: Keep hosted CI explicitly non-DUT and fail closed when real runtime inputs are unavailable.**

### Task 5: Verify and review

**Files:**
- Test: `tools/test_gate17_contract.sh`
- Test: `tools/test_gate17_live.sh`
- Test: `.github/workflows/g17-runtime-execution-contract.yml`

- [ ] **Step 1: Run static shell validation.**
- [ ] **Step 2: Run G17 contract tests.**
- [ ] **Step 3: Run live verification only on a runtime-capable target with explicit PRoot/rootfs inputs.**
- [ ] **Step 4: Verify G1-G16 protection.**
- [ ] **Step 5: Inspect CI result and do not declare G17 GREEN without objective guest execution evidence.**
