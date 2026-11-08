# Android device test checklist

Use a **new build** from GitHub Actions. Record phone model, Android version, system navigation mode, and target app when reporting problems.

## Screen and resizing
- [ ] The bottom row is entirely above the gesture bar / 3-button navigation.
- [ ] The floating Android keyboard switcher does not obscure Backspace or Enter.
- [ ] Rotate landscape: all rows and the Space key remain usable.
- [ ] Enable compact height in app settings, close/reopen the keyboard, and check changed sizing.
- [ ] Scroll the top utility toolbar horizontally to reach arrows and Fn.

## Typing and mode switching
- [ ] Type letters, numbers, symbols and punctuation in a normal text field.
- [ ] Tap Shift then a letter; only that letter is uppercase.
- [ ] Double-tap or hold Shift until ⇪ appears; type several uppercase letters; tap Shift to unlock.
- [ ] Long-press Space: system "Change keyboard" picker appears **without inserting a space**.
- [ ] Tap 🌐: system keyboard picker appears.

## Copy/paste
- [ ] Select text in a regular editor, press Copy, then Paste in another field.
- [ ] Verify Cut / All in the navigation layout.
- [ ] Test paste into Termux; different apps support clipboard actions differently.

## Terminal
- [ ] Ctrl+C interrupts a running command (\`sleep 30\`).
- [ ] Ctrl+D signals end-of-input in a terminal, as applicable.
- [ ] Esc works in Vim and Nano, and Tab completes a filename in the shell.
- [ ] Arrow keys, Home, End and F1–F12 where supported.

## Accessibility/security
- [ ] Long-press and keys still work with TalkBack enabled.
- [ ] Check key visibility at large system font scale.
- [ ] The app requests no INTERNET permission; neither text nor clipboard is logged.
