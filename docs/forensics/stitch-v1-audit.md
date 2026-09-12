# Alfa Device Ctrl — Stitch v1.0.0 Forensic Audit

## Scope

This document records the forensic reconciliation between the supplied Stitch v1.0.0 reference corpus and the canonical Alfa Device Ctrl repository. Stitch is treated as **design/reference evidence**, not as runtime source of truth. Reference HTML/package/path/security claims are never imported blindly.

## Evidence snapshot

- Stitch corpus: `stitch_alfa_device_control_v1.0.0.zip`
- ZIP SHA-256: `7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d`
- HTML screens: 159
- PNG screenshots: 160
- Design/spec markdown files: 7
- Canonical repository: `panglimanetizen01/Alfa_device_ctrl`
- Canonical base branch: `master`
- Canonical base SHA at implementation start: `492c9adddf8f7da192826a14a767c6fc594e763c`
- Implementation branch: `feat/stitch-v1-forensic-implementation`
- PR: #73
- Current implementation HEAD: see PR #73; CI/DUT are the acceptance authority

## Reference-to-canonical coverage lock

`StitchV1ReferenceCatalog` contains all 159 unique Stitch `code.html` screen IDs. The catalog is generated from the supplied ZIP and records the ZIP hash, canonical package identity, and canonical host-storage root. `StitchV1ReferenceCatalogTest` locks the count at 159 and verifies that every catalog entry is addressable.

This is deliberate: the reference states are not dropped merely because they do not correspond one-to-one with existing Android activities. State variants are mapped to shared native components and real data sources.

## Corpus families

The 159 screens are state variants across these major families:

- terminal / workstation / command execution
- runtime dashboard and runtime-specific terminals
- session manager and session lifecycle
- split-pane terminal and synchronized input
- floating terminal, bubble, opacity, geometry, and IME states
- appearance, typography, font picker, contrast profiles, and persistence
- project explorer, VFS views, directory/file creation, deletion, and editor
- mount isolation, directory overrides, masking, and policy backup/restore
- security diagnostics, Shizuku/Rish, seccomp, blocked escalation, and audit reports
- network/socket/tunnel/webhook diagnostics
- language and interaction settings
- audit vault/file browser/share/Termux ingestion
- application icon presentation

## Required disposition rule

Every reference discrepancy receives an explicit disposition. No discrepancy is silently ignored.

### Package identity — ADAPT & IMPLEMENT

Reference documentation uses `moe.alfa.device.ctrl`; canonical Android package is `com.alfa.device_ctrl`.

Disposition: retain canonical package identity and adapt all reference labels/state models to it. Do not introduce a second package namespace solely for visual parity.

### Storage paths — REPLACE REFERENCE PATH WITH CANONICAL STORAGE BOUNDARY

Reference documents contain Android-private paths such as `/data/data/moe.alfa.device.ctrl/...` and `/storage/emulated/0/Android/data/moe.alfa.device.ctrl/files/workspace/`. The canonical project has an explicit host-storage contract rooted at `/sdcard/Alfa_device_ctrl_HOST` and a host-visible project root beneath it.

Disposition: UI may show the reference concept of a workspace, but storage operations resolve through the canonical storage bridge and actual returned URI/path evidence. Android-private storage is never presented as host-visible proof.

### Runtime architecture — ADAPT & IMPLEMENT

Reference architecture describes runtime registry, selection, verification, isolated session, PTY allocation, interactive contract, and terminal frame. The canonical repository already contains RuntimeRegistry, RuntimeProfile, InteractiveSessionContract, RuntimeSessionManager, Gate 6/7 launch attestation, PRoot execution, and foreground-service keep-alive.

Disposition: reuse those canonical boundaries. UI state follows actual runtime evidence and prompt readiness; reference runtime names are presentation data only.

### Network namespace / isolation — BLOCKED WITH PROVEN ROOT CAUSE

