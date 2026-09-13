# Stitch v1 Native Implementation Forensic

## Baseline
- Canonical UI base: `fix/stitch-interaction-forensic` @ `b46edc58e3df070c44c4d3f072b51109982232b8`.
- Implementation branch: `work/stitch-native-implementation`.
- Reference corpus: `stitch_alfa_device_control_v1.0.0.zip` (159 `code.html` screens plus screenshots and Terminal Obsidian design tokens).

## Evidence-derived implementation boundary
The ZIP is treated as an interaction/state/design specification, not as executable source. Native Android Views remain the implementation substrate. Runtime/session/network/security truth remains owned by the existing Alfa runtime engine and evidence contracts.

### Implement directly
1. Terminal-first workstation shell: ALFA::CTRL header, PTY/session metadata, terminal viewport, accessory keys, bottom navigation.
2. Runtime manager: runtime registry cards, evidence-backed READY/VERIFYING/FAILED state, INSTALL/OPEN actions mapped to existing runtime engine.
3. PTY accessory behavior: ESC, TAB, CTRL latch, ALT, SIGINT, pipe/slash/minus, arrows, COPY and PASTE.
4. Appearance settings: persisted font size, line height, cursor style and Obsidian/Amber/High Contrast terminal presets.
5. Runtime lanes: TERMUX, TERMUX API, SHIZUKU/RISH and Alfa runtime are represented only when canonical evidence exists; no synthetic readiness values.
6. Network/diagnostic views use existing `AlfaNetworkPanel` and runtime evidence instead of reproducing static telemetry from the reference.
7. IME/window-inset behavior uses native `WindowInsets` and `adjustResize`; critical controls retain the 48dp touch floor.

### Capability-gated states
- Multiple PTY tabs / split pane: do not fabricate independent sessions. The current canonical `RuntimeSessionManager` owns one verified session. UI must expose only the capability that the runtime engine can prove.
- Floating/bubble terminal: do not claim a system overlay until actual overlay capability/permission is present.
- Firewall/NAT/VPN/packet capture/raw-socket claims: do not infer them from UI labels. Network UI may report only Android/Linux evidence actually collected.

## Design tokens
Dominant Terminal Obsidian tokens are preserved: canvas `#101419`, surface `#161B22`, active surface `#1C2229`, border `#22272E`, focused border `#374151`, text `#F0F6FC`, muted `#8B949E`, ready `#10B981`, verifying `#F59E0B`, error `#EF4444`, telemetry `#38BDF8`, unknown `#64748B`, and 48dp minimum touch target.

## Network/OS boundary
The UI is presentation only. Android `ConnectivityManager`, `NetworkCapabilities`, and `LinkProperties` remain the authoritative app-level network state boundary; Linux `/proc/net` and socket/route evidence is displayed only when produced by the selected verified runtime. No UI state may imply packet capture, firewall policy, NAT table, VPN encryption, proxy enforcement, namespace isolation, or raw-socket privilege without corresponding evidence.

## Verification gates
1. Unit contracts and renderer contracts GREEN.
2. Build/packaging/Gate 7 contracts GREEN.
3. PR CI GREEN against `master`.
4. Local canonical repository synchronized to the implementation commit.
5. APK built from that exact commit and installed to DUT.
6. DUT visual + interaction verification proves the native shell, terminal input, runtime actions, IME behavior and evidence-backed panels.
7. Only then declare `PASS GREEN LOCKED`.
