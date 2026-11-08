# DevTerm Keyboard: development plan

## Goal
Ship a lightweight offline Android system keyboard for Termux, mobile coding, and SSH sessions.

## Architecture
1. MainActivity: onboarding and Android keyboard settings.
2. DeveloperKeyboardService: renders keys, handles layers and one-shot modifier state.
3. KeyLayouts: declarative ABC, SYM, NAV and FN layouts.
4. KeyEventSender: commitText for ordinary text and sendKeyEvent for special/terminal keys.
5. GitHub Actions: debug APK build workflow.

## Phase 1 — MVP
- [x] Android InputMethodService declaration
- [x] Four layouts and terminal key strip
- [x] Esc, Tab, Ctrl, Alt, Shift, arrows, Home, End, Page Up/Down, F1-F12
- [x] Programming symbols, backspace, enter and Meta
- [x] One-shot modifiers and Ctrl shortcuts
- [x] Offline/privacy design
- [x] GitHub Actions debug APK workflow
- [ ] Verify Android SDK 35 CI compilation
- [ ] Test with Termux on real Android hardware

## Phase 2 — Usability
- [ ] Key repeat on hold
- [ ] Shift double-tap lock, Ctrl lock and state indicators
- [ ] Portrait/landscape layouts, haptics and custom key height
- [ ] Custom themes and rearrangeable shortcuts

## Phase 3 — Advanced
- [ ] Custom macros/snippets
- [ ] Reliable Vim, Nano, tmux and SSH compatibility test matrix
- [ ] Touchpad cursor motion and advanced Meta/Super combinations
- [ ] Accessibility and performance improvements

## Phase 4 — Distribution
- [ ] Automated emulator and unit tests
- [ ] Signed release APK and GitHub Releases
- [ ] Android-version regression tests
- [ ] Privacy and reproducible build documentation

## Acceptance
Install on Android 8+, enable as a system keyboard, type shell commands in Termux, check Escape, Tab, arrows, function keys, and verify Ctrl+C interruption and Ctrl+D EOF. No network permission or logging.
