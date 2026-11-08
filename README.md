# Termora

**Terminal-ready typing for Android.** A compact native Android keyboard for Termux, SSH, code editors and normal input fields. Formerly called DevTerm Keyboard, Termora keeps the same app ID (so Android recognizes the app when upgrading).

## Install

Get the [latest GitHub Release](https://github.com/janverty123/devterm-keyboard/releases) and tap **Termora-v0.3.0.apk** under **Assets** for a direct installation—no ZIP extraction required. This is a debug-signed prerelease; Android may require uninstalling a differently signed previous debug build. Re-enable the keyboard after reinstalling. Do not install both under the same package ID.

## What's new in v0.3.0

- **Shift on the permanent terminal toolbar** and in the navigation layout; single-tap Shift, double-tap or long-press to lock Caps.
- **Improved Termux clipboard paste:** keyboard sends clipboard text using `InputConnection.commitText()`, rather than relying solely on Android's context-menu paste action. Clipboard insertion never presses Enter automatically.
- **Circle-shaped letter preview bubbles** on touch, animated subtly, with a Settings toggle. No preview on password fields.
- **Voice typing** via Android's available speech service (such as Google Speech Services when installed). The app asks for microphone permission only when you enable voice. A provider may transmit audio online; check its privacy policy. Termora itself has no network permission and never stores recordings.
- **New Termora name and native icon**, redesigned launcher/setup experience with activation status, privacy explanation, accessible toggles, and light/dark themes.
- Esc, Ctrl, Alt, arrows, Copy/Paste, symbol navigation and F1–F12 remain supported.

## Termux paste and Shift

Shift is available in the top developer toolbar. You can tap **Ctrl**, then **Shift**, then **V** to emit Ctrl+Shift+V where supported. Termux documents Ctrl+Alt+V as a *hardware keyboard* shortcut, which isn't guaranteed to work through Android's touch keyboard. The **Paste** key is the recommended method: it now commits clipboard text directly to the focused terminal. If a terminal ignores the clipboard insertion, use Termux's native long-press paste menu. See [Termux keyboard reference](https://github.com/termux/termux-tools/blob/master/doc/termux.1.md.in).

## Permissions and privacy

- `RECORD_AUDIO`: optional, required to start speech recognition; requested at runtime, not needed for normal keyboard use.
- No `INTERNET` permission in the keyboard app, no analytics, no typed-text logging, no persistent clipboard history.
- The separate installed speech provider (such as Google's) **may process audio online**. Voice input does not automatically execute any terminal command.
- Android rightly warns that all keyboards can potentially access entered text. Review source before using with sensitive data.

## Developer setup

- Kotlin Android `InputMethodService`, target SDK 35, minimum SDK 26.
- Build with Java 17 and Gradle 8.11.1 using `gradle assembleDebug` or GitHub Actions.
- Main source: `app/src/main/java/com/oasan/devtermkeyboard/`.
- Four keyboard layers: letters, symbols, navigation, function keys.
- Source project kept under the existing `com.oasan.devtermkeyboard` application ID for upgrade compatibility.

Refer to [device test checklist](docs/TESTING.md) for Android 16/Termux verification, [roadmap](docs/PLAN.md), and [GitHub Releases](https://github.com/janverty123/devterm-keyboard/releases) for packaged APKs.

## Known limitations

Actual key behavior depends on the receiving terminal/editor. Clipboard and voice support vary between apps and speech providers. Tests on real hardware remain important, particularly on Android 16 with navigation overlays. This is a development build, not a Play Store release.
