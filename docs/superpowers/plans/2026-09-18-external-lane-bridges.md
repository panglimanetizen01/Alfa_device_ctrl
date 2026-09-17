# External Execution Lane Bridges Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement evidence-backed Alfa bridges for Shizuku UserService and Termux RUN_COMMAND while keeping Termux:API separate and keeping the Linux runtime/PTy engine unchanged.

**Architecture:** External execution is a sibling capability layer to the canonical Linux runtime, not a replacement transport. Shizuku uses `ShizukuProvider` + permission check + `bindUserService` + AIDL callback to a shell/root UserService that spawns a non-PTY process and returns PID/stdout/stderr/exit. Termux uses the documented `com.termux.RUN_COMMAND` Intent to `com.termux.app.RunCommandService` with a PendingIntent result receiver; Termux:API is detected as a separate installed capability and, when exercised, is invoked through the Termux command lane (for example `termux-battery-status`), never treated as a shell transport.

**Tech Stack:** Android Java 8-compatible source, AIDL, Shizuku API/provider 13.1.5, Android Intent/PendingIntent, JUnit 4, Android instrumentation.

**Spec:** `docs/superpowers/specs/2026-09-11-final-ui-design-reconciliation.md` plus the canonical external-lane isolation contract.

## Global Constraints

- `RuntimeSessionManager → TerminalSession → embedded PTY → PRoot → rootfs` remains the Linux runtime engine.
- External lanes must not become a hidden prerequisite for runtime readiness.
- Shizuku `newProcess` is not used; Shizuku UserService is the causal process boundary.
- Termux:API is not used as terminal transport.
- ADB/USB is not a product runtime dependency.
- No UI state is promoted to READY/PASS without evidence.
- No file deletion is allowed in this milestone unless dependency proof exists.
- External process execution must expose real PID, stdin, stdout, stderr, exit and lifecycle/failure state where the primitive supports it.
- Rish remains an external Shizuku-backed shell interface; Alfa does not pretend that the presence of `/storage/emulated/0/Shizuku/rish` proves Alfa integration.

---

### Task 1: Lock bridge contracts with failing tests

**Files:**
- Create: `app/src/test/java/com/alfa/device_ctrl/ExternalBridgeContractTest.java`
- Create: `app/src/test/java/com/alfa/device_ctrl/TermuxRunCommandContractTest.java`

**Interfaces:**
- `ExternalExecutionResult` exposes lane, process id, exit code, stdout, stderr, error.
- `TermuxRunCommandBridge` exposes documented action/component/extra constants without depending on Termux source.

- [ ] **Step 1: Write the failing tests** asserting Shizuku and Termux are explicit external lanes, Termux RUN_COMMAND targets `com.termux/com.termux.app.RunCommandService`, and Termux:API is represented as a capability rather than a runtime transport.
- [ ] **Step 2: Run `./gradlew :app:testDebugUnitTest --tests com.alfa.device_ctrl.ExternalBridgeContractTest --tests com.alfa.device_ctrl.TermuxRunCommandContractTest` and verify failure because the new bridge classes do not exist.
- [ ] **Step 3: Commit the failing tests** with `test: lock external bridge contracts`.

### Task 2: Implement Shizuku UserService bridge

