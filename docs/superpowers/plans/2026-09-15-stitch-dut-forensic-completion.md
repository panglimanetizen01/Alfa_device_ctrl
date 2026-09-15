# Stitch DUT Forensic Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve the verified multi-session capability gap, classify every Stitch state without inflating implementation counts, harden legacy/APK provenance evidence, and drive the exact branch head through build/APK/DUT/runtime/network/visual evidence without issuing PASS until every required boundary is proven.

**Architecture:** Keep `RuntimeRegistry` as the sole runtime-identity owner. Add real multi-session orchestration above `RuntimeSessionManager`, where every session owns its own `TerminalSession`, PTY/process identity, listener, semantic state, and terminal surface; session switching changes the attached live session rather than a label. Stitch state realization remains a semantic mapping problem: each state must map to native hierarchy, action/state contract, and backend behavior where required, then be proven in the packaged APK and DUT. Debian remains the known-good PTY baseline; Ubuntu, Alpine, and Kali receive independent evidence.

**Tech Stack:** Android/Java, Termux TerminalSession/TerminalView, Gradle, GitHub Actions, ADB/DUT evidence, Linux `readelf`/`objdump`/`strings`/`sha256sum`/`ss`/`ip`/`strace`/`tcpdump` where available, Stitch ZIP forensic corpus.

**Spec:** `docs/forensic/7-gate-execution-plan.md` and `docs/forensic/stitch-159-state-realization-matrix.md`

## Global Constraints

- `RuntimeRegistry` remains the sole runtime-identity owner; do not create a second runtime registry.
- A session ID is never evidence of a distinct execution identity; distinct sessions must produce distinct live `TerminalSession`/PTY/process identities.
- No fake terminal output, cached output substituted for live PTY output, or label-only session switching.
- Execution plane remains network-independent; network installation/diagnostic capabilities stay outside interactive session execution.
- Debian behavior is a protected known-good baseline and must not regress.
- Ubuntu, Alpine, and Kali must each be proven independently.
- Stitch ZIP SHA-256 remains `7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d`; corpus remains 159 HTML states, 160 PNG, 7 Markdown, 326 files.
- Every evidence claim must identify the exact commit SHA; do not mix evidence across SHAs.
- Final PASS is forbidden until exact APK SHA, install identity, DUT behavior, runtime/PTY, network, security/provenance, and visual evidence are all present.

---

### Task 1: Establish the exact-head failure baseline

**Files:**
- Inspect: `.github/workflows/*` relevant to UI/native/APK/non-Debian/forensic gates
- Inspect: `app/src/test/java/com/alfa/device_ctrl/*Stitch*`
- Inspect: `app/src/test/java/com/alfa/device_ctrl/RuntimeSessionMultiplexer*`

**Interfaces:**
- Consumes: commit `016677b143380ac67137b80efd4358b44f69cba9` and its exact-head workflow runs.
- Produces: a failure matrix tied only to that SHA, including the failing UI/native focused-test gates.

- [ ] **Step 1: Record exact-head CI state.**
  Confirm the exact SHA in each run and record completed success/failure/in-progress states.

- [ ] **Step 2: Retrieve job-level evidence.**
  Identify the failing job and test step for `Alfa UI Contract` and `Stitch Native CI`; do not substitute an older run.

- [ ] **Step 3: Inspect the corresponding test/source contracts.**
  Determine whether the failures are stale assertions, missing implementation, or an architectural contradiction.

- [ ] **Step 4: Preserve the baseline.**
  Do not alter production behavior merely to make the existing contract pass.

---

### Task 2: Make multi-session execution semantically real

**Files:**
- Modify: `app/src/main/java/com/alfa/device_ctrl/RuntimeSessionMultiplexer.java`
- Modify: `app/src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java` only where a real per-session identity/lifecycle contract is required
- Create/modify: `app/src/test/java/com/alfa/device_ctrl/RuntimeSessionMultiplexerBehaviorTest.java`
- Create/modify: `app/src/test/java/com/alfa/device_ctrl/RuntimeSessionExecutionIdentityContractTest.java`

