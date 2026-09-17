# Stitch Multi-Runtime Forensic Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove and, where necessary, minimally repair the complete path from the canonical Stitch reference through legacy-free APK packaging, real Debian/Ubuntu/Alpine/Kali PTY execution, network behavior, security boundaries, and exact APK/DUT verification.

**Architecture:** `StitchOperationalActivity` remains the sole operational UI owner and `RuntimeRegistry` remains the sole runtime registry. Execution remains network-independent through `RuntimeSessionManager`/`InteractiveSessionContract`; network acquisition remains in `RuntimeInstaller`. Legacy components are removed only after source→build→APK→runtime evidence proves they are dead. Visual fidelity is proven by APK/DUT screenshots and structural interaction evidence, not source inspection alone.

**Tech Stack:** Android/Gradle, Java, TerminalView/TerminalSession, PRoot, Linux rootfs artifacts, GitHub Actions, APK/ELF inspection, Android instrumentation/UI automation where available, adb/logcat, Linux networking tools where available, Stitch ZIP corpus.

**Spec:** Approved user milestone: seven forensic gates covering legacy build/UI, Stitch realization, four distro runtime execution, network/protocol, security/provenance, APK packaging, and exact APK/DUT validation.

## Global Constraints

- GitHub repository `panglimanetizen01/Alfa_device_ctrl` is SOURCE OF TRUTH.
- Canonical branch is `work/stitch-dut-canonical-hardening-v2`.
- Do not claim VERIFIED/PASS without direct evidence.
- Preserve the proven Debian runtime path unless a reproducible contradiction is demonstrated.
- Do not move runtime ownership back to `MainActivity`.
- Do not create a second runtime registry.
- Do not put network transport into the execution plane.
- Do not replace real PTY/shell execution with mock terminal output.
- Do not weaken tests to make CI green.
- Do not delete legacy files until dependency-chain evidence classifies them DEAD.
- The canonical Stitch ZIP is `stitch/stitch_alfa_device_control_v1.0.0.zip` with expected SHA-256 `7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d` and expected corpus `159 code.html`, `160 PNG`, `7 Markdown`, `326 total files`.
- Final PASS requires exact source/build/APK/install/DUT identity plus real UI, interaction, four-distro runtime, network, PRoot, security, and provenance evidence.

---

### Task 1: Freeze the canonical baseline and current CI failure

**Files:**
- Read: `.github/workflows/*.yml`
- Read: `app/src/main/AndroidManifest.xml`
- Read: `app/src/main/java/com/alfa/device_ctrl/StitchOperationalActivity.java`
- Read: `app/src/main/java/com/alfa/device_ctrl/RuntimeRegistry.java`
- Read: `app/src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java`
- Read: `app/src/main/java/com/alfa/device_ctrl/InteractiveSessionContract.java`
- Read: `app/src/test/java/com/alfa/device_ctrl/RuntimeKeepAliveOwnershipContractTest.java`
- Create: `docs/forensic/stitch-multiruntime-baseline-2026-09-15.md`

**Interfaces:**
- Consumes: current branch HEAD and GitHub Actions evidence.
- Produces: immutable baseline SHA, CI status table, known failures, and evidence inventory for every later task.

- [ ] **Step 1: Record exact HEAD and changed files.**

Run against GitHub source of truth and record the exact commit SHA, parent, and changed files. Do not use a guessed local state.

- [ ] **Step 2: Record current CI state.**

For the exact HEAD, collect every workflow run and identify completed success, failure, and in-progress jobs. A green individual contract job must not be interpreted as an APK/DUT pass.

- [ ] **Step 3: Record the current KeepAlive failure exactly.**

Capture the failing test name and source/test expressions. Preserve the semantic contract; do not alter production behavior merely to satisfy whitespace or formatting.

- [ ] **Step 4: Write the baseline evidence document.**

The document must contain exact SHA values, workflow names, failure text, current launcher manifest, and an explicit `NOT PROVEN` list for APK/DUT/UI/runtime items lacking evidence.

- [ ] **Step 5: Commit the baseline document.**

Commit only the evidence document.

---

### Task 2: Build the complete legacy dependency graph before deletion

**Files:**
- Read: `app/src/main/`
- Read: `app/src/test/`
- Read: `app/build.gradle`
- Read: `build.gradle`
- Read: `settings.gradle`
- Read: `gradle.properties`
- Read: `.github/workflows/*.yml`
- Read: `ci/`
- Read: `artifacts/`
- Create: `docs/forensic/legacy-dependency-graph.md`
- Create if required by evidence: `ci/legacy-apk-signature-scan.sh`

