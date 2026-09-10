# Alfa Device Ctrl Final UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved final Alfa Device Ctrl UI as a native, evidence-driven Android presentation layer without changing the runtime/security execution architecture.

**Architecture:** Keep `MainActivity`/native Views and the existing RuntimeRegistry, RuntimeUiState, Gate6/Gate7, RuntimeSessionManager, InteractiveSessionContract, HostStorageBridge, and execution-lane boundaries. Introduce focused presentation components around those contracts rather than copying Stitch HTML or mock state. Feature work is staged so every stage is independently testable.

**Tech Stack:** Android Java Views, existing TerminalView/TerminalSession integration, Gradle/JUnit, Android instrumentation/Espresso, Android window/insets APIs.

**Spec:** `docs/superpowers/specs/2026-09-11-final-ui-design-reconciliation.md`

## Global Constraints
- GitHub is the source of truth; changes land on `fix/multi-runtime-ui-v2` until verified.
- No fake runtime/session/telemetry/security status.
- Existing Gate6/Gate7 and runtime evidence remain mandatory.
- Termux, Termux:API, and Shizuku/Rish remain separate execution lanes.
- Native Android Views remain the production UI architecture.
- Minimum interactive target is 48dp.
- Responsive layout is based on available window space/insets, not fixed device assumptions.
- Canonical Alfa storage contracts remain authoritative; design-artifact placeholder paths are rejected.
- One change → one test → verification.
- No release/merge claim without CI and DUT evidence.

### Task 1: Presentation token layer and core shell
**Files:** new focused UI token/helper classes; `MainActivity.java`; UI tests.
- Encode Terminal Obsidian colors, typography hierarchy, borders, radii, spacing and touch-target invariants without external font/CDN dependencies.
- Remove the fixed 248dp runtime-dashboard overflow by making the runtime region adaptive/scroll-safe.
- Preserve current terminal/session behavior.
- Add instrumentation assertions for dashboard fit, accessibility targets, and selected-runtime presentation.

### Task 2: Runtime/session presentation model
**Files:** runtime UI state/presentation classes; `MainActivity.java`; tests.
- Map runtime states to final visual semantics.
- Add explicit session states including NOT_READY, STARTING, RUNNING, FAILED and FINISHED where supported by existing evidence.
- Ensure runtime-specific terminal title/prompt/session metadata is derived from the actual selected runtime/session.
- Preserve fail-closed OPEN behavior.

### Task 3: Session multiplexer
**Files:** focused session presentation/manager classes; `RuntimeSessionManager` only where its current API is insufficient; tests.
- Support up to 10 independent real sessions per runtime.
- Session switching must preserve independent PTY/process state.
- Stop/restart one session without implicitly stopping another.
- Reject session creation at the proven per-runtime limit with explicit UI state.

### Task 4: Project Explorer
**Files:** new native Project Explorer components; HostStorageBridge integration; tests.
- Present host-project and guest-runtime filesystem domains separately.
- Read actual directory/file data through the existing storage/runtime contracts.
- Implement mobile drawer and tablet/desktop persistent pane.
- Delete/create/override operations require evidence-backed authorization and explicit confirmation.

### Task 5: Floating terminal and split-pane presentation
**Files:** new floating/split UI components; Android manifest/service components only when required; tests.
- Floating terminal attaches to a real existing session; it never creates a fake parallel shell.
- Overlay permission failure is explicit and fail-closed.
- Split panes may display multiple real sessions but never broadcast input unless a real synchronization contract exists and is enabled.

### Task 6: Appearance, typography, and interaction language
**Files:** presentation preferences and UI settings components; tests.
- Persist presentation-only settings.
- Support Indonesian and English UI labels.
- Technical command lines, paths, package names, shell output and evidence identifiers remain invariant.
- Avoid adding DataStore/Compose solely because the Stitch artifact contains a sample implementation; use the smallest architecture compatible with the current app.

### Task 7: Security diagnostics and policy UI
**Files:** new security/policy presentation components; existing security/runtime contracts; tests.
- Display real diagnostic evidence and explicit UNKNOWN/BLOCKED states.
- Implement harden/bind, mount isolation, sensitive-file warning, seccomp/syscall verification, policy backup/restore and audit-log presentation only where an authoritative backend contract exists.
- Never elevate the UI's own authority or infer capabilities from mockup content.

### Task 8: External execution-lane presentation
**Files:** lane-status presentation and audit views; tests.
- Present Termux, Termux:API and Shizuku/Rish as separate lanes.
- Use actual bridge results where already available; otherwise show UNKNOWN/DEGRADED rather than fabricated green states.
- Do not embed Shizuku/Rish into the runtime PTY or make UI success depend on a cosmetic lane badge.

### Task 9: Brand/app icon assets
**Files:** Android drawable/mipmap resources; manifest only if required; tests/build verification.
- Convert the approved final icon direction into native adaptive/monochrome assets.
- Verify resource packaging and launcher behavior on the DUT.

### Task 10: Full verification and release gate
**Files:** tests/workflows/evidence as required.
- Run unit and instrumentation tests.
- Run Gradle build and packaging checks.
- Verify CI on the exact GitHub HEAD.
- Install the resulting APK on the ARM64 Android DUT.
- Verify launch, runtime selection, real PTY session, session switching, storage/project explorer boundaries, security evidence presentation, accessibility, and responsive behavior.
- Record evidence before any completion or release claim.