Linux network namespaces isolate network devices, IPv4/IPv6 stacks, routing tables, firewall rules, `/proc/net`, ports, and related network state. A PRoot compatibility layer does not by itself establish a Linux network namespace boundary.

Disposition: do not claim network isolation for the guest until a real namespace boundary is created and proven on the target kernel. Current UI must report the boundary honestly rather than converting a reference screenshot into a false security claim.

### Firewall / NAT / packet capture — BLOCKED WITH PROVEN CAPABILITY BOUNDARY

The current application has read-only network evidence through Android connectivity APIs, Java network interfaces, and bounded `/proc` snapshots. There is no verified application-level firewall/NAT control or packet-capture pipeline in the canonical runtime.

Disposition: expose observed state only. Firewall rules, NAT tables, packet capture, and packet-level inspection remain `UNAVAILABLE`/`UNKNOWN` until direct evidence exists.

### VPN / encrypted tunnel — BLOCKED UNTIL REAL IMPLEMENTATION

Android `VpnService` requires an actual VPN service, system preparation/user authorization, a TUN interface, routes, and tunnel processing. A label or screenshot is not evidence of a VPN. TLS provides a secure channel only when an actual client/server protocol performs the handshake and protects traffic.

Disposition: reference tunnel/VPN states are mapped to capability status and evidence; no fake VPN/encryption status is emitted.

### IME / keyboard-active terminal — ADAPT & IMPLEMENT

The reference contains active IME, accessory-key, repositioned viewport, and keyboard-dismissed states. Android/Termux evidence establishes that TerminalView is an actual text editor/input connection, terminal focus is required, and `adjustResize`/WindowInsets must be handled as live window behavior.

Disposition: preserve the real TerminalView and session while implementing keyboard/accessory/viewport behavior natively. No screenshot overlay is used.

### Split pane — ADAPT & IMPLEMENT, EXECUTION GATED

Reference screens include dual PTYs, vertical/horizontal split, focus, ratio, and synchronized broadcast. The canonical session manager currently owns one interactive session instance.

Disposition: implement the shared split-pane state machine and UI vocabulary, but gate actual dual execution/broadcast behind independently proven second-session support. Never synthesize a second PTY.

### Floating terminal — ADAPT & IMPLEMENT, SYSTEM CAPABILITY GATED

Reference screens imply a system-level floating window/bubble. A true Android system overlay requires the appropriate platform capability/permission and lifecycle handling.

Disposition: implement in-app floating/window states first; only enable true system overlay behavior when the canonical manifest and DUT permission/runtime evidence prove it.

### Settings / typography / persistence — ADAPT & IMPLEMENT

Reference contains text scaling, PTY font size, line height, font family/weight, cursor style, scrollback, themes, contrast profiles, and persistence. The DataStore document is a reference specification, not proof that the canonical project already uses Proto DataStore.

Disposition: implement these as canonical persisted configuration using the repository's actual settings architecture. Do not add a library merely to reproduce a reference document if the existing architecture provides an equivalent persisted contract.

### Project explorer / file editor — ADAPT & IMPLEMENT

Reference screens cover workspace browsing, VFS comparison, file/folder creation, deletion confirmations, and source editing.

Disposition: bind to canonical storage/runtime APIs and enforce bounded file operations and authorization. Destructive operations require explicit confirmation and evidence; reference sample paths are not treated as real files.

### Security diagnostics / Shizuku / Rish / seccomp — ADAPT & IMPLEMENT WITH EVIDENCE GATES

Reference screenshots contain successful and blocked security operations. They are not proof of capability.

Disposition: expose actual permission declarations, runtime evidence, Shizuku/Rish results, seccomp evidence, and blocked reasons. A successful-looking reference state cannot be emitted without the corresponding runtime evidence.

### Matrix webhook / Termux ingestion — ADAPT & IMPLEMENT, NETWORK GATED

Reference screens describe dispatch, retry, success, and ingestion states.

