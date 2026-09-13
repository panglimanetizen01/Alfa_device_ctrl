# Final Stitch Terminal UI Design

## Goal
Replace the legacy Alfa presentation with a native Terminal Obsidian interaction layer while preserving the proven runtime/session backend.

## Source of truth
- GitHub canonical runtime/source remains authoritative for behavior and provenance.
- `UI_alfa_device_control_final.zip` is the UX/appearance/interaction reference.
- No Stitch telemetry is treated as runtime evidence.

## Runtime interaction contract
1. Terminal tap requests focus and explicitly requests the native Android IME.
2. `TerminalView.onCreateInputConnection()` remains the real PTY input boundary.
3. IME visibility is handled through Android window insets; the accessory bar is repositioned above the IME.
4. The PTY accessory bar sends real terminal control sequences/input events.
5. Font size changes rebuild the real terminal renderer.
6. Line-height changes rebuild the real terminal renderer with an explicit multiplier.
7. Pinch zoom changes the real renderer font size rather than scaling a fake canvas.
8. Cursor styles use real terminal control sequences.
9. Terminal palette updates the canonical `TerminalColors.COLOR_SCHEME` and resets the active emulator palette.
10. Terminal geometry is adaptive; no fixed-height terminal pane is retained in the final presentation.

## Visual contract
- Canvas: `#0B0F14`
- Surface: `#121820`
- Active surface: `#1A222D`
- Border: `#1F2937`
- Focused border: `#374151`
- Ready: `#10B981`
- Cyan/telemetry: `#38BDF8`
- Warning: `#F59E0B`
- Error: `#EF4444`
- Terminal canvas: `#0D1117`
- Terminal foreground: `#F0F6FC`
- Minimum interactive target: `48dp`

## Evidence rules
- Runtime labels are derived from the selected `RuntimeProfile`; no hardcoded runtime identity is used as evidence.
- Network/diagnostic panels must expose only values obtainable from the actual Android/Linux runtime. Unsupported privileged data is explicitly marked unavailable rather than fabricated.
- Split/multi-session UI is not allowed to claim a second PTY until independent session evidence exists.

## Verification gates
- Unit contract for visual tokens and appearance ranges.
- Terminal renderer line-height unit contract.
- APK build/provenance CI.
- Fresh-install DUT verification of focus, Gboard appearance, typing, paste, accessory bar, IME inset repositioning, font scaling, line-height, cursor, palette, fullscreen and terminal geometry.