**Interfaces:**
- Consumes: Task 1 baseline.
- Produces: classified legacy inventory with `FACT`, `DEAD/OBSOLETE`, `MIGRATION REMNANT`, `ACTIVE DEPENDENCY`, or `NOT PROVEN`.

- [ ] **Step 1: Enumerate legacy candidates.**

Search source, resources, manifest, Gradle, generated-resource declarations, native packaging, tests, and workflows for `MainActivity`, old layouts, old renderers, old navigation, old resources, old themes/styles, old strings, old drawable names, old task names, old packaging paths, and stale source-reference assertions.

- [ ] **Step 2: Trace each candidate forward.**

For every candidate construct `source → Gradle/build task → generated resource/native artifact → APK entry → Activity/renderer → runtime action`. A filename search alone cannot classify an item as dead.

- [ ] **Step 3: Trace each candidate backward from the APK contract.**

Identify manifest components, resource IDs, classes, native libraries, assets, and packaged strings that could reintroduce legacy UI or build behavior.

- [ ] **Step 4: Classify without deletion.**

Only classify as DEAD when all forward and backward references are proven absent. Mark uncertainty `NOT PROVEN` rather than guessing.

- [ ] **Step 5: Add a regression scanner if the existing project has no equivalent.**

The scanner must fail on an explicitly defined list of forbidden legacy operational entrypoints/signatures while allowing documentation/history references. It must inspect the built APK, not only source.

- [ ] **Step 6: Commit the graph/scanner separately.**

Do not delete production code in this task.

---

### Task 3: Repair the current semantic KeepAlive contract without weakening it

**Files:**
- Modify only if required: `app/src/test/java/com/alfa/device_ctrl/RuntimeKeepAliveOwnershipContractTest.java`
- Modify production only if semantic inspection proves the implementation itself is wrong: `app/src/main/java/com/alfa/device_ctrl/RuntimeKeepAliveService.java`

**Interfaces:**
- Consumes: exact failing assertion from Task 1.
- Produces: semantic ownership contract that proves multiple owners, duplicate prevention, final-owner shutdown, and lifecycle cleanup.

- [ ] **Step 1: Reproduce the failure.**

Run the exact failing unit test and preserve its output.

- [ ] **Step 2: Compare test expectations with production semantics.**

Verify `owners`, `contains(manager)`, empty-owner handling, and service stop behavior. Formatting differences must never be treated as semantic failures.

- [ ] **Step 3: Write/adjust the smallest semantic assertion.**

Prefer parsing/normalization or a regex that expresses the actual semantic invariant. Never remove an ownership assertion merely to obtain green CI.

- [ ] **Step 4: Run the isolated test, then the full unit suite.**

Record counts and exact result.

- [ ] **Step 5: Commit only the verified fix.**

---

### Task 4: Reconstruct the 159-state Stitch UI contract

**Files:**
- Read: `stitch/stitch_alfa_device_control_v1.0.0.zip`
- Read: `app/src/main/java/com/alfa/device_ctrl/StitchV1ReferenceCatalog.java`
- Read: `app/src/main/java/com/alfa/device_ctrl/StitchV1StateModel.java`
- Read: `app/src/main/java/com/alfa/device_ctrl/StitchV1StatePanel.java`
- Read: `app/src/main/java/com/alfa/device_ctrl/AlfaStitchOperationalPanels.java`
- Read: `app/src/main/java/com/alfa/device_ctrl/AlfaFinalUiHooks.java`
- Read: `app/src/test/java/com/alfa/device_ctrl/AlfaFinalUiPresentationContractTest.java`
- Create: `docs/forensic/stitch-159-state-realization-matrix.md`

**Interfaces:**
- Consumes: canonical ZIP corpus and source implementation.
- Produces: state-by-state mapping of visual structure, action semantics, live/reference status, and proof requirements.

- [ ] **Step 1: Verify ZIP identity and corpus.**

Require exact SHA-256 and exact corpus counts before using any extracted reference as canonical.

- [ ] **Step 2: Enumerate all 159 `code.html` states.**

For each state capture domain, action, native screen, reference-only flag, live-execution claim, and any visual/component identifiers.

- [ ] **Step 3: Map each state to canonical source.**

