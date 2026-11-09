# Keylix v0.7.0 — Inline voice typing and visual polish

Based on user-provided Gboard screenshots, Keylix now takes an independently implemented approach to Gboard's voice/status toolbar and conventional key spacing.

### Changes

- **Inline speech recognition:** The microphone triggers Android's installed SpeechRecognizer from within the keyboard itself. No external voice-typing window. In the keyboard toolbar, see **Starting…**, **Listening…**, and **Processing…**, plus Back/Stop/Microphone native icons. Text is inserted at the original cursor once after recognition, without automatically sending Enter.
- **Voice permission:** The first microphone tap can show Keylix's setup page and Android's one-time RECORD_AUDIO permission dialog. Later dictation stays in the keyboard. Android may require Google Speech Services or another installed recognizer, and some services process audio online.
- **Centered symbols:** Backspace, Enter, Emoji and keyboard-switcher actions draw their own vector icons centered in the key background, fixing the previous off-center appearance.
- **Balanced bottom row:** ?123, comma, Emoji, Space, period, rightmost Enter.
- **Prediction dividers:** Thin separators between three evenly spaced word suggestions.
- **Updated app icon:** All-mint keyboard silhouette and subtle terminal prompt on a deep navy launcher background.
- Retains Standard/Dev modes, terminal modifiers, customizable toolbar, emoji categories, and emoji keyword search.

### Install

Under GitHub Releases **Assets**, choose `Keylix-v0.7.0.apk` (not Source code ZIP). This is a debug-signed prerelease: an APK with a different debug signing certificate may require you to uninstall the previous version and re-enable Keylix.

### Device verification

Even if GitHub Actions compiles the Android APK and tests pass, we still need to test real microphone capture, speech-provider availability, visual alignment, and system navigation insets on your Android phone. Keylix has no INTERNET permission, but external speech services may send audio online.
