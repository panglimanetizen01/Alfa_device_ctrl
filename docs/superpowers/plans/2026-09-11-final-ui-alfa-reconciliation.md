# Final Alfa UI Reconciliation Implementation Plan

> **For agentic workers:** This plan is executed inline by ChatGPT only; no external agent is delegated implementation or verification.

**Goal:** Reconcile the uploaded 118-screen Alfa Device Ctrl UI baseline with the current canonical Alfa runtime architecture so the native Android UI presents real runtime/session/evidence state without changing the established visual direction.

**Architecture:** Keep the current native Android View/TerminalView architecture and canonical runtime contracts as the source of truth. Treat the uploaded HTML/screenshots as visual/state coverage references, then implement missing capabilities as native stateful components backed by RuntimeRegistry, RuntimeUiState, RuntimeSessionManager, HostStorageBridge/SAF, and OperationEvidence. Hardcoded terminal/security PASS output must never become runtime truth.

**Tech Stack:** Android Java Views, existing TerminalView/Termux terminal library, Android 16/API 36, existing RuntimeRegistry/RuntimeProfile/RuntimeInstaller/InteractiveSessionContract/RuntimeSessionManager, existing SAF storage bridge, Android instrumentation/JUnit tests.

**Spec:** Uploaded `UI_alfa_device_control_final(1).zip`, especially its Terminal Obsidian DESIGN.md files and runtime/state screen coverage, reconciled against canonical Alfa master at `66def003ad521e6f844b40fffa73bcf94f0dd178` and later current master contracts.

## Global Constraints

- GitHub master is canonical; this branch is the isolated implementation lane.
- No external agent is delegated implementation, diagnosis, patching, testing, or verification.
- Existing Alfa runtime contracts remain authoritative over stale ZIP documentation.
- Package identity remains `com.alfa.device_ctrl`.
- Runtime truth must come from evidence/backend execution, not UI labels or screenshots.
- Terminal body output remains PTY-backed; no fake prompts or synthetic runtime telemetry.
- Runtime selection remains RuntimeRegistry-driven and fail-closed.
- Host storage remains SAF-first and canonical Alfa host-storage contract; never use UI text as storage proof.
- Preserve the Terminal Obsidian visual direction and existing layout intent; adapt only where required by real runtime state, accessibility, or Android platform constraints.
- Interactive controls must retain a minimum 48dp hit target.
- Android 16/API 36 adaptive and edge-to-edge behavior must remain safe under window insets.
- One behavioral change -> focused test -> verification before the next change.

---

### Task 1: Establish a machine-checkable UI baseline manifest

**Files:**
- Create: `ci/ui-baseline/uploaded-ui-manifest.json`
- Create: `ci/ui-baseline/verify_uploaded_ui.py`
- Test: `app/src/test/java/com/alfa/device_ctrl/UiBaselineContractTest.java`

**Purpose:** Convert the uploaded 118-screen reference into an auditable feature/state inventory without treating HTML as executable production UI.

- [ ] Enumerate screen names, categories, simulation markers, runtime distributions, security states, session lifecycle states, settings, project explorer, floating terminal, policy backup/restore, and notification flows.
- [ ] Record the known stale package/storage strings as `REFERENCE_ONLY` mismatches rather than propagating them into production.
- [ ] Record each production capability that must be backed by a canonical Alfa contract.
- [ ] Add a deterministic test that fails if the production UI claims a capability for which no canonical backend/state source exists.

### Task 2: Reconcile the native shell with the Terminal Obsidian visual contract

**Files:**
- Modify: `app/src/main/java/com/alfa/device_ctrl/MainActivity.java`
- Modify: `app/src/main/java/com/alfa/device_ctrl/AlfaUiTheme.java`
- Modify: `app/src/androidTest/java/com/alfa/device_ctrl/AlfaUiThemeInstrumentationTest.java`