Trace catalog → state model → state panel → concrete panel/rendering method. Record missing mappings as NOT PROVEN.

- [ ] **Step 4: Identify false-live and reference-only states.**

A state may not claim live execution merely because it exists in the catalog. If backend support is absent, retain explicit reference-only gating rather than faking behavior.

- [ ] **Step 5: Define visual acceptance dimensions.**

The matrix must cover hierarchy, dimensions, spacing, typography, icons, cards, buttons, navigation, colors, loading/error/success/disabled states, scrolling, terminal region, runtime cards, dialogs, and responsive behavior.

- [ ] **Step 6: Commit the matrix.**

No visual patch is allowed solely to make screenshots resemble a reference without corresponding source/component evidence.

---

### Task 5: Upgrade visual forensic from reference-only to APK/DUT comparison

**Files:**
- Modify: `.github/workflows/stitch-visual-forensic.yml`
- Create: `ci/stitch-apk-visual-forensic.sh`
- Create: `ci/stitch-apk-interaction-forensic.sh`
- Create: `docs/forensic/stitch-apk-visual-method.md`
- Test: `app/src/androidTest/` or existing instrumentation test location discovered in Task 2

**Interfaces:**
- Consumes: Task 4 state/reference matrix and a reproducible debug APK.
- Produces: reference screenshot hashes, APK screenshot hashes, normalized diffs, UI hierarchy evidence, and interaction evidence.

- [ ] **Step 1: Prove the existing workflow limitation.**

Document that current visual forensic extracts reference PNGs but does not render the APK or compare images.

- [ ] **Step 2: Define deterministic screenshot conditions.**

Fix device resolution/density/orientation, font scale, locale, animation scale, and initial application state for reproducible captures.

- [ ] **Step 3: Add APK capture.**

Build the exact tested APK, install it on an emulator/device runner where available, launch the canonical activity, navigate to selected Stitch states, and capture screenshots plus UI hierarchy.

- [ ] **Step 4: Add image comparison.**

Normalize only deterministic platform chrome differences. Report pixel/structural differences; do not silently crop or blur substantive UI.

- [ ] **Step 5: Add action verification.**

For every live button in the selected critical-path matrix, invoke the UI control and capture before/after semantic state and visible effect. A click listener alone is insufficient evidence.

- [ ] **Step 6: Commit workflow and harness separately from UI fixes.**

---

### Task 6: Preserve and prove the Debian known-good runtime path

**Files:**
- Read: `app/src/main/java/com/alfa/device_ctrl/RuntimeInstaller.java`
- Read: `app/src/main/java/com/alfa/device_ctrl/RuntimeSessionManager.java`
- Read: `app/src/main/java/com/alfa/device_ctrl/InteractiveSessionContract.java`
- Read: `app/src/main/java/com/alfa/device_ctrl/RuntimeEvidence.java`
- Read: `app/src/main/java/com/alfa/device_ctrl/StitchOperationalActivity.java`
- Read: `.github/workflows/debian-runtime-installer.yml`
- Read: `.github/workflows/debian-runtime-profile.yml`
- Create: `docs/forensic/debian-runtime-baseline.md`

**Interfaces:**
- Consumes: existing Debian evidence.
- Produces: immutable Debian runtime reference trace used to compare other distro implementations.

- [ ] **Step 1: Trace Debian artifact provenance.**

Verify artifact identity, rootfs integrity, PRoot/loader identity, staging, publication, and rollback evidence.

- [ ] **Step 2: Trace actual PTY creation.**

Follow `RuntimeSessionManager` and terminal session creation to prove an actual PTY/process boundary exists.

- [ ] **Step 3: Define safe runtime commands.**

Execute `id`, `uname`, `pwd`, `echo`, environment inspection, and filesystem inspection. Record raw output and semantic state transitions.

- [ ] **Step 4: Verify stdin/stdout/stderr.**

Prove input reaches the process and output returns to the terminal view. Do not use hardcoded expected output as the sole evidence.

- [ ] **Step 5: Verify stop/restart/preservation.**

Test lifecycle transitions and keep-alive ownership without changing the known-good execution architecture.

- [ ] **Step 6: Commit the Debian baseline evidence.**

---

### Task 7: Prove Ubuntu, Alpine, and Kali independently

**Files:**
- Read/modify only after evidence: `forensic-runtime-ubuntu/`, `forensic-runtime-alpine/`, corresponding Kali/other runtime artifact paths discovered from source
- Read: `RuntimeRegistry.java`
- Read: `RuntimeInstaller.java`
- Read: `RuntimeSessionManager.java`
- Create: `docs/forensic/multidistro-runtime-matrix.md`

