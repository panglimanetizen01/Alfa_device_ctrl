# Stitch v1 Interaction Forensic Audit — 2026-09-13

## Scope

Canonical source: `master` at `492c9adddf8f7da192826a14a767c6fc594e763c`.
Repair branch starts from PR #73 head `36aafb249d49ca65c5489be158a74803c809404b`.

DUT evidence reported:

- Debian PTY executes real commands successfully.
- Runtime screen had no Back control.
- Secondary action buttons appeared clickable but several produced no visible state change.
- Ubuntu/Alpine/Kali installation actions were not usable from the visible runtime flow.
- Terminal selection Copy produced no clipboard result.
- Terminal context Paste/Copy was not equivalent to a normal terminal clipboard path.
- Appearance controls changed transient state but did not persist reliably.

## Root-cause findings

### RC-01 — UI presentation detached the canonical action views

`AlfaFinalUiPresentation` previously removed the original MainActivity hierarchy and retained legacy `Button`/`View` objects as action dependencies. The new bottom navigation then invoked `performClick()` on detached legacy controls. Some controls executed their listeners against detached legacy output views, so the action could run without a visible state transition.

Disposition: **REPLACE** the detached-view action boundary with a native presentation that calls the canonical runtime/session operations and owns its own screen state.

### RC-02 — Runtime dashboard had no navigation contract

The runtime screen was inserted as a detached legacy panel through `showPanel(runtimePanel)`. There was no application-level navigation state and no system Back registration for the panel.

Disposition: **REPAIR** with `AlfaUiNavigation`, an explicit Back button, and Android 13+ `OnBackInvokedDispatcher` registration while secondary screens are active.

Android reference: https://developer.android.com/reference/android/window/OnBackInvokedCallback

### RC-03 — Terminal clipboard callbacks were explicit no-ops

`RuntimeSessionManager` implemented `TerminalSessionClient.onCopyTextToClipboard()` and `onPasteTextFromClipboard()` as empty methods. The terminal selection layer therefore had no bridge to Android's system clipboard even though `TerminalBuffer` already exposed transcript/selection text.

Disposition: **REPAIR** through `TerminalClipboardBridge` and Android `ClipboardManager`/`ClipData` integration. A visible `COPY` action additionally copies the complete terminal transcript.

Android reference: https://developer.android.com/develop/ui/views/touch-and-input/copy-paste

### RC-04 — Settings were transient UI-only state

Font size and line height were held only in the presentation instance. Theme selection directly changed the terminal palette but did not persist the selected preset. Cursor selection sent an ANSI sequence but did not persist the selected style.

Disposition: **REPAIR** using Activity-scoped `SharedPreferences` for font size, line height, theme, and cursor style, then restore those settings when the presentation is recreated.

### RC-05 — Secondary overlays lacked system Back handling

Network and diagnostics were displayed as full-screen overlays with a visible `BACK` button but did not register a system Back callback. On Android 13+ this can bypass the expected in-app return path.

Disposition: **REPAIR** by registering an `OnBackInvokedCallback` at overlay priority and unregistering it when the overlay closes.

### RC-06 — Debian being immediately available is intentional Gate 7 behavior

The packaged Gate 7 launch asset is bound to Debian and `RuntimeStartupCoordinator` can auto-start an authorized, ready Debian runtime. This is not evidence of an accidental Debian install bypass. Other runtimes remain registry-driven install targets.

Disposition: **KEEP** the provenance boundary; improve visible runtime action feedback rather than disabling the canonical Gate 7 path.

### RC-07 — Runtime installer itself is registry-bound and verified

`RuntimeInstaller` validates the requested runtime against `RuntimeRegistry`, verifies the archive SHA-256, verifies packaged `libproot.so` and `libproot-loader.so`, extracts into a staging directory, runs a runtime smoke contract, and publishes atomically. Existing tests cover verified installation, loader denial, traversal denial, checksum denial, and rollback preservation.

Disposition: **KEEP** installer security boundary. The visible runtime dashboard must call this canonical path directly and expose resulting state.

## Implementation

- Added deterministic `AlfaUiNavigation` contract.
- Added tested `TerminalClipboardBridge` contract.
- Replaced detached legacy-panel wiring in `AlfaFinalUiPresentation` with owned runtime/lanes screens.
- Added explicit runtime Back and Android predictive/system Back handling.
- Added direct runtime `OPEN`, `INSTALL`, and `VERIFY` controls against the canonical registry/state projection.
- Added terminal `COPY` for full transcript and repaired selection Copy/Paste callbacks.
- Added persisted terminal font, line-height, theme, and cursor settings.
- Added system Back handling for Alfa RF/Diagnostics overlays.
- Extended UI CI to execute navigation and clipboard contracts.

## Verification boundary

CI must pass before a new APK is accepted. DUT verification remains mandatory for:

1. Runtime screen entry and Back button.
2. Android system Back from runtime/network/diagnostics overlays.
3. Debian OPEN and interactive PTY.
4. Ubuntu, Alpine, and Kali INSTALL with visible state transition and READY evidence.
5. Terminal selection Copy into Android clipboard.
6. Terminal Paste from Android clipboard into PTY.
7. Full transcript COPY.
8. Font, line-height, cursor, and theme persistence after recreation.
9. Minimize/fullscreen/kill and all bottom navigation actions.
10. No regression of Gate 7 provenance or runtime process-group boundaries.

No DUT PASS is claimed by this document alone.
