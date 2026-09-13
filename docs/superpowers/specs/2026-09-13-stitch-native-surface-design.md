# Stitch v1 Native Surface Design

**Status:** Approved for implementation by the task request.

## Goal
Implement the supplied Stitch v1 artifact as a real native Android presentation surface in Alfa Device Ctrl, while preserving Alfa runtime, session, storage, security, networking, MCP, and evidence boundaries.

## Source of truth
The only Stitch input is `stitch/stitch_alfa_device_control_v1.0.0.zip`, SHA-256 `7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d`, containing 159 `code.html` states, 160 PNG files, and 7 markdown files. The ZIP is an implementation input, not merely a visual reference.

## Forensic findings driving implementation
The supplied corpus contains a terminal-first operational workstation, runtime dashboard, security diagnostics, storage/VFS policy dialogs, project explorer, split-pane terminal workspace, floating terminal/overlay states, appearance/typography settings, session multiplexer, session lifecycle dialogs, and refined Alfa control-center states. Observed controls include 48dp-class interactive targets, Obsidian surfaces, cyan/green/amber/red status semantics, PTY accessory keys, session switching/spawn/stop controls, appearance presets, typography steppers, and evidence/diagnostic panels.

## Architecture
Native Android Views remain the substrate. `RuntimeSessionManager` remains the only owner of actual PTY/runtime execution. `AlfaNetworkPanel` remains read-only evidence from Android network APIs and runtime evidence; no UI state may manufacture DNS, routes, NAT, firewall, VPN, sockets, packet capture, or security telemetry. Unsupported Stitch states are implemented as capability-gated native surfaces with truthful disabled/blocked states rather than fake backend behavior.

## Network/data-flow boundary
Android-side network evidence uses `ConnectivityManager`, `NetworkCapabilities`, and `LinkProperties` for transports, capabilities, addresses, DNS, routes, interface and proxy information. Linux/runtime network claims are emitted only from the selected runtime. This separation follows Android's documented dynamic network model and Linux Netlink/socket boundaries.

## UI implementation requirements
1. Replace text-only placeholder operational panels with structured native cards, rows, chips, dialogs, drawers, split panes, session rows, and settings controls matching the observed Stitch hierarchy and tokens.
2. Implement the observed terminal shell, session multiplexer, PTY accessory bar, appearance settings, control center, storage/project flows, diagnostics, split/floating capability surfaces, and lifecycle dialogs where Alfa has a real capability boundary.
3. Bind visual state to the 159-state catalog, but do not execute reference HTML/JavaScript.
4. Keep 48dp minimum interactive targets and explicit content descriptions for icon-only controls.
5. Keep Android window/IME inset handling compatible with the existing terminal session.
6. Do not introduce a new UI framework or alter the runtime architecture.
7. Preserve canonical host storage transparency: host visibility remains SAF/device-layer evidence, never an invented direct app path.
8. Preserve security semantics: capability unavailable is shown as unavailable/blocked, never as successful telemetry.

## Asset boundary
The raw ZIP remains the immutable source artifact. CI forensic tooling must derive a deterministic asset/state manifest from the ZIP. Only assets proven to be actual native UI assets, rather than screenshots or reference-only artifacts, may be copied into Android resources. Asset provenance must record source ZIP path and SHA-256.

## Verification gates
- Static contract tests prove the native surface contains the required Stitch domains and controls.
- Unit tests cover state-to-surface mapping and capability gating.
- CI runs the forensic ZIP hash/count check and native build/tests.
- Android instrumentation tests verify terminal launch, navigation, settings, session controls, and network evidence remain operational.
- Final milestone requires CI evidence, local Termux sync/build/test evidence, fresh DUT install, and runtime/session/network verification. No PASS GREEN LOCKED claim is valid before all are evidenced.