**Interfaces:**
- Consumes: `InteractiveSessionContract`, `RuntimeSessionManager`, `RuntimeKeepAliveService`, `TerminalSession`, `TerminalView`.
- Produces: independently addressable sessions whose managers each own a distinct live `TerminalSession`; observable session ID, PID, state, input/output stream, attach/switch/stop/remove lifecycle.

- [ ] **Step 1: Write failing behavior tests.**
  Test two session IDs backed by two independent contracts/managers; assert distinct manager objects, lifecycle isolation, duplicate-ID rejection, and removal semantics.

- [ ] **Step 2: Add an execution-identity seam.**
  Expose only evidence-safe read access needed to correlate a manager with its live `TerminalSession` PID/session identity. Do not manufacture IDs when no live session exists.

- [ ] **Step 3: Implement minimal orchestration.**
  Keep the multiplexer as a manager collection; each manager creates its own `TerminalSession`. Do not reuse a single `TerminalSession` across IDs.

- [ ] **Step 4: Add input/output isolation tests.**
  Use deliberately unique markers such as `SESSION_A_MARKER` and `SESSION_B_MARKER`; require each marker to be produced by its own live shell and never attributed to the other session.

- [ ] **Step 5: Add failure isolation.**
  Stop/finish one manager and prove the other remains running and addressable.

- [ ] **Step 6: Add lifecycle/keepalive tests.**
  Prove activity pause/background preservation does not accidentally terminate unrelated sessions and that service ownership stops only after the final owner is gone.

---

### Task 3: Integrate real session switching into the canonical Stitch UI

**Files:**
- Modify: `app/src/main/java/com/alfa/device_ctrl/StitchOperationalActivity.java`
- Modify: `app/src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java`
- Modify: `app/src/main/java/com/alfa/device_ctrl/StitchUiContract.java` only if action/state contracts are missing
- Modify: `app/src/main/java/com/alfa/device_ctrl/SessionUiState.java` only if semantic states cannot represent the live multiplexer state
- Create/modify: UI/session integration tests

**Interfaces:**
- Consumes: `RuntimeSessionMultiplexer`, per-session `RuntimeSessionManager.Listener`, `SessionUiState`.
- Produces: UI session A → live PTY A and UI session B → live PTY B, with switching, isolation, active-session state, and correct terminal attachment.

- [ ] **Step 1: Write a failing integration contract.**
  Require session controls to be enabled only when real multiplexer-backed sessions exist and require switch operations to select the live manager.

- [ ] **Step 2: Replace reference-only session gating.**
  Remove only the `MULTI-PTY BACKEND=NOT_EXPOSED_BY_CANONICAL_MANAGER` behavior that is contradicted by the new real backend; retain honest disabled states for capabilities still not implemented.

- [ ] **Step 3: Bind terminal surface to the active manager.**
  Attach the existing `TerminalView` to the selected live `TerminalSession`; preserve the previous session in the background rather than destroying it on switch.

- [ ] **Step 4: Wire create/attach/switch/stop/remove.**
  Ensure every UI action routes to the multiplexer and produces a semantic state update.

- [ ] **Step 5: Test marker isolation.**
  Switch A→B→A→B while issuing unique shell commands and verify terminal content/state follows the actual selected session.

---

### Task 4: Build the 159-state realization matrix from individual corpus evidence

**Files:**
- Modify: `docs/forensic/stitch-159-state-realization-matrix.md`
- Create: `tools/forensics/stitch_state_realization_audit.*` as appropriate to repository conventions
- Modify: `app/src/test/java/com/alfa/device_ctrl/*Stitch*` only for evidence contracts

