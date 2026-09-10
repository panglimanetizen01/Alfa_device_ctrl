# Alfa Device Ctrl — Final UI Design Reconciliation

## Goal
Adopt `UI_alfa_device_control_final.zip` as the presentation/design source of truth while implementing its approved visual and interaction model in the existing native Android runtime architecture without importing mock state, invalid storage paths, or unverified capabilities.

## Design Source
The uploaded final artifact contains 121 visual screen directories plus shared Terminal Obsidian design tokens, a real-runtime architecture note, and a Proto DataStore locale note. Google Stitch is treated as a design/prototyping source only; its HTML/preview output is not production Android code.

## Non-Negotiable Runtime Boundary
The UI never fabricates runtime, session, telemetry, process, network, security, storage, Shizuku/Rish, or Termux state. Every status badge and action capability must be derived from an existing Alfa contract/evidence source or be rendered as UNKNOWN/BLOCKED when evidence is absent.

Production terminal content remains the real PTY stream from the selected runtime. Termux, Termux:API, and Shizuku/Rish remain separate execution lanes; the UI coordinates/presents them and does not merge them into the runtime PTY.

## Visual System
Use the final artifact's Terminal Obsidian direction:
- Obsidian/carbon surfaces with low-contrast structural borders.
- Emerald for READY/PASS/ACTIVE/ONLINE.
- Amber for VERIFYING/DEGRADED/WARN.
- Crimson for FAILED/HALT/DENIED/ERR.
- Cyan for telemetry/socket/info.
- Slate for UNKNOWN/IDLE/SUSPEND.
- Geist-style UI hierarchy and JetBrains Mono-style terminal/telemetry hierarchy where locally available; no network font dependency.
- 4dp spacing base and 48dp minimum interactive touch targets.
- No decorative gradients, blur, faux glass, or pill-shaped controls.

## Android Implementation Boundary
The production UI remains native Android Views in the existing application. Do not replace the terminal/session architecture with a webview, HTML prototype, or Compose migration solely to reproduce Stitch output.

Responsive behavior must use actual available window metrics rather than fixed assumptions. Edge-to-edge/insets must remain correct for target SDK 35+. The existing accessibility contract remains mandatory.

## Storage Reconciliation
The design artifact contains example package/storage paths that do not match the canonical Alfa repository/runtime model. They are visual examples only and must not be copied into production code.

Production storage continues to use the existing canonical Alfa storage and HostStorageBridge contracts. Runtime rootfs and application-internal storage remain distinct domains. Host visibility must be established through the existing provider-neutral bridge and evidence, not through UserLAnd/FUSE/private-path claims.

## Feature Mapping
The final design is decomposed into independently verifiable UI capabilities:
1. Core Terminal Obsidian shell and responsive runtime dashboard.
2. Runtime-specific terminal/session presentation for Debian, Ubuntu, Alpine, and Kali.
3. Up to ten real independent sessions per runtime, with session lifecycle evidence.
4. Project Explorer with host-project and guest-runtime filesystem-domain separation.
5. Floating terminal presentation sharing a real existing session; overlay permission is fail-closed.
6. Split-pane/synchronized terminal presentation without merging process/session state.
7. Appearance and typography settings with persisted presentation preferences only.
8. Indonesian/English interaction language with technical terminal output invariant.
9. Security diagnostic console and audit/remediation dialogs driven by real security evidence.
10. Policy backup/restore and directory override flows with explicit validation and confirmation.
11. Shizuku/Rish and Termux/Termux:API audit/status presentation without embedding those external lanes into MainActivity runtime execution.
12. App icon/adaptive icon assets matching the approved visual identity.

## Explicitly Rejected Direct Imports
- Static telemetry values shown in Stitch screenshots.
- Static READY/ACTIVE/ONLINE labels without evidence.
- HTML/Tailwind/Google Fonts CDN as Android production dependencies.
- The artifact's placeholder package names (`moe.alfa.device.ctrl`) or storage paths.
- Proto DataStore/Compose code from the design note unless independently required by the existing Android architecture and introduced with its own tests.
- Security capabilities merely because a mockup depicts them.

## Acceptance
A UI capability is complete only when its native implementation, state/evidence source, failure behavior, accessibility, responsive layout, automated test, and DUT verification agree. Visual similarity alone is not completion.

The final UI artifact is a design reference and acceptance target; it is not runtime evidence.