**Interfaces:**
- Consumes: Debian baseline and each distro's actual artifact/provisioning path.
- Produces: independent PASS/FAIL/NOT PROVEN result for Debian, Ubuntu, Alpine, and Kali.

- [ ] **Step 1: Identify exact artifact and rootfs for each distro.**

Record SHA-256, ABI, ELF interpreter/loader, rootfs structure, and registry binding.

- [ ] **Step 2: Validate PRoot launch per distro.**

Capture actual process/loader result, not only source declarations.

- [ ] **Step 3: Validate shell and PTY per distro.**

Run the safe command set and prove interactive stdin/stdout/stderr.

- [ ] **Step 4: Validate semantic session state per distro.**

Map raw events through `SessionUiState.resolve()` and verify the resulting UI state is semantically correct.

- [ ] **Step 5: Validate lifecycle per distro.**

Test stop, restart, error path, and preservation where the runtime contract requires it.

- [ ] **Step 6: Do not infer one distro from another.**

Every row requires its own evidence.

- [ ] **Step 7: Commit the matrix.**

---

### Task 8: Perform network/protocol forensic from Android to guest process

**Files:**
- Read: `RuntimeInstaller.java`
- Read: `StitchOperationalActivity.java`
- Read: `RuntimeSessionManager.java`
- Read: network evidence panel source identified during Task 2
- Create: `docs/forensic/network-protocol-chain.md`
- Create/modify: CI/DUT diagnostic harness only where an existing safe diagnostic path exists

**Interfaces:**
- Consumes: Android network state and runtime execution evidence.
- Produces: layer-by-layer DNS/TCP/PRoot result.

- [ ] **Step 1: Prove execution-plane network independence.**

Static-scan `RuntimeSessionManager` and `InteractiveSessionContract` for Android network and Java socket APIs prohibited by the architecture. Verify acquisition remains in `RuntimeInstaller`.

- [ ] **Step 2: Capture Android network state.**

Record `Network`, `NetworkCapabilities`, `LinkProperties`, addresses, routes, and DNS information from the permitted Android boundary.

- [ ] **Step 3: Inspect guest resolver configuration.**

For each distro inspect `/etc/resolv.conf`, resolver addresses, IPv4/IPv6 behavior, timeout, and lookup result.

- [ ] **Step 4: Prove DNS resolution.**

Run a deterministic resolver diagnostic and record queried name, resolver, result IP, address family, and timing.

- [ ] **Step 5: Prove TCP path.**

Where tooling and DUT permissions permit, capture socket state and packets. Establish destination IP, route, SYN, SYN/ACK, ACK, and failure/timeout point. If packet capture is unavailable on the DUT, explicitly mark packet-level proof NOT PROVEN rather than substituting ping.

- [ ] **Step 6: Trace PRoot to kernel/network boundary.**

Correlate guest socket behavior with host kernel/network observations and explain the boundary semantically.

- [ ] **Step 7: Commit network evidence.**

---

### Task 9: Deep security and artifact provenance forensic

**Files:**
- Read: `RuntimeInstaller.java`
- Read: native packaging/build scripts and artifact manifests identified in Task 2
- Read: `.github/workflows/forensic-*.yml`
- Create: `docs/forensic/security-provenance-chain.md`
- Extend existing tests only where a concrete uncovered invariant is identified

**Interfaces:**
- Consumes: artifact and runtime evidence.
- Produces: security invariant matrix with VERIFIED or NOT PROVEN status.

- [ ] **Step 1: Verify SHA-256 and artifact binding.**

Check PRoot, loader, runtime archive/rootfs, and APK hashes against canonical metadata.

- [ ] **Step 2: Verify ELF/ABI/interpreter constraints.**

Use `readelf`/equivalent in a reproducible CI or DUT environment where available.

- [ ] **Step 3: Verify staging containment.**

Prove staging UUID and `PROOT_TMP_DIR` containment.

- [ ] **Step 4: Verify archive traversal defenses.**

Inspect and test regular files, symlinks, hardlinks, absolute paths, and parent traversal against the actual extraction helpers.

- [ ] **Step 5: Verify atomic publication/rollback.**

Prove READY evidence is only published after validation and that failure does not leave a partially trusted runtime.

- [ ] **Step 6: Verify secret/log boundaries.**