- [ ] Map the ZIP dashboard hierarchy to native components: top identity bar, terminal/runtime/monitor navigation, telemetry strip, session card, terminal body, command row, shortcut row, runtime manager, execution-lane status.
- [ ] Keep runtime body output exclusively TerminalView/PTy-backed.
- [ ] Remove remaining legacy visual constants from production rendering paths instead of relying only on post-hoc normalization.
- [ ] Make measured-window layout adaptive without physical-device assumptions.
- [ ] Preserve 48dp interactive footprints and verify them through instrumentation.

### Task 3: Make session state projection complete and fail-closed

**Files:**
- Modify: `app/src/main/java/com/alfa/device_ctrl/SessionUiState.java`
- Modify: `app/src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java`
- Modify: `app/src/main/java/com/alfa/device_ctrl/MainActivity.java`
- Test: existing and new session-state unit/instrumentation contracts.

- [ ] Cover every state represented by the ZIP lifecycle screens: blocked, starting, PTY-created, waiting-for-prompt, ready, running, stopping, preserved-background, finished, failed.
- [ ] Ensure the UI never derives READY/ACTIVE from static screen state.
- [ ] Bind session identity, runtime identity, PID, and exit status to actual session/evidence.
- [ ] Verify process-group cleanup remains canonical for command execution.

### Task 4: Replace mock telemetry/process/network surfaces with evidence-backed runtime views

**Files:**
- Modify: `app/src/main/java/com/alfa/device_ctrl/MainActivity.java`
- Modify: `app/src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java` only where required by evidence transport.
- Create/modify focused tests under `app/src/test` and `app/src/androidTest`.

- [ ] CPU, memory, swap, process, interface, and route values must be obtained from the selected runtime session.
- [ ] Preserve UNKNOWN/BLOCKED states when evidence is unavailable.
- [ ] Process-kill UI must remain scoped to selected rootless runtime and require a fresh process evidence snapshot.
- [ ] No `Math.random`, timer-generated PASS, or hardcoded telemetry may feed production state.

### Task 5: Implement the runtime/session multiplexer represented by the ZIP

**Files:**
- Modify: `app/src/main/java/com/alfa/device_ctrl/MainActivity.java`
- Modify/create: session UI state/model classes as needed.
- Test: instrumentation coverage for session selection and lifecycle.

- [ ] Represent multiple runtime sessions as data/state rather than separate static screens.
- [ ] Add session creation, selection, stop, and reattach behavior against the existing RuntimeSessionManager contract.
- [ ] Enforce an explicit authoritative session/resource limit rather than hardcoded visual counts.
- [ ] Ensure session switching cannot cross runtime identity without stopping/closing the previous interactive session safely.

### Task 6: Reconcile Project Explorer and Host Storage with the canonical SAF bridge

**Files:**
- Modify: `app/src/main/java/com/alfa/device_ctrl/MainActivity.java`
- Modify: `app/src/main/java/com/alfa/device_ctrl/HostStorageBridge.java` only if evidence shows a missing operation.
- Modify: `app/src/main/java/com/alfa/device_ctrl/SafTreeProvider.java` only if evidence shows a missing operation.
- Test: storage/SAF instrumentation and contract tests.

- [ ] Implement native project/file browser states represented by the ZIP.
- [ ] Separate host project files from guest VFS and app-private runtime vault in the data model.
- [ ] Never expose the stale ZIP path `/storage/emulated/0/Android/data/moe.alfa.device.ctrl/files/workspace/` as canonical.
- [ ] Use SAF/ContentResolver evidence for host-visible files and keep guest VFS commands scoped to the selected runtime.
- [ ] File/folder create, delete, recursive-delete confirmation, rename, and audit-file sharing must have real backend outcomes before success states are shown.

### Task 7: Implement settings/appearance/locale as real persisted presentation state

**Files:**
- Modify/create native settings model/store and resources under `app/src/main/java/com/alfa/device_ctrl/` and `app/src/main/res/`.
- Test: unit/instrumentation coverage.