**Interfaces:**
- Consumes: authoritative Stitch ZIP manifest/corpus, native source/catalog/contracts.
- Produces: one row per state with `IMPLEMENTED`, `PARTIALLY_IMPLEMENTED`, `REFERENCE ONLY`, `OBSOLETE`, `NOT IMPLEMENTED`, or `NOT PROVEN`, plus source/action/backend evidence.

- [ ] **Step 1: Parse all 159 HTML states and 160 PNG assets.**
  Preserve exact filenames/state identifiers and domain classification.

- [ ] **Step 2: Extract semantic controls and interaction intent.**
  Record navigation, buttons, inputs, disabled/loading/error/success/scroll/terminal/session/runtime/security/network semantics.

- [ ] **Step 3: Map each state to native implementation.**
  A catalog entry alone is insufficient; require actual native hierarchy/action code or an explicit reference-only classification.

- [ ] **Step 4: Map required backend semantics.**
  For live terminal/runtime/session states, require a corresponding real backend capability.

- [ ] **Step 5: Mark NOT PROVEN aggressively.**
  Do not infer implementation from names, comments, screenshots, or tests that only inspect source tokens.

- [ ] **Step 6: Add aggregate counts.**
  Publish counts by classification and domain without inflating `IMPLEMENTED`.

---

### Task 5: Harden legacy eradication and packaged-APK scanning

**Files:**
- Modify: `tools/tests/test_stitch_ui_boundary.sh`
- Modify: `tools/ci/stitch-legacy-ui-blacklist.txt` only when a verified legacy signature is found
- Modify: relevant CI workflow that builds/scans the APK
- Inspect: Java/Kotlin/XML/Gradle/manifest/generated/resource/native packaging

**Interfaces:**
- Consumes: legacy blacklist, source tree, generated build tree, exact debug APK.
- Produces: source/build/APK scan proving retired operational owners and signatures are absent, with exceptions explicitly justified.

- [ ] **Step 1: Expand source scan.**
  Scan Java/Kotlin/XML/Gradle/manifest/resources/tests/generated outputs and reflection/string literals for each forbidden signature.

- [ ] **Step 2: Build the exact head APK.**
  Stop if compile/test fails; do not scan a stale APK.

- [ ] **Step 3: Inspect APK contents.**
  Verify manifest launcher, classes/resources/native libs, package identity, and forbidden strings/classes.

- [ ] **Step 4: Hash the APK.**
  Record SHA-256 and tie it to the exact commit.

- [ ] **Step 5: Require the same APK downstream.**
  DUT evidence must reference the exact APK hash, not merely the version name.

---

### Task 6: Preserve and independently prove Debian, Ubuntu, Alpine, and Kali

**Files:**
- Inspect/modify only evidence/test tooling unless a proven runtime defect requires production change
- Create/modify: CI runtime forensic workflow and evidence manifests

**Interfaces:**
- Consumes: `RuntimeRegistry`, `RuntimeInstaller`, `RuntimeSessionManager`, rootfs/artifact payloads, PRoot.
- Produces: distro-specific evidence bundles.

- [ ] **Step 1: Debian regression baseline.**
  Prove PRoot → shell → PTY → stdin/stdout/stderr → terminal state before accepting unrelated runtime changes.

- [ ] **Step 2: Ubuntu independent proof.**
  Verify artifact/rootfs, loader, executable, PRoot, PTY, shell, streams, semantic state, and terminal surface.

- [ ] **Step 3: Alpine independent proof.**
  Repeat the same chain; do not reuse Debian evidence.

- [ ] **Step 4: Kali independent proof.**
  Repeat the same chain; do not reuse Debian evidence.

- [ ] **Step 5: Record failure semantics.**
  For every failed layer capture pre-state, event/data, actual transition, expected transition, boundary, root cause, fix, and regression test.

---

### Task 7: Network forensic evidence

**Files:**
- Modify: runtime forensic scripts/workflows only where needed to collect evidence
- Inspect: `AlfaNetworkPanel.java`, installer/network boundary, runtime rootfs configuration