Disposition: implement the state model and diagnostics only when a real endpoint/client contract exists. Network reachability, TLS, HTTP response, and signature verification are evidence fields; success is never inferred from a button click.

### Language — ADAPT & IMPLEMENT

Reference contains Indonesian/English interaction settings and confirmation states.

Disposition: map to canonical resource/configuration state. Package/path claims from the reference are ignored.

### Design-token variants — RECONCILE TO ONE CANONICAL SET

The ZIP contains several token families. The selected canonical industrial Obsidian family is:

- canvas `#101419`
- surface `#161B22`
- terminal `#0D1117`
- border `#22272E`
- primary `#10B981`
- telemetry/focus `#38BDF8`
- warning `#F59E0B`
- critical `#EF4444`
- text-high `#F0F6FC`
- text-medium `#8B949E`
- text-dim `#484F58`

Disposition: normalize native presentation around this set. Conflicting reference tokens are retained as forensic input, not mixed silently.

### Fonts — ADAPT WITH FALLBACK

The reference names Geist and JetBrains Mono/Fira Code. The supplied ZIP does not provide font binaries sufficient to prove bundled-font availability.

Disposition: use canonical/system monospace fallback where a bundled font is unavailable; do not claim exact font identity without runtime/font evidence.

## Network forensic model

The implementation follows a layered evidence model:

1. **Link / interface:** `NetworkInterface`, interface name, addresses, MTU.
2. **Network:** Android `Network`, `NetworkCapabilities`, `LinkProperties`.
3. **Routing:** `LinkProperties.getRoutes()`, `/proc/net/route`, `/proc/net/ipv6_route`.
4. **Transport:** bounded `/proc/net/tcp*` and `/proc/net/udp*` socket snapshots where accessible.
5. **DNS/proxy:** `LinkProperties` DNS and HTTP proxy fields.
6. **Security/control:** firewall/NAT/VPN/packet capture only when direct evidence exists.
7. **Application/distributed layer:** webhook/API/client-server state must be tied to real request/response evidence.

The implementation therefore distinguishes `CONFIGURED`, `VALIDATED`, `OBSERVED`, `UNAVAILABLE`, `UNKNOWN`, and `BLOCKED` rather than collapsing them into a generic green status.

## Primary research basis

The technical model was cross-checked against primary sources:

- Android Developers — network state, `ConnectivityManager`, `NetworkCapabilities`, `LinkProperties`.
- Android Developers — `VpnService` and VPN routing/TUN requirements.
- Android Developers — Network Security Configuration and cleartext policy.
- Android Developers — 16 KB page-size compatibility and NDK/AGP requirements.
- AOSP — IME/windowing behavior and Android compatibility requirements.
- Termux upstream — `TerminalView`, `TermuxTerminalViewClient`, `KeyboardUtils`, terminal extra keys and IME input connection behavior.
- Linux man-pages — `network_namespaces(7)`, `ip-netns(8)`, `ip(8)`, `socket(7)`, `sock_diag(7)`, `proc_pid_net(5)`, `iptables(8)`.
- IETF RFCs — IPv4 (RFC 791), IPv6 (RFC 8200), IPv6 addressing (RFC 4291), IPv6 Neighbor Discovery (RFC 4861), CIDR (RFC 4632), DHCP (RFC 2131), DNS concepts (RFC 1034), TCP (RFC 9293), TLS 1.3 (RFC 8446), IPsec architecture (RFC 4301).

## Acceptance boundary

A feature is `PASS` only when source implementation, tests, CI, and required DUT execution all prove the claimed behavior. A visual match is only `UI PASS`.

`GREEN` requires all mandatory CI gates to pass for the exact source commit and a fresh APK derived from that commit.

`LOCKED` requires fresh APK installation and end-to-end DUT evidence for the runtime paths in scope, with no stale APK, stale provenance, stale runtime state, or unverified security/network claims.
