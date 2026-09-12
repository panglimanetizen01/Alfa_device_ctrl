# Alfa Device Ctrl — UI Forensic Design Lock

## Source precedence

1. GitHub `master` / application source is the behavioral and runtime Source of Truth.
2. `UI_alfa_device_control_final.zip` is the approved visual/design corpus only.
3. Runtime evidence remains the only source for live status, telemetry, security, network, process, and capability claims.

## Design corpus provenance

- Archive: `UI_alfa_device_control_final.zip`
- SHA-256: `281a987c91915d3652b437602ba00e9dd8fd9577ae5536e900da80fee8f9086d`
- 363 entries: 118 `code.html`, 119 `screen.png`, and design/specification Markdown.
- Primary visual language: Terminal Obsidian / Obsidian PTY.

## Locked visual system

- Canvas: `#0B0F14`
- Surface: `#121820`
- Active surface: `#1A222D`
- Structural border: `#1F2937`
- Text: `#E0E2EA`
- Muted text: `#BBCABF`
- Ready/active/online: `#10B981`
- Verifying/warn/partial: `#F59E0B`
- Failed/halt/denied: `#EF4444`
- Telemetry/socket/info: `#06B6D4`
- Unknown: `#64748B`
- Minimum interactive target: 48dp
- No decorative gradients, glassmorphism, or elevation shadows.
- Native Android presentation; no runtime dependency on prototype CDN assets or web fonts.

## Semantic safety boundary

Prototype screenshots contain representative telemetry and security labels. Those values are not runtime evidence and must not be copied into production as facts. UI components may display a state only when the application already has a corresponding canonical evidence source; otherwise the UI must show an explicit unknown/waiting/blocked state.

## Android integration boundary

The current application is a native Android Views implementation. The final cosmetic system therefore adapts the existing View hierarchy rather than replacing the runtime/session architecture with the prototype's HTML, package names, Compose/DataStore assumptions, or web dependencies.

The current canonical runtime registry contains Debian, Ubuntu, Alpine, and Kali. UI visibility must remain compatible with all registered runtimes and must not collapse cards below the 48dp touch floor.

## Verification contract

The UI milestone is not green from source review alone. Required evidence:

- design-token unit contract passes;
- existing runtime/session contracts remain green;
- Android instrumentation/UI checks pass where environment permits;
- canonical APK readiness/build remains green;
- fresh APK visual/runtime verification is performed on the DUT before declaring the final UI locked.
