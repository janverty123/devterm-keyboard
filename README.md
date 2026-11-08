# DevTerm Keyboard

An offline, native Android software keyboard for developers, Termux, SSH, and code editors. Written in Kotlin with Android InputMethodService.

**Status: developer preview.** Source code is provided but the APK and terminal compatibility have not been verified on hardware.

## Included
- ABC: QWERTY, number row, Shift, Backspace, Space and Enter.
- SYM: programming punctuation, braces, brackets, pipes, slash, backslash, tilde and more.
- NAV: Esc, Tab, arrows, Home, End, Insert, Delete, Page Up/Down, Ctrl shortcuts.
- FN: F1-F12, Caps Lock, Num Lock, Scroll Lock, Pause, Print Screen and Menu.
- Always-visible scrollable terminal strip: Esc, Tab, Ctrl, Alt, arrows, Fn.
- One-shot Ctrl, Alt, Shift and Meta modifiers.
- No Internet permission, clipboard history or keyboard logging.

## Build
Install Android Studio with JDK 17 and SDK Platform 35. Open this project and run **Build > Build APK(s)**.

You can instead build in the cloud using **Actions > Android debug APK > Run workflow** in GitHub. On a successful run, find the debug APK in **Artifacts**. Initial CI success is not yet confirmed.

## Activate
Install the APK, open DevTerm Keyboard, tap **Enable keyboard**, enable it in Android settings, tap **Choose keyboard**, and select it. Then open Termux or a text editor.

## Important limitations
This is a system keyboard, not a terminal emulator. Android apps decide whether to accept hardware-like Ctrl/Esc/Fn key events. Ordinary text input uses InputConnection.commitText; special keys use sendKeyEvent. Device testing is required.

All keyboards can potentially read what you type, so only install code you trust and review the source before entering sensitive data.

## Project structure
- app/src/main/java/com/oasan/devtermkeyboard/ contains the Android service, key models, layers, event sender and setup screen
- docs/PLAN.md contains the development plan
- docs/GITHUB-TERMUX.md contains a Termux publishing guide
- .github/workflows/android.yml builds a debug APK

## License
No license has been selected. Repository owner should choose one before broad redistribution.
