# Keylix v0.4.0 — Dual-mode keyboard preview

## New
- The app is renamed **Keylix**, with a minimal native keyboard/terminal icon.
- Standard/Dev switching from Settings **and** the keyboard header; user preference persisted.
- Rightmost Enter, full-text Shift key, familiar Gboard-like Space and emoji placement.
- Functional categorized Unicode emoji picker, with six categories and a return-to-letters control.
- Lightweight offline English word suggestions and conservative autocorrect in Standard mode. Both can be switched off.
- Voice via Android's speech-recognition activity, with a fallback recognizer when available, rather than relying on a speech recognizer hidden in the keyboard service.
- Seven appearance themes and accent-color key flashes (no spreading ripple).
- Cleaner settings with setup status and a text field for keyboard testing.
- Expanded source organization, documentation and unit tests.

## Install on Android
In the release **Assets**, tap **Keylix-v0.4.0.apk**, not Source code ZIP. Install it, open Keylix, enable it as a system keyboard and select it. Tap the **Aa Standard** / **>_ Dev** chip in the keyboard header to switch modes.

## Notes
This is a debug-signed prerelease. Installing over a differently signed older version may fail; uninstall/reinstall if needed, and reenable the keyboard. The existing package ID is unchanged.

Keylix itself has no internet permission or typing history. Optional speech recognition uses other installed providers that may process voice online. **Voice and pasted text are inserted without automatically pressing Enter.**

Predictions and autocorrect are intentionally lightweight; this is not a copy of Gboard's proprietary engine. Actual Termux paste and speech behavior, including OEM-specific Android 16 edge-to-edge controls, should be tested on a device.
