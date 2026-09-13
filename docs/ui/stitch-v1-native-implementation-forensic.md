# Stitch v1 Native Implementation Forensic

## Baseline
- Canonical UI base: `fix/stitch-interaction-forensic` @ `b46edc58e3df070c44c4d3f072b51109982232b8`.
- Implementation branch: `work/stitch-native-implementation`.
- Supplied implementation corpus: `stitch_alfa_device_control_v1.0.0.zip` (159 `code.html` states plus 160 screenshots and 7 design/spec markdown files).
- Artifact SHA-256: `7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d`.

## Evidence-derived implementation boundary
The ZIP is an implementation input: its UI states, interaction flows, design tokens, runtime architecture specification, storage model, and security/network vocabulary are reconciled into Alfa's canonical code. Its HTML is not executed as production UI, and its sample runtime/security claims are not treated as evidence. Native Android Views remain the implementation substrate while RuntimeRegistry, RuntimeSessionManager, storage bridges, and evidence contracts remain authoritative for behavior.

### Implement directly
1. Terminal-first workstation shell: ALFA::CTRL header, PTY/session metadata, terminal viewport, accessory keys, bottom navigation and responsive operator surfaces.
2. Runtime manager: runtime registry cards, evidence-backed READY/VERIFYING/FAILED state, INSTALL/OPEN actions mapped to existing runtime engine.
3. PTY accessory behavior: ESC, TAB, CTRL latch, ALT, SIGINT, pipe/slash/minus, arrows, COPY and PASTE.
4. Appearance settings: persisted font size, line height, cursor style and canonical Obsidian/Amber/High Contrast terminal presets, with font-family fallback when reference font binaries are absent.
5. Runtime lanes: TERMUX, TERMUX API, SHIZUKU/RISH and Alfa runtime are represented only when canonical evidence exists; no synthetic readiness values.
6. Network/diagnostic views use existing `AlfaNetworkPanel` and runtime evidence instead of reproducing static telemetry from screenshots.
7. IME/window-inset behavior uses native `WindowInsets` and `adjustResize`; critical controls retain the 48dp touch floor.
8. All 159 catalog states have a deterministic `StitchV1StateModel` mapping and are exposed through the native STITCH state catalog for forensic/state-level traversal.

### Capability-gated states
- Multiple PTY tabs / split pane: do not fabricate independent sessions. The current canonical `RuntimeSessionManager` owns one verified session. UI must expose only the capability that the runtime engine can prove.
- Floating/bubble terminal: do not claim a system overlay until actual overlay capability/permission is present.
- Firewall/NAT/VPN/packet capture/raw-socket claims: do not infer them from UI labels. Network UI may report only Android/Linux evidence actually collected.
- Reference `simulation` states are non-executable state representations and cannot claim live runtime execution.

## Design tokens
Final Industrial Obsidian evidence: canvas `#101419`, surface `#161B22`, surface highlight `#21262D`, terminal `#0D1117`, structural border `#22272E`, focused border `#374151`, text-high `#F0F6FC`, text-medium `#8B949E`, text-dim `#484F58`, primary `#10B981`, secondary `#38BDF8`, warning `#F59E0B`, critical `#EF4444`, unknown `#64748B`, 4px default radius, 4px spacing base, and 48dp minimum interactive target.

## Network/OS boundary
The UI is presentation only. Android `ConnectivityManager`, `NetworkCapabilities`, and `LinkProperties` remain the authoritative app-level network state boundary; Linux `/proc/net` and socket/route evidence is displayed only when produced by the selected verified runtime. No UI state may imply packet capture, firewall policy, NAT table, VPN encryption, proxy enforcement, namespace isolation, or raw-socket privilege without corresponding evidence.

## Verification gates
1. Unit contracts and renderer/state contracts GREEN.
2. Build/packaging/Gate 7 contracts GREEN.
3. PR CI GREEN against `master`.
4. Local canonical repository synchronized to the implementation commit.
5. APK built from that exact commit and installed to DUT.
6. DUT visual + interaction verification proves the native shell, terminal input, runtime actions, IME behavior and evidence-backed panels.
7. Only then declare `PASS GREEN LOCKED`.