Ensure diagnostic output does not expose signing secrets or unrelated sensitive material.

- [ ] **Step 7: Commit the security matrix.**

---

### Task 10: Build forensic and legacy-free APK verification

**Files:**
- Read/modify: `.github/workflows/apk-readiness.yml`
- Read: `.github/workflows/forensic-apk-zipalign.yml`
- Read: `app/build.gradle`
- Read: `AndroidManifest.xml`
- Create/modify: `ci/apk-legacy-signature-scan.sh`
- Create: `docs/forensic/apk-provenance.md`

**Interfaces:**
- Consumes: canonical source and all prior gates.
- Produces: reproducible APK with exact source SHA, manifest, resources, native libraries, ABI, hashes, alignment, and legacy scan evidence.

- [ ] **Step 1: Build from exact commit.**

The build job must check out the exact source SHA under test and produce one named APK artifact.

- [ ] **Step 2: Verify manifest.**

Confirm canonical launcher is `StitchOperationalActivity`; inspect every exported component and service for unintended legacy operational entrypoints.

- [ ] **Step 3: Inspect APK resources and dex/classes.**

Search packaged resources/classes/assets for legacy signatures and verify expected Stitch/runtime surfaces exist.

- [ ] **Step 4: Inspect native libraries.**

Verify ABI, ELF headers/interpreter, alignment requirements, and expected native payloads.

- [ ] **Step 5: Compute exact APK SHA-256.**

Persist the hash with source SHA and build run ID.

- [ ] **Step 6: Publish only the verified APK artifact.**

Do not substitute a locally built APK for the canonical artifact without exact hash identity.

---

### Task 11: Remove only legacy components proven DEAD

**Files:**
- Only the exact paths classified DEAD by Task 2
- Corresponding tests/workflows/resources that are proven migration remnants
- `docs/forensic/legacy-dependency-graph.md`

**Interfaces:**
- Consumes: signed-off DEAD classifications and dependency graph.
- Produces: minimal cleanup commit with no active dependency removed.

- [ ] **Step 1: Reconfirm every deletion candidate.**

Repeat source, build, packaging, and runtime reference checks immediately before deletion.

- [ ] **Step 2: Write failing regression assertions first.**

The regression scanner must ensure the removed legacy entrypoint cannot return to the APK unnoticed.

- [ ] **Step 3: Delete only proven DEAD files/references.**

Do not delete `ACTIVE DEPENDENCY` or `NOT PROVEN` items.

- [ ] **Step 4: Run unit/contract tests.**

Verify no architectural contract regresses.

- [ ] **Step 5: Rebuild and inspect APK.**

Verify manifest, resources, classes, native libraries, and legacy scan.

- [ ] **Step 6: Commit cleanup separately.**

---

### Task 12: Exact APK installation and DUT verification

**Files:**
- Read: APK artifact/provenance from Task 10
- Read/modify: instrumentation/DUT harness discovered in Tasks 5 and 7
- Create: `docs/forensic/dut-exact-apk-report.md`

**Interfaces:**
- Consumes: one exact APK SHA and one exact source/build identity.
- Produces: installation identity, UI screenshots, interaction logs, runtime logs, and test matrix from the same installed package.

- [ ] **Step 1: Verify DUT identity and test environment.**

Record device model/build, Android version, ABI, density/resolution, locale, animation scale, and relevant permissions.

- [ ] **Step 2: Verify APK identity before install.**

Compute SHA-256 of the exact APK file.

- [ ] **Step 3: Install that exact APK.**

Use adb or an equivalent controlled DUT mechanism. Record package/version/signature and installed artifact identity.

- [ ] **Step 4: Launch only the canonical launcher.**

Capture logcat and UI hierarchy from cold start.

- [ ] **Step 5: Execute critical UI paths.**

Test navigation, runtime selection, terminal launch, terminal input, terminal output, stop/restart, dialogs, errors, and every critical live action from the Stitch matrix.

- [ ] **Step 6: Execute all four distro paths independently.**

Do not reuse Debian evidence for Ubuntu, Alpine, or Kali.

- [ ] **Step 7: Capture visual evidence.**

For each selected Stitch state capture the DUT screenshot and UI hierarchy and compare against the reference.

- [ ] **Step 8: Capture network evidence.**

Run DNS and TCP diagnostics and preserve logs/packet capture when permissions allow.

- [ ] **Step 9: Verify exact artifact identity after testing.**

