# Termora — development roadmap

## Milestone 0.2 (implemented in source)
- [x] Keyboard input method with four layouts and Android setup UI
- [x] Adaptive row heights and navigation-bar inset padding for Android 15/16
- [x] Gboard-inspired rounded keys, light/dark themes, ripple and key-press animations
- [x] Permanent scrollable developer toolbar; copy/paste/cut/select all actions
- [x] Long-press Space / 🌐 keyboard picker
- [x] Shift one-shot, double-tap/hold Caps Lock and auto-sentence caps in regular text editors
- [x] Long-press repeat for Backspace and cursor arrows
- [x] Configurable compact size and optional haptics
- [x] Offline with no INTERNET permission
- [ ] Test layout on actual Android 16 device with gesture and 3-button navigation
- [ ] Test Termux, Vim, Nano, tmux, SSH and common code editors

## Upcoming
1. Device-based bug fixes for OEM navigation-bar inset differences, orientation changes and small screens.
2. User-editable key strip with drag/reorder, repeat speed, and keyboard height slider.
3. Clipboard toolbar previews without persistent history, terminal-specific selection handling.
4. Accessibility review, automated UI/emulator checks, stable signing and release APKs.
5. Long-press symbol alternates, swipe cursor movement and improved predictive capitalization if desired.

## Manual acceptance checks
See TESTING.md. A successful CI compile alone does not prove correct behavior in the user's specific terminal emulator.

## v0.3.0 features
- [x] Permanent Shift toolbar / Shift on navigation layer
- [x] Direct clipboard-text paste for Termux-like InputConnections
- [x] Configurable circular key previews
- [x] Optional Android speech recognition with runtime microphone permission
- [x] Original Termora native vector app icon and revamped onboarding
- [ ] Real Android device testing for Google speech services and Termux clipboard
- [ ] A stable release signing key for seamless updates
