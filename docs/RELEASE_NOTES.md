# Keylix v0.6.0 — Searchable emojis and customizable toolbar

The Gboard-inspired interface continues to use original Android VectorDrawable controls and independently implemented UI. No proprietary Google code, artwork or data files are copied.

### New in this release

- **Search emoji names:** Tap the search field at the top of Emoji, then type using the built-in search keyboard. Matches use Unicode character names and curated English keyword aliases; tapping a result inserts the emoji into your app.
- **Personalized toolbar:** Four-square Tools menu → Customize. Tap a slot, then select an available action; long-press and drag also supported. Two pinned quick actions, stored privately on device. Reset and Done controls included.
- **Gboard-style cleaner tools area:** A scrollable, clipped tools panel keeps Dev Navigation and Function Keys accessible without adding extra permanent key rows.
- **Expanded emoji coverage:** Includes more Unicode sequences and skin tone options, under the More category. Device font support determines which ones appear correctly.
- **Scrollable emoji viewport:** The categories, search field and bottom action row are not children of the scrollable emoji grid, preventing content from drawing on top of the controls.
- Original vector icons for microphone, clipboard, emoji toggle, customize, backspace and Enter; no placeholder emoji icons in buttons.

### Installation

Choose the `Keylix-v0.6.0.apk` file from **Assets**. Install it, then enable Keylix in Android's keyboard settings. This is a debug-signed prerelease; uninstall an older differently signed build if Android reports a signature mismatch, then re-enable the keyboard.

### Limitations

Prediction is a small offline English dictionary, not Gboard's proprietary model. Emoji search is keyword-based; there is no GIF or sticker search. Dictation uses Android's installed speech provider, which may use the internet. The keyboard itself requests no Internet permission and does not log text. Device verification is still needed.