**Interfaces:**
- Consumes: Android `ConnectivityManager`/`Network`/`LinkProperties`, Linux interface/route/socket state, runtime resolver config.
- Produces: a layer-by-layer network evidence record.

- [ ] **Step 1: Capture `/etc/resolv.conf` and resolver behavior.**
- [ ] **Step 2: Capture interface and route state with `ip`.**
- [ ] **Step 3: Capture socket/TCP state with `ss`.**
- [ ] **Step 4: Correlate Android `LinkProperties`/capabilities.**
- [ ] **Step 5: Use `tcpdump` when available.**
- [ ] **Step 6: Use `strace` or closest available syscall evidence when packet capture is unavailable.**
- [ ] **Step 7: Distinguish DNS, routing, TCP, NAT/firewall, PRoot, and Android network-selection failures instead of collapsing them into `ping` success/failure.**

---

### Task 8: APK provenance and exact-DUT verification

**Files:**
- Modify: CI artifact/provenance workflow as required
- Create/modify: DUT evidence scripts and manifest
- Modify: forensic documentation with exact SHA references

**Interfaces:**
- Consumes: exact-head APK, APK SHA-256, package/version/manifest, device installation state.
- Produces: reproducible APK→DUT provenance chain and runtime/UI evidence.

- [ ] **Step 1: Lock commit SHA and APK SHA-256.**
- [ ] **Step 2: Verify package/version/launcher and native payload.**
- [ ] **Step 3: Install that exact APK to DUT.**
- [ ] **Step 4: Verify installed package and artifact hash/provenance.**
- [ ] **Step 5: Capture screenshots/UI hierarchy for implemented states.**
- [ ] **Step 6: Exercise interactions and record state transitions/logcat.**
- [ ] **Step 7: Exercise real multi-session markers and PID identities.**
- [ ] **Step 8: Exercise Debian, Ubuntu, Alpine, Kali independently.**

---

### Task 9: Stitch visual comparison and failure-driven iteration

**Files:**
- Modify: Stitch forensic workflow/scripts
- Modify: `docs/forensic/stitch-159-state-realization-matrix.md`
- Create: screenshot comparison evidence artifacts as repository/CI artifacts

**Interfaces:**
- Consumes: Stitch reference PNGs/HTML semantics and DUT APK screenshots.
- Produces: per-state visual/semantic comparison with explicit failures.

- [ ] **Step 1: Capture actual APK/DUT screenshots.**
- [ ] **Step 2: Pair screenshots to state IDs using deterministic navigation/state evidence.**
- [ ] **Step 3: Compare hierarchy/composition/spacing/typography/controls/state/visual identity.**
- [ ] **Step 4: Treat large structural mismatch as failure, not as "close enough".**
- [ ] **Step 5: Iterate root cause → fix → rebuild → reinstall → recapture until evidence closes the gap.**

---

### Task 10: Final gate and PASS lock

**Files:**
- Modify: `docs/forensic/7-gate-execution-plan.md`
- Modify: `docs/forensic/stitch-159-state-realization-matrix.md`
- Modify: final evidence manifest

**Interfaces:**
- Consumes: all prior gate evidence tied to one final commit/APK/DUT.
- Produces: either explicit `YELLOW/RED` with open gaps or `PASS — GREEN — LOCKED` only when every required claim is empirically proven.

- [ ] **Step 1: Run final exact-head CI and verify all required workflows.**
- [ ] **Step 2: Verify exact APK provenance and DUT installation identity.**
- [ ] **Step 3: Verify real multi-session PTY/process identities and isolation.**
- [ ] **Step 4: Verify all four distro runtime chains independently.**
- [ ] **Step 5: Verify network evidence where runtime capability requires it.**
- [ ] **Step 6: Verify visual/semantic Stitch realization for every state claimed implemented.**
- [ ] **Step 7: Verify no legacy operational owner remains in source/build/APK/runtime.**
- [ ] **Step 8: Issue PASS only if every required evidence link is closed; otherwise retain YELLOW or RED.**
