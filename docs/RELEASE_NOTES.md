# Keylix v0.5.0 — Tools menu & emoji scrolling overhaul

This developer preview refines Keylix's keyboard interface using familiar Gboard-style toolbar/menu patterns. The keyboard and icons are independently implemented; no proprietary Gboard graphics or code are included.

### Changes
- A compact main toolbar with vector **Tools**, **Clipboard**, **Emoji**, and **Mic** icons; settings moved into the expanded tools panel.
- A **3×3 tools grid** with Mode, Emoji, Paste, Text editing, Themes, Settings, Switch input, Voice, and Symbols.
- No emoji glyph placeholders for control buttons; Android VectorDrawables for menu icons, emoji toggle, microphone, category tabs, Backspace and Enter.
- Emoji scrolling constrained to a dedicated clipped viewport. Category row and ABC/Space/Delete/Enter row remain fixed.
- Ten categorized emoji groups with Recent, Activities, Objects, Flags and an expanded set of Unicode emoji.
- Dev mode retains its terminal shortcuts, separate from the main Gboard-inspired controls.
- Original solid accent key-press animation retained.

### Install
Open Assets and tap **Keylix-v0.5.0.apk**. The source-code ZIP is not the Android installer. This is a debug-signed prerelease. Signature mismatches with older APKs may require uninstall/reinstall and re-enabling the keyboard.

### Emoji styling
Actual emoji are rendered by your Android device's color emoji font (commonly Noto Color Emoji). This preserves Unicode sequences such as flags, professions and skin tones; no Gboard-owned emoji asset bundle is redistributed.

### Testing status
The APK CI build and unit tests verify compilation, not physical device behavior. Check scroll clipping on Android 16, changing emoji categories, and keyboard bars in Termux.
