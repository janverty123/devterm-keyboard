# Keylix — Dual-mode Android keyboard

**Everyday typing. Developer power.**

Keylix is a native Kotlin system keyboard for Android. It combines a **Standard** phone layout with a **Dev** terminal layout, without root, network access, ad tracking or typing history. Formerly Termora / DevTerm Keyboard. The package ID remains `com.oasan.devtermkeyboard` to preserve Android upgrade compatibility.

## Install

Download **[Keylix v0.4.0 APK](https://github.com/janverty123/devterm-keyboard/releases/tag/v0.4.0)** from the release's **Assets** section, then install the `Keylix-v0.4.0.apk` file on Android. No ZIP extraction is needed. These are GitHub Actions **debug-signed prereleases**. Signature mismatches may require uninstalling an earlier debug APK first (which resets app preferences).

## What's new in v0.4

- **Two modes:** Standard is a four-row messaging keyboard; Dev adds a number row, scrollable terminal controls, Nav and F1–F12 pages. Switch from the key strip or the app's Settings.
- **Correct bottom row:** Enter is the rightmost key, with ?123, emoji, comma, Space and period in familiar positions. Shift uses the text **Shift**.
- **Voice:** Dedicated microphone at the top right, outside the developer shortcut strip. Opens Android's available speech-recognition UI (Google if installed), with an on-device/standard recognizer fallback when available.
- **English suggestions:** On-device prefix suggestions plus conservative spelling corrections using a modest built-in word list. Standard mode only. Disable independently under Text correction. Not equivalent to the full proprietary Gboard prediction model.
- **Emoji picker:** Six categorized panels of Unicode emoji rendered with Android's color emoji font. System fonts support flags, skin tones and combined emoji better than one SVG per emoji.
- **Solid key flash:** Removes spreading ripples in favor of a quick solid-color flash on press/release.
- **Themes:** System, Cloud, Charcoal, Midnight, Mint, Slate and Rose. Compact height, haptics and character-preview toggles.
- **New brand:** Clean keyboard/terminal logo and a simplified light settings screen.

## Privacy and safety

- No `INTERNET` permission, keylogging, clipboard history, or background uploading.
- Optional `RECORD_AUDIO` permission is used *only* if the fallback speech recognizer needs it.
- External Google or other speech-recognition services may transmit voice recordings over the network according to the provider's policies.
- Predictive suggestions are **off** in Dev mode, password fields, URLs, email inputs and editors marking input as `TYPE_NULL`.
- The keyboard **does not press Enter after voice dictation or clipboard paste**, preventing unintended shell command execution.

## Build

Open in Android Studio (JDK 17, Android SDK 35), or run `gradle testDebugUnitTest assembleDebug`. Successful pushes to `main` trigger GitHub Actions and a direct APK GitHub prerelease if the version is new. Version metadata lives in `app/build.gradle.kts`.

## Developer layout
The top strip has Esc, Tab, Ctrl, Alt, Shift, Paste, Copy, Nav, Fn, arrow keys and Meta. The bottom row always ends with Enter. Long-press Space to change Android keyboards; double tap/hold Shift for Caps Lock.

## Known limitations and future work
The basic offline correction engine is not trained on large language models, has a limited English vocabulary, and does not yet offer multilingual predictions, advanced grammar correction, gesture typing, personalized phrase learning, clipboard history, or automatic emoji search. Feature correctness varies by target app, speech provider, OEM keyboard safe areas, and Android version; **device testing is still required**.

See [testing](docs/TESTING.md) and [roadmap](docs/PLAN.md).
