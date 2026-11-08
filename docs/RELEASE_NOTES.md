# Termora v0.3.0 — Developer preview

DevTerm Keyboard has been renamed **Termora**. The Android package ID stays the same so it remains one installed keyboard.

## Highlights
- Permanent toolbar **Shift**, additional Shift on Nav, and Ctrl+Shift+V combinations where supported.
- Reliable text-oriented **Paste** action for Termux via the InputConnection, without executing shell commands automatically.
- Gboard-style animated round **letter previews** (configurable, hidden for password fields).
- Optional **microphone/voice typing** using the device's installed Android speech recognition service.
- New **Termora launcher icon**, clean onboarding, activation state display, theme and haptics controls.

## Installation
1. Under **Assets**, tap `Termora-v0.3.0.apk` directly (not the source ZIP).
2. Open the APK on Android and approve installation from your browser/file manager if needed.
3. Open **Termora**, enable it in Android keyboard settings, and choose it as your active keyboard.
4. In Termora settings optionally enable the microphone permission for voice typing.

## Important notices
- This is a **debug-signed prerelease**, not a production-signed app. Uninstalling a previous differently signed debug build may be needed; doing so clears its local appearance settings.
- The keyboard itself has no Internet permission and does not save dictated audio or keystrokes. **Android speech recognition services may send audio to their providers.** Mic access is optional.
- Termux and other apps may handle clipboard/key combinations differently. The Paste key inserts clipboard text without sending Enter; test before pasting untrusted shell commands.
- No guarantee of Gboard-specific proprietary functionality; this is a standalone native Android keyboard.