- [ ] Implement the ZIP's locale, typography, density, appearance preset, and reset states without changing technical invariants.
- [ ] Keep technical identifiers such as PTY, PID, SIGTERM, SIGKILL, PRoot, SHA-256, UID/GID, and runtime IDs invariant.
- [ ] Persist presentation settings and restore them across process recreation.
- [ ] Verify settings changes do not stop/restart an active runtime session.

### Task 8: Implement security/diagnostic surfaces as evidence viewers, not simulators

**Files:**
- Modify: `app/src/main/java/com/alfa/device_ctrl/MainActivity.java`
- Modify/create security/evidence UI model classes as needed.
- Test: unit/instrumentation security-state projection.

- [ ] Add diagnostic sections for Shizuku/Rish, seccomp/mount, syscall traps, socket/FD status, and remediation outcomes only when corresponding evidence exists.
- [ ] Every PASS/FAIL/WARN state must carry an evidence reference or an explicit UNKNOWN/BLOCKED reason.
- [ ] Never claim Android root, kernel isolation, mount capability, or seccomp success from UI configuration alone.
- [ ] Sanitize runtime-derived text before rendering into any HTML-capable surface; native TextView rendering is preferred.

### Task 9: Implement policy backup/restore as validated data flow

**Files:**
- Modify/create policy model/store and validation classes under `app/src/main/java/com/alfa/device_ctrl/`.
- Modify: `MainActivity.java` for UI flow.
- Test: unit tests for schema/integrity/compatibility and instrumentation for import/export states.

- [ ] Define a versioned policy schema tied to current Alfa runtime contracts.
- [ ] Validate syntax, schema version, runtime IDs, integrity metadata, and compatibility before applying.
- [ ] Use atomic apply/rollback semantics; never show RESTORED/APPLIED before verification.
- [ ] Preserve the ZIP's confirmation/processing/success/failure visual states as native state transitions.

### Task 10: Implement floating terminal/split-pane presentation as a native projection of existing sessions

**Files:**
- Modify/create native terminal container/session UI classes.
- Modify: `AndroidManifest.xml` only if a required platform permission/component is proven necessary.
- Test: instrumentation for session attach/detach, split selection, IME, and insets.

- [ ] Implement split-pane presentation using existing terminal sessions rather than creating fake duplicate sessions.
- [ ] Implement keyboard/IME state and terminal focus transitions.
- [ ] For true overlay behavior, require Android permission/capability evidence before exposing it; otherwise present the supported in-app floating/sheet equivalent and label unsupported overlay capability explicitly.
- [ ] Verify edge-to-edge and gesture insets do not place controls under system bars.

### Task 11: Reconcile notification/webhook and sharing flows with real capabilities

**Files:**
- Modify/create notification/share capability classes and UI projection.
- Test: capability-present, capability-absent, and failure paths.

- [ ] Do not hardcode Matrix webhook success/failure as UI state.
- [ ] Gate external dispatch behind configured endpoint/auth state and show UNKNOWN/NOT CONFIGURED when absent.
- [ ] Use Android share APIs for audit-file sharing and show success only after the handoff succeeds.

### Task 12: Full regression and release reconciliation

**Files:**
- Modify: relevant tests/workflows only after evidence identifies a gap.
- Create: final UI reconciliation evidence/report under `ci-evidence/` or the canonical evidence path.

- [ ] Run unit tests, Android instrumentation, static contract tests, build, and existing Alfa gates.
- [ ] Verify master/current canonical contracts remain intact.
- [ ] Verify no stale package/storage identifiers from the ZIP leaked into production source.
- [ ] Verify no mock/simulation runtime output is used as production truth.
- [ ] Verify UI state transitions against real runtime/evidence paths.
- [ ] Only after all gates pass, prepare a PR for review/merge; do not force-update master.