**Files:**
- Modify: `app/build.gradle`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/aidl/com/alfa/device_ctrl/external/IAlfaShizukuCallback.aidl`
- Create: `app/src/main/aidl/com/alfa/device_ctrl/external/IAlfaShizukuService.aidl`
- Create: `app/src/main/java/com/alfa/device_ctrl/external/AlfaShizukuUserService.java`
- Create: `app/src/main/java/com/alfa/device_ctrl/external/ShizukuExecutionBridge.java`
- Create: `app/src/main/java/com/alfa/device_ctrl/external/ExternalExecutionResult.java`

**Interfaces:**
- `ShizukuExecutionBridge.isAvailable()` checks binder readiness.
- `ShizukuExecutionBridge.hasPermission()` checks the actual Shizuku permission.
- `ShizukuExecutionBridge.requestPermission(int)` delegates to Shizuku.
- `ShizukuExecutionBridge.execute(String[] command, String workDir, Callback)` binds a UserService and starts one real process.
- `IAlfaShizukuService.execute(...)`, `writeStdin(...)`, `terminate()` provide non-PTY process control.

- [ ] **Step 1: Add Shizuku API/provider dependency 13.1.5 and provider manifest entry based on official Shizuku developer guidance.
- [ ] **Step 2: Implement AIDL callback/service definitions.
- [ ] **Step 3: Implement UserService process creation using `/system/bin/sh -c 'echo $$; exec "$0" "$@"' ...` so the first stdout record is the real exec PID without relying on unavailable `Process.pid()` on minSdk 26.
- [ ] **Step 4: Stream stdout/stderr on separate threads, forward stdin, return exit code, and destroy the child on termination/disconnect.
- [ ] **Step 5: Implement app-side binder lifecycle, permission state, callback propagation, and unbind cleanup.
- [ ] **Step 6: Run the contract tests and full unit tests; fix implementation only, never weaken assertions.

### Task 3: Implement Termux RUN_COMMAND bridge

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/alfa/device_ctrl/external/TermuxRunCommandBridge.java`
- Create: `app/src/main/java/com/alfa/device_ctrl/external/TermuxResultReceiverService.java`

**Interfaces:**
- `TermuxRunCommandBridge.isInstalled()` checks `com.termux` package visibility.
- `TermuxRunCommandBridge.hasPermission()` checks `com.termux.permission.RUN_COMMAND`.
- `TermuxRunCommandBridge.execute(...)` sends the documented `RUN_COMMAND` Intent and registers a one-shot result callback.

- [ ] **Step 1: Add `com.termux.permission.RUN_COMMAND` and `<queries><package android:name="com.termux"/></queries>`.
- [ ] **Step 2: Implement the documented action/component/command path/arguments/workdir/stdin/background/pending-intent fields.
- [ ] **Step 3: Implement the result receiver for stdout, stderr, exit code and Termux-side error fields.
- [ ] **Step 4: Run unit tests and build checks.

### Task 4: Add explicit Termux:API capability lane

**Files:**
- Create: `app/src/main/java/com/alfa/device_ctrl/external/TermuxApiCapability.java`
- Create: `app/src/test/java/com/alfa/device_ctrl/TermuxApiCapabilityTest.java`

**Interfaces:**
- `TermuxApiCapability.isInstalled()` checks package `com.termux.api`.
- `TermuxApiCapability` never exposes shell/process transport methods.

- [ ] **Step 1: Write failing package/role tests.
- [ ] **Step 2: Implement package detection only.
- [ ] **Step 3: Verify that a device API probe such as `termux-battery-status`, when later exercised, is routed through `TermuxRunCommandBridge`, preserving the causal chain Alfa → Termux → command → Termux:API helper/receiver → result.

### Task 5: Integrate evidence projection without merging lanes

**Files:**
- Modify only the existing external-lane presentation/evidence owner identified by current source references after Task 2–4.
- Do not modify `RuntimeSessionManager` transport construction.

- [ ] **Step 1: Add evidence fields for transport connected, process started, PID, output captured, exit, failure/timeout.
- [ ] **Step 2: Keep runtime readiness derived exclusively from runtime evidence.
- [ ] **Step 3: Add instrumentation coverage for capability states and fail-closed behavior.

### Task 6: Verify, CI, APK provenance and DUT

**Files:**
- Modify only workflow/test files required by actual failing CI evidence.

- [ ] **Step 1: Run full unit tests and instrumentation-capable build locally/CI.
- [ ] **Step 2: Commit the implementation in small commits with causal evidence in messages.
- [ ] **Step 3: Open PR against `master` and run GitHub Actions.
- [ ] **Step 4: Verify APK provenance, canonical source SHA and native/runtime gates.
- [ ] **Step 5: Install the resulting APK on the real DUT only after CI is green.
- [ ] **Step 6: Execute Shizuku command probe and Termux RUN_COMMAND probe and capture PID/stdout/stderr/exit evidence.
- [ ] **Step 7: Execute Termux:API probe through Termux RUN_COMMAND and record the distinct API receiver/result evidence.
- [ ] **Step 8: Only then evaluate Debian/Ubuntu/Alpine/Kali runtime evidence; do not infer distro success from external-lane success.
