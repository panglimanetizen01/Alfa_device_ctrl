# Stitch v1 Interaction Forensic Audit — 2026-09-13

## Scope

Canonical source: `master` at `492c9adddf8f7da192826a14a767c6fc594e763c`.
Implementation branch: `work/stitch-native-implementation`.
Supplied implementation artifact: `stitch_alfa_device_control_v1.0.0.zip`.
Artifact SHA-256: `7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d`.
Reference corpus: 159 `code.html` states.

DUT evidence previously reported:

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

### RC-03 — Terminal clipboard callbacks were explicit no-ops

`RuntimeSessionManager` implemented `TerminalSessionClient.onCopyTextToClipboard()` and `onPasteTextFromClipboard()` as empty methods. The terminal selection layer therefore had no bridge to Android's system clipboard even though `TerminalBuffer` already exposed transcript/selection text.

Disposition: **REPAIR** through `TerminalClipboardBridge` and Android `ClipboardManager`/`ClipData` integration. A visible `COPY` action additionally copies the complete terminal transcript.

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

### RC-08 — The first Stitch state model used synthetic action counts

The initial `StitchV1StateModel` assigned a fabricated integer `supportedActionCount` from string heuristics. That proved catalog coverage but did not prove that a state could perform a real action. This was insufficient for the requirement that the ZIP be implemented rather than merely catalogued.

Disposition: **REPLACE** synthetic counts with explicit state actions. Each of the 159 states now resolves to a deterministic native surface and an explicit action boundary. Known executable terminal states map to real `RuntimeSessionManager.runRuntimeCommand()` commands; session-stop states map to the canonical session lifecycle; simulation/result states remain non-executable and cannot manufacture success telemetry.

### RC-09 — Stitch runtime-result states must not be replayed as fake success

The ZIP contains states named as completed or successful outcomes, including completed package operations, successful webhook dispatch, successful policy restore, and a terminated session. Replaying these screens as if they were current runtime truth would violate Alfa's evidence model.

Disposition: **BLOCK** replay of result-state claims. These states remain visible as reference dispositions and route to the live native surface. A live result can only be produced by the canonical backend after an actual user action and observed result.

### RC-10 — Network claims must remain evidence-bounded

The Stitch corpus contains tunnel, socket, Matrix webhook, DNS/network, and security-console states. Android's `ConnectivityManager`, `NetworkCapabilities`, and `LinkProperties` can establish current network state, transports, routes, DNS, proxy, and capability information, but they do not by themselves prove firewall rules, NAT tables, packet capture, VPN encryption, or privileged network-namespace isolation.

Disposition: **KEEP** `AlfaNetworkPanel` read-only and evidence-backed. Unsupported network/security claims remain explicitly unclaimed until direct privileged evidence exists.

## Implementation

- Added deterministic `AlfaUiNavigation` contract.
- Added tested `TerminalClipboardBridge` contract.
- Replaced detached legacy-panel wiring in `AlfaFinalUiPresentation` with owned runtime/lanes screens.
- Added explicit runtime Back and Android predictive/system Back handling.
- Added direct runtime controls against the canonical registry/state projection.
- Added terminal `COPY` for full transcript and repaired selection Copy/Paste callbacks.
- Added persisted terminal font, line-height, theme, and cursor settings.
- Added system Back handling for Alfa RF/Diagnostics overlays.
- Added native 159-state Stitch catalog and deterministic state mapping.
- Replaced synthetic state action counts with explicit executable/reference action semantics.
- Bound executable Stitch command states to the canonical `RuntimeSessionManager` boundary with confirmation and live result reporting.
- Bound session-stop states to `RuntimeSessionManager.stop()` rather than simulated termination.
- Kept simulation/result states non-executable.
- Kept network evidence read-only and bounded to Android/Linux observations.
- Extended UI CI contracts to cover state/action determinism and non-fabrication boundaries.

## Primary-source constraints used for reconciliation

- Android navigation: navigation destinations must have predictable state and Back behavior.
- Android networking: `ConnectivityManager`, `NetworkCapabilities`, and `LinkProperties` are the authoritative app-visible network evidence surfaces for connectivity, transports, routes, DNS, proxy and capabilities.
- Android security: `NetworkSecurityPolicy` and Network Security Configuration define observable cleartext/TLS policy; the UI must not infer privileged kernel security state from them.
- Android accessibility: interactive targets must remain at least 48dp and interactive elements require meaningful content descriptions.
- Android 16KB: native ELF load segments and APK zip alignment must be verified independently with `llvm-readelf` and `zipalign -P 16`.
- MCP tools: sensitive tool invocation must preserve an explicit human-in-the-loop boundary; tool metadata and authorization do not justify fabricated execution results.

## Verification boundary

CI must pass on the **current implementation HEAD**, not on an earlier commit, before a new APK is accepted. DUT verification remains mandatory for:

1. Runtime screen entry and Back button.
2. Android system Back from runtime/network/diagnostics overlays.
3. Debian OPEN and interactive PTY.
4. Ubuntu, Alpine, and Kali INSTALL with visible state transition and READY evidence where the canonical backend supports those operations.
5. Terminal selection Copy into Android clipboard.
6. Terminal Paste from Android clipboard into PTY.
7. Full transcript COPY.
8. Font, line-height, cursor, and theme persistence after recreation.
9. Minimize/fullscreen/kill and all bottom navigation actions.
10. Native Stitch state catalog: each of 159 states resolves deterministically; executable states invoke only canonical runtime/session boundaries; simulation/result states never claim live execution.
11. Network evidence: transport, capability, route, DNS, proxy, IPv4/IPv6 and bounded socket observations remain read-only and evidence-backed.
12. No regression of Gate 7 provenance, process-group cleanup, MCP authorization boundaries, or runtime installer security.

No DUT PASS is claimed by this document alone.
