# Alfa Device Ctrl — Stitch v1.0.0 Forensic Audit

## Scope

This document records the forensic comparison between the uploaded Stitch v1.0.0 design corpus and the canonical Alfa Device Ctrl repository before implementation. The Stitch ZIP is treated as a **design/reference corpus**, not as a source of truth for runtime, package identity, storage paths, security claims, or network behavior.

## Evidence snapshot

- Stitch corpus: `stitch_alfa_device_control_v1.0.0.zip`
- HTML screens: 159
- PNG screenshots: 160
- Design/spec markdown files: 7
- Canonical repository: `panglimanetizen01/Alfa_device_ctrl`
- Canonical default branch at audit time: `master`
- `master` HEAD at audit time: `492c9adddf8f7da192826a14a767c6fc594e763c`
- Current UI implementation branch used as implementation base: `feat/final-stitch-ui`
- UI branch HEAD at audit time: `74323b39f042a568842b59042f9bdee87ed41638`

## Corpus coverage

The 159 screens cluster into these visual/state families:

- terminal: 78
- session: 34
- floating terminal: 28
- dialogs: 26
- settings/appearance/language: 23
- split terminal: 14
- security: 8
- project explorer: 8
- runtime: 6
- storage/file browser: 5
- network/security diagnostic: 4
- language: 4
- app icon: 2
- source editor: 1

Many screens are state variants of the same underlying feature. They must not be implemented as independent fake features; they should map to shared native components/state models backed by the existing runtime implementation.

## Critical provenance conflicts

### 1. Package identity conflict

The Stitch integration documentation uses `moe.alfa.device.ctrl` while the canonical Android application uses `com.alfa.device_ctrl`.

**Decision:** canonical repository package identity wins. Do not introduce `moe.alfa.device.ctrl` merely to reproduce the reference documentation.

### 2. Host-storage conflict

The Stitch runtime architecture document describes an Android/data workspace path. The canonical Alfa project has a separately locked host-storage contract and host-visible project root. The design reference therefore cannot redefine storage provenance.

**Decision:** preserve the canonical host-storage contract. UI labels must describe actual storage domains returned by the application and must never imply that an Android-private path is host-visible.

### 3. Runtime/security claims in design corpus

Several Stitch screens are simulations of blocked operations, seccomp/mount policy, Shizuku/Rish, tunnel/webhook activity, and security diagnostics. A screenshot is not proof that the corresponding capability exists.

**Decision:** implement the visual state vocabulary and evidence presentation, but bind every runtime/security/network value to real repository/runtime evidence. Unsupported capabilities must render `UNKNOWN`, `UNAVAILABLE`, or `BLOCKED` with a truthful reason.

### 4. Multiple design-token variants

The corpus contains multiple Obsidian token documents with slightly different canvas/surface values. The latest workstation/industrial variants converge on the `#101419` / `#161B22` / `#0D1117` family, while an earlier contract currently uses `#0B0F14` / `#121820` / `#1A222D`.

**Decision:** do not silently mix token families. The final implementation must select one canonical token set and update tests/documentation to that set. The supplied latest visual direction is the `#101419` canvas, `#161B22` surface, `#0D1117` terminal, `#22272E` structural border, `#10B981` primary, `#38BDF8` telemetry, `#F59E0B` warning, `#EF4444` critical family.

## Current implementation gap

The current `feat/final-stitch-ui` implementation already provides a native terminal-first presentation layer, an accessory bar, terminal font/line-height controls, basic appearance settings, a network evidence panel, and runtime evidence navigation. However, forensic inspection shows it is a **partial projection**, not a full implementation of the Stitch v1 corpus.

Examples:

- split terminal is currently visually blocked because independent second-PTY evidence is not established;
- floating terminal system-overlay states are not fully implemented;
- settings expose only a subset of the reference controls;
- copy/paste has direct clipboard paste support but lacks the full reference selection/context-feedback state system;
- session manager screens represented by the corpus are not all projected into the presentation layer;
- project explorer, storage browser, language settings, audit-log views, and their state/dialog variants are not all represented by the final presentation shell;
- network evidence is read-only and intentionally limited to APIs/files currently available to the app; firewall/NAT inspection is not claimed;
- the application is targetSdk 36, therefore edge-to-edge and IME inset behavior must be validated against current Android platform rules rather than relying on screenshots.

## Network forensic boundary

The network UI must distinguish **observation** from **control**.

Allowed evidence sources include:

- `ConnectivityManager`
- `NetworkCapabilities`
- `LinkProperties`
- `NetworkInterface`
- `/proc/net/route`
- `/proc/net/ipv6_route`
- other read-only kernel socket/proc evidence where accessible and bounded

`NetworkCapabilities.NET_CAPABILITY_INTERNET` means the network is configured for internet access; `NET_CAPABILITY_VALIDATED` is the stronger Android observation for actual validated public-internet reachability. `LinkProperties` provides interface, routes, DNS, addresses, and proxy information.

The UI must not claim that a firewall, NAT table, packet capture, VPN tunnel, proxy enforcement, or network namespace isolation is active unless the application has direct evidence for that exact claim.

## Android IME/edge-to-edge boundary

The reference contains keyboard-active terminal states. The implementation must treat these as actual Android window/inset behavior, not screenshots pasted over the UI.

Current Android guidance requires:

- `adjustResize` for activities that need IME insets;
- correct `WindowInsets` handling for system bars, display cutout, gestures, and IME;
- no double-consumption/double-padding of insets;
- preservation of terminal focus while the IME is shown.

The implementation must preserve the existing runtime/PTY lifecycle while adapting only the presentation geometry.

## Implementation rule

For every Stitch screen/state:

1. Identify whether it is a unique component or a state variant.
2. Locate the existing canonical Alfa runtime/data source.
3. Reuse real state and evidence.
4. Implement the smallest native UI change required.
5. Add a regression/contract test where practical.
6. Build and run the relevant GitHub Actions gates.
7. Do not mark a capability PASS merely because the visual screen exists.

## Acceptance boundary

A feature is only `PASS / GREEN / LOCKED` when:

- the corresponding source implementation exists;
- the implementation is bound to real state/evidence;
- relevant tests pass;
- canonical build gates pass;
- APK readiness passes for the resulting commit;
- runtime verification is completed on the DUT where the feature requires actual Android execution.

A visual-only match is `UI PASS`, not functional/runtime PASS.
