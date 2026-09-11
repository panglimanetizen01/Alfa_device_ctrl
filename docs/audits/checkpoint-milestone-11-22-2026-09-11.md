# Alfa Device Ctrl — Checkpoint Milestone 11–22

Date: 2026-09-11
Source of Truth: GitHub repository `panglimanetizen01/Alfa_device_ctrl`
Working branch: `fix/final-ui-alfa-reconciliation`
Base master: `66def003ad521e6f844b40fffa73bcf94f0dd178`
Checkpoint branch HEAD at creation: `7950afbafbabf9ef883b3640d12c01845c943c13`

## Working contract

- GitHub is canonical/source of truth; local Alfa/Termux is verification only.
- No evidence -> no claim -> no command -> no patch.
- Workflow: RESEARCH -> EVIDENCE -> ROOT CAUSE -> IMPLEMENT -> TEST -> VERIFY -> NEXT.
- Use primary upstream specifications/source/issues before technical decisions.
- Use systematic debugging and TDD where applicable.
- Never convert UNKNOWN/NOT_MEASURED/BLOCKED into PASS.
- Milestones are locked only from execution evidence, not static presence or UI labels.

## Current state

### Milestones 1–5
PASS GREEN LOCKED from Android emulator instrumentation evidence: 10/10 tests, 0 failed, 0 skipped.

### Milestones 6–10
Implementation exists, but the milestone is NOT LOCKED until the latest CI run and semantic backend evidence are verified. Gate 9 required correction because the original UI exposed an evidence-only directory override surface with `NOT_AVAILABLE/NOT_MEASURED`, which was not equivalent to a real runtime policy mutation path.

The correction now adds a persisted runtime directory override model/store and binds the rules to newly created PRoot sessions. The contract intentionally scopes application to NEW_SESSION rather than claiming live mutation of an existing PTY.

### CI state at checkpoint

The current UI instrumentation workflow run for the latest branch HEAD is in progress. Compile and emulator results must be inspected before locking Milestones 6–10.

## Next mandatory sequence

11. Termux Execution & API UI — real execution lane + evidence.
12. Project Editor + Socket Diagnostics.
13. 118-Screen Capability Matrix — map every reference state to a real capability.
14. Remove/block every simulation, mock, placeholder, demo-only state, and hardcoded success path.
15. Android Instrumentation Test — complete UI/backend contract.
16. Build + CI Verification — wait for actual GitHub Actions results.
17. DUT Functional Verification — Android 14 / SDK 34 + real runtime.
18. DUT Visual Regression — compare against all 118 reference states.
19. Accessibility Verification — semantics, labels, keyboard/touch target, adaptive layout.
20. Final Security / Storage / Permission Verification.
21. Final Evidence Package — record PASS/FAIL/NOT_MEASURED/BLOCKED with provenance.
22. UI GREEN GATE — all UI requirements validated.

## Research basis for Milestone 11

Primary Termux source confirms third-party execution through `RunCommandService` and the `RUN_COMMAND` contract, including command path, arguments, workdir, runner, and optional PendingIntent result delivery. Alfa must use the upstream contract rather than inventing a private execution protocol.

Primary Termux API source confirms Termux:API is an add-on exposing Android device functionality to Termux commands/programs and that the helper communicates with the API receiver using sockets. Alfa must expose evidence from actual API execution rather than presenting a static capability label.

Android's `LocalSocket` API is a real UNIX-domain socket API and supports stream/datagram/seqpacket sockets; socket diagnostics must therefore inspect an actual endpoint/connection rather than mock status.

## Locking rule

No milestone from 11 onward may be declared complete merely because an Activity launches or a text token exists. Each capability must have a traceable path to the canonical backend and an execution test that distinguishes PASS from unavailable or unmeasured states.
