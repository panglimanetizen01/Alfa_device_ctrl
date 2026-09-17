# Stitch 159-State Forensic Realization Matrix — 2026-09-15

## Reference corpus evidence

The repository forensic artifact for the canonical Stitch ZIP records:

- ZIP SHA-256: `7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d`
- 159 `code.html` states
- 160 PNG files/assets
- 7 Markdown files
- 326 total files
- 3,450 extracted button records across the 159 HTML states
- 157 extracted input records

The 159-state catalog is therefore substantially larger than the current native Android shell. The reference corpus contains state families for terminal/runtime, sessions, split panes, floating terminals, settings/appearance, project/storage, security/audit, and network/socket behavior.

## Domain distribution derived from the locked forensic manifest

| Domain | Reference states |
|---|---:|
| settings | 29 |
| sessions | 26 |
| floating | 23 |
| project-storage | 13 |
| split | 13 |
| storage-policy | 12 |
| security | 12 |
| terminal | 11 |
| network | 7 |
| audit | 6 |
| runtime | 5 |
| other | 2 |
| **Total** | **159** |

## Canonical source mapping

The current implementation maps every catalog ID through `StitchV1ReferenceCatalog` and `StitchV1StateModel` to one native `AlfaUiNavigation.Screen`. This is a provenance/state mapping, not evidence that every reference visual or interaction has a native equivalent.

Current native operational panels are implemented by `AlfaStitchOperationalPanels` for NETWORK, SECURITY, STORAGE, AUDIT, PROJECT, SESSIONS, SPLIT, FLOATING, APPEARANCE, SETTINGS, and DIAGNOSTICS. `StitchOperationalActivity` separately owns the terminal/runtime shell.

## Critical proven contradiction: session states claim live execution but backend exposes one PTY

`StitchV1StateModel.resolve()` sets `claimsLiveExecution=true` for all non-reference-only states whose domain is `TERMINAL`, `RUNTIME`, or `SESSIONS`.

The Stitch catalog contains explicit multi-session states including:

- `15_session_multiplexer_ubuntu_kali_alpine_mobile_view`
- session 3/4/5/6 spawn states
- session switch states
- maximum-session state
- all-5-active-session manager state
- floating terminal session switcher states

The current `RuntimeSessionManager` contains a single `TerminalSession session` field and a single `currentSession()` accessor. `AlfaStitchOperationalPanels.sessions()` explicitly reports `MULTI-PTY BACKEND=NOT_EXPOSED_BY_CANONICAL_MANAGER` and disables session switching. It renders reference lanes but does not create those PTYs.

Therefore this is not a cosmetic mismatch. It is an implementation-capability gap between the Stitch live-execution contract and the canonical execution backend.

### Classification

**RED CANDIDATE — ARCHITECTURAL CONTRADICTION, pending implementation-resolution evidence.**

The contradiction is specifically:

`Stitch live session states → claimsLiveExecution=true`

versus

`RuntimeSessionManager → exactly one TerminalSession`

versus

`SESSIONS panel → explicit multi-PTY capability gate / disabled switch controls`.

The correct resolution is not to relabel the Stitch states as reference-only. The approved milestone requires real interaction and does not permit lowering the reference contract. The required implementation direction is a real multi-session orchestration layer built on multiple independently owned `RuntimeSessionManager` instances/contracts, with no second runtime registry and with `RuntimeKeepAliveService` ownership preserved.

## Other high-priority reference families

### Split pane

The catalog contains horizontal/vertical split and synchronized-input states. The current panel explicitly disables both split modes because a second PTY is not exposed. These states currently do not claim live execution under `StitchV1StateModel`, but the UI is still required to realize the reference only if the milestone treats those states as live. They must remain explicitly classified rather than visually implying a capability that is absent.

### Floating terminal

The catalog contains overlay permission, interactive floating window, bubble/minimize/restore, keyboard/IME, opacity/size, command execution, SIGINT, clear, and session-monitoring states. The current panel is capability-gated and does not provide a real interactive floating terminal session. This remains **NOT PROVEN / not live-equivalent** until an actual Android window/session path is demonstrated on a DUT.

### Settings/appearance

The reference family contains persistent theme, font, density, line-height, cursor, contrast, and datastore/proto states. The current native appearance panel is a reduced native adaptation and does not yet demonstrate persistence/equivalence across the full reference state family.

### Project/storage/network/security

The current implementation has native panels and evidence boundaries for these domains, but source presence is not APK/DUT proof. Each live interaction must be traced to the corresponding Android action/backend and verified after packaging.

## Required next implementation gate

Before final PASS, the following must be demonstrated:

1. A real multi-session orchestration layer exists without duplicating `RuntimeRegistry`.
2. Each session has its own `InteractiveSessionContract`/`RuntimeSessionManager` ownership and exact runtime binding.
3. Multiple real PTYs can be spawned, independently selected, stopped, and observed.
4. Session switch UI changes the actual attached terminal surface, not only a label.
5. KeepAlive ownership correctly tracks multiple session managers and stops only after the final owner exits.
6. Stitch session states can be driven by real state transitions rather than hardcoded reference rows.
7. The resulting APK is installed and tested on the DUT.

Until these conditions are proven, the 159-state Stitch requirement cannot be classified PASS.
