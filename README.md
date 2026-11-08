# DevTerm Keyboard

A lightweight **Android developer keyboard** built with Kotlin and `InputMethodService`. Designed for Termux, SSH terminals, editors, and regular Android text fields, without needing a laptop or root access.

## v0.2 — Adaptive keyboard

- Compact Gboard-inspired rounded keys, ripple effects and subtle press animations.
- Resizes keyboard key height for short portrait and landscape screens.
- Reserves a navigation-bar safe area on Android 15/16 (including an OEM fallback), so the Android keyboard switcher does not overlap the final row.
- Letter, symbol, navigation, and F1–F12 layouts.
- A horizontally scrollable top developer toolbar with Esc, Tab, Ctrl, Alt, **Copy**, **Paste**, arrows, Fn and Meta.
- **Hold Space** (or tap 🌐) to open Android's **Change keyboard** picker; no extra permissions required.
- **Tap Shift** to capitalize the next letter, **double-tap or hold Shift** for Caps Lock, tap Shift to unlock. Android text fields requesting sentence capitalization are respected.
- Long-press Backspace and arrow keys to repeat actions.
- Copy, Cut, Paste and Select All through the target application's InputConnection. Clipboard fallback is used for Paste and copying selected text when supported.
- Optional light, dark or follow-system theme, compact keyboard height and haptics — configured from the DevTerm app.
- No `INTERNET` permission, telemetry, clipboard history, or logging.

## Get the APK

**Direct Android installation:** open the [current v0.2.0 developer preview](https://github.com/janverty123/devterm-keyboard/releases/tag/v0.2.0) or [all GitHub Releases](https://github.com/janverty123/devterm-keyboard/releases), expand **Assets**, and tap the `.apk` file. There is no need to download or extract the GitHub Actions ZIP.

Release assets are automatically published for newly versioned, successful `main`-branch APK builds. The release workflow reads `versionName` from `app/build.gradle.kts`, skips an already published version, and attaches a direct-install APK plus its SHA-256 checksum.

**Signing:** these are prerelease debug APKs. The v0.2.0 release reuses the verified v0.2.0 CI build to avoid a signature change from that build. Future debug builds may have different signatures until a permanent signing key is set up. If Android reports an app signature mismatch, uninstall the previously installed debug version, then install the new version and enable the keyboard again.

## Enable

1. Open DevTerm after installing.
2. Tap **Enable DevTerm Keyboard** and turn it on in Android's settings.
3. Tap **Choose active keyboard** and select DevTerm.
4. Open Termux or your editor, and type. Use `?123`, `Nav` and `Fn` for extra keys.

## Build from source

Android Studio with JDK 17, SDK Platform 35, Gradle 8.11.1 (or CI's GitHub-hosted runner) is supported:

```sh
gradle assembleDebug
```

See [docs/PLAN.md](docs/PLAN.md) for the roadmap and [docs/TESTING.md](docs/TESTING.md) for on-device checks.

## Compatibility and caveats

Some apps, including Termux and SSH clients, decide which `KeyEvent` combinations to process. Standard `InputConnection` clipboard actions may not be supported by a terminal session even if they work in editable text fields. Hold Shift to lock uppercase when typing; Fn keys are sent as key events, not special text.

**The GitHub CI build is not a substitute for physical device testing.** Please report the device, Android version, layout mode and target app when a key is not recognized.

Android warns when enabling any third-party keyboard because such software can see typed input. Only install source you trust. The app intentionally does not access the network or keep a history of entered text.

## License

No license has been selected yet. The repository owner should choose one before broad redistribution.
