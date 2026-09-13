# Final Stitch Terminal UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the legacy Alfa terminal presentation with the native Stitch Terminal Obsidian interaction layer while preserving canonical runtime/session behavior.

**Architecture:** Keep `MainActivity` as the runtime/session backend and install a presentation adapter from `AlfaApplication` after the existing shell is constructed. The adapter extracts the real `TerminalView` and existing runtime/monitor controls, then builds the final responsive shell around them. Terminal renderer changes remain in `terminal-view` and are exercised independently.

**Tech Stack:** Android Java, Android window insets/IME APIs, existing Termux `TerminalView`/`TerminalRenderer`, Gradle/JUnit, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-12-final-stitch-terminal-ui-design.md`

## Global Constraints

- GitHub canonical source is authoritative.
- Stitch assets define UX/appearance, not runtime evidence.
- No fake telemetry or hardcoded runtime identity.
- Minimum interactive target is `48dp`.
- Native Android IME is mandatory; no HTML/fake keyboard.
- One change → one test → verification.
- Fresh APK/DUT verification is required before completion claims.

---

### Task 1: Lock visual and renderer contracts

**Files:**
- Create: `app/src/test/java/com/alfa/device_ctrl/AlfaFinalUiPresentationContractTest.java`
- Create: `terminal-view/src/test/java/com/termux/view/TerminalRendererLineSpacingTest.java`
- Modify: `app/src/test/java/com/alfa/device_ctrl/AlfaUiForensicDesignContractTest.java`

- [x] Define Obsidian palette and appearance range assertions.
- [x] Update the existing telemetry token assertion from legacy cyan to `#38BDF8`.
- [x] Define renderer line-height constructor behavior.

### Task 2: Native terminal renderer appearance

**Files:**
- Modify: `terminal-view/src/main/java/com/termux/view/TerminalRenderer.java`
- Modify: `app/src/main/java/com/alfa/device_ctrl/AlfaTerminalViewClient.java`

**Interfaces:**
- `TerminalRenderer(int textSize, Typeface typeface, float lineSpacingMultiplier)` preserves the existing two-argument constructor and clamps line spacing to `1.0..1.75`.
- `AlfaTerminalViewClient` owns the native terminal focus/IME boundary and can rebuild the renderer for zoom.

- [x] Add the three-argument renderer constructor.
- [x] Make pinch zoom rebuild the real renderer.
- [x] Make tap request focus and native IME.

### Task 3: Final Stitch presentation shell

**Files:**
- Create: `app/src/main/java/com/alfa/device_ctrl/AlfaFinalUiPresentation.java`
- Modify: `app/src/main/java/com/alfa/device_ctrl/AlfaApplication.java`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- `AlfaFinalUiPresentation.apply(Activity)` is idempotent and only operates on `MainActivity`.
- The presentation layer reuses the existing `TerminalView`, runtime dashboard and monitor controls rather than replacing the runtime backend.

- [x] Install the presentation after `MainActivity` creates its legacy shell.
- [x] Build Terminal Obsidian header, runtime selector, terminal card, PTY accessory bar and bottom navigation.
- [x] Wire native IME insets and `adjustResize`.
- [x] Add appearance settings for font size, line height, cursor style, theme presets and fullscreen.
- [x] Keep unsupported split behavior explicitly blocked until independent PTY evidence exists.
- [x] Derive displayed runtime name from `RuntimeSelection` rather than hardcoding a distribution.

### Task 4: Theme contract alignment

**Files:**
- Modify: `app/src/main/java/com/alfa/device_ctrl/AlfaUiTheme.java`

- [x] Align telemetry cyan to `#38BDF8`.
- [x] Preserve semantic status mapping and legacy-palette detection.

### Task 5: CI verification

**Files:**
- Modify: `.github/workflows/ui-contract.yml`

- [x] Run app UI unit contracts.
- [x] Run terminal renderer line-height contract.
- [ ] Confirm all jobs pass on the final head commit.
- [ ] Confirm APK Readiness produces a fresh APK from the final head.

### Task 6: DUT verification

**Evidence required:** fresh APK from the final canonical head.

- [ ] Install fresh APK; verify package/version/digest provenance.
- [ ] Start verified runtime; verify real PTY and shell prompt.
- [ ] Tap terminal; verify focus and native Gboard appearance.
- [ ] Type, backspace, enter, Ctrl-C, paste and accessory controls.
- [ ] Open/close IME; verify accessory bar and terminal reposition against IME inset.
- [ ] Change 11/13/15sp; verify actual renderer dimensions.
- [ ] Change 1.0/1.25/1.5x line height; verify actual row spacing.
- [ ] Change cursor style and terminal palette.
- [ ] Verify fullscreen and adaptive geometry on the real DUT.
- [ ] Verify no terminal clipping at rest or with IME visible.
