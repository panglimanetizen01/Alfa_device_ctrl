# Alfa Device Ctrl — Final Evidence Package

Status model: `PASS` = executed evidence proves the requirement; `FAIL` = executed evidence disproves it; `NOT_MEASURED` = no valid execution evidence yet. A reference image, source inspection, or mocked/simulated state never counts as PASS for a runtime capability.

## Current release candidate

- Repository: `panglimanetizen01/Alfa_device_ctrl`
- Release branch: `fix/final-ui-alfa-reconciliation`
- PR: `#44`
- Base: `test/ui-session-ownership-red2`
- Latest source commit at package creation: `8cdac2e19d87607e4b1caa94cfce21be5e14e4f4`
- UI reference: `UI_alfa_device_control_final.zip`, 118 reference states

## Evidence matrix

| Gate | Status | Evidence / reason |
|---|---|---|
| UI reference inventory: 118 states | PASS | Reference archive audited; 118 HTML states and corresponding visual references identified. |
| Native UI theme reconciliation | PASS | Terminal Obsidian tokens reconciled in native Alfa UI. |
| Android build / instrumentation APK compilation | PASS | UI Milestone run #21 assembled both debug and androidTest APKs successfully before emulator setup failed. |
| Android API 34 instrumentation execution | NOT_MEASURED | Run #21 failed before test execution because `avdmanager` could not resolve the requested `Pixel_2` device profile. Workflow profile was corrected to `pixel_2`; run #23 is executing. |
| UI lifecycle/recreation contract | NOT_MEASURED | Test exists, but valid execution evidence is pending. Runtime reattachment code was additionally corrected to use the canonical `rebindListener()` API rather than reflection. |
| Accessibility labels / 48dp targets | NOT_MEASURED | Instrumentation test exists; execution evidence pending. |
| Screenshot capture | NOT_MEASURED | Instrumentation test exists; no valid emulator screenshot was captured in run #21 because the AVD was never created. |
| 118-state visual regression | NOT_MEASURED | Reference exists; real Android captures have not yet been generated and compared. |
| Android 14 physical DUT | NOT_MEASURED | Requires the actual ARM64 Android 14 device. |
| ARM64 real runtime / PRoot loader | NOT_MEASURED | CI proves ARM64 ELF loader packaging/build only; physical execution remains unproven. |
| Shizuku/Rish execution | NOT_MEASURED | Requires physical DUT evidence. |
| Termux RunCommandService execution | NOT_MEASURED | Requires physical DUT evidence. |
| Termux:API callbacks | NOT_MEASURED | Requires physical DUT evidence. |
| SAF host-storage grant/persistence | NOT_MEASURED | Source path is implemented; real persisted grant and host-visible evidence require DUT execution. |
| Overlay terminal | NOT_MEASURED | Source implementation exists; real overlay/IME behavior requires DUT execution. |
| Split PTY/session isolation | NOT_MEASURED | Source implementation exists; distinct live PTY evidence requires DUT execution. |
| Runtime policy enforcement | NOT_MEASURED | Source implementation exists; mutation-to-enforcement execution evidence pending. |
| MCP server build/tests | NOT_MEASURED | Repository contains Go MCP server and tests; release-candidate execution evidence must be attached before release. |
| Security stack integration | NOT_MEASURED | Physical execution and security evidence pending. |
| Final storage/permission verification | NOT_MEASURED | Android 14/DUT execution pending. |
| Full release verification | BLOCKED | Cannot be PASS while required runtime/DUT evidence is NOT_MEASURED. |

## UI GREEN GATE

**NOT GRANTED.** It requires all UI requirements to have valid execution evidence, including the 118-state visual comparison, accessibility, lifecycle/session behavior, and Android 14 verification.

## UI LOCKED

**NOT GRANTED.** UI LOCK may only be declared after UI GREEN GATE. Once locked, UI changes require a demonstrated requirement or reproducible bug plus regression evidence.

## Full Alfa Release

**NOT RELEASED.** The release candidate remains on the isolated PR branch until CI, instrumentation, physical DUT, MCP, runtime, security, storage, and evidence gates are complete.

## Evidence rules

1. No PASS from source inspection alone for runtime behavior.
2. No PASS from simulation, mock, placeholder, hardcoded state, or reference screenshot.
3. Every failure must record the exact observed root cause and corrective commit.
4. Every `NOT_MEASURED` item must either acquire real evidence or remain explicitly unmeasured; it must not be silently converted to PASS.
5. Final release requires a reproducible evidence trail tied to the exact source commit under test.