The installed/tested package must still correspond to the recorded APK SHA/provenance.

- [ ] **Step 10: Commit only the forensic report/harness changes.**

Never commit generated DUT secrets or private captures.

---

### Task 13: Root-cause loop for every failure

**Files:**
- Modify only the smallest production/test/CI files implicated by evidence.
- Create: `docs/forensic/root-cause-log.md`

**Interfaces:**
- Consumes: any failure from Tasks 1–12.
- Produces: verified minimal correction and a new evidence cycle.

- [ ] **Step 1: Classify the symptom layer.**

Use `source`, `build`, `packaging`, `APK`, `launcher`, `render`, `action`, `runtime`, `PTY`, `state`, `DNS`, `socket`, `TCP`, `PRoot`, `kernel`, or `DUT`.

- [ ] **Step 2: Trace the data/state flow.**

For binary anomalies inspect structure/headers/segments/offset semantics; for network failures trace resolver/IP/route/socket/TCP; for UI failures trace reference→source→APK→render→action.

- [ ] **Step 3: Form a falsifiable root-cause statement.**

Do not call an error message itself the root cause.

- [ ] **Step 4: Add a regression test before patching when practical.**

The test must fail for the reproduced defect and pass after the minimal fix.

- [ ] **Step 5: Patch minimally.**

Do not restructure canonical architecture unless evidence proves an architectural contradiction.

- [ ] **Step 6: Re-run the affected gate and all dependent gates.**

A fix is not complete until downstream packaging/DUT evidence remains valid.

- [ ] **Step 7: Record the causal chain.**

Every failure gets a before/after evidence record.

---

### Task 14: Final forensic closure gate

**Files:**
- Read: all `docs/forensic/*.md`
- Read: final CI run evidence
- Read: final APK artifact/provenance
- Read: final DUT report
- Create: `docs/forensic/final-milestone-verdict.md`

**Interfaces:**
- Consumes: every prior gate's evidence.
- Produces: exactly one final classification: `🟡 YELLOW — MORE EVIDENCE REQUIRED`, `🔴 RED — ARCHITECTURAL CONTRADICTION`, or `🟢 PASS — GREEN — LOCKED 🔒`.

- [ ] **Step 1: Verify every required evidence row.**

Required rows: Stitch ZIP identity, 159-state mapping, legacy elimination, build, APK provenance, manifest, resources, native payload, Debian, Ubuntu, Alpine, Kali, PTY, shell, stdin/stdout/stderr, semantic session state, UI fidelity, buttons/actions, DNS, socket, TCP, PRoot, security, exact installation, exact APK identity, DUT execution.

- [ ] **Step 2: Reject any hidden NOT PROVEN.**

If any required row remains NOT PROVEN, final verdict cannot be PASS.

- [ ] **Step 3: Reject source-only evidence.**

Source, unit tests, and build success cannot substitute for APK/DUT evidence.

- [ ] **Step 4: Reject distro inference.**

Debian PASS does not imply Ubuntu/Alpine/Kali PASS.

- [ ] **Step 5: Issue final verdict.**

Use GREEN LOCKED only when every required chain is independently proven and no unexplained failure remains.

---

## Execution order

The execution order is intentionally dependency-driven:

`1 Baseline → 2 Legacy graph → 3 Current test failure → 4 Stitch state matrix → 5 APK visual harness → 6 Debian baseline → 7 Four-distro proof → 8 Network forensic → 9 Security/provenance → 10 APK forensic → 11 Dead legacy cleanup → 12 Exact APK/DUT → 13 Root-cause loops → 14 Final gate`

A task that discovers a failure opens Task 13 and then resumes the dependent downstream gate. No later gate may convert an earlier `NOT PROVEN` into `VERIFIED` by inference.

## Self-review checklist

- [ ] All user requirements map to a concrete task above.
- [ ] Legacy deletion is evidence-gated.
- [ ] Debian remains the known-good baseline.
- [ ] Ubuntu, Alpine, and Kali each require independent evidence.
- [ ] Terminal proof requires actual PTY/process/input/output/state.
- [ ] Network proof goes beyond ping and reaches DNS/socket/TCP where tooling permits.
- [ ] PRoot/network architecture remains separated.
- [ ] Stitch visual forensic compares actual APK/DUT rendering rather than only extracting references.
- [ ] APK SHA identity is carried through build→artifact→install→test.
- [ ] Final PASS explicitly rejects any NOT PROVEN item.
