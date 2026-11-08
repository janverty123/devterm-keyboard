# Keylix Android device test checklist

## Setup
- [ ] Launcher shows Keylix name and keyboard logo.
- [ ] Enable Keylix in Android settings and choose as active keyboard.
- [ ] Select Standard or Dev in the settings screen; selection persists.
- [ ] Switch modes using the first keyboard-header chip.

## Standard layout
- [ ] Enter appears as the **rightmost bottom-row key** and is not next to the navigation controls.
- [ ] The bottom row contains ?123, ☺ emoji, comma, space, period, Enter.
- [ ] Shift label is visible; tap for one uppercase letter, hold or double-tap for Caps Lock.
- [ ] Suggestions appear for an English word; selecting a suggestion inserts it.
- [ ] Type `teh ` in a normal text field; autocorrect changes it to `the `.
- [ ] Turn Autocorrect or Suggestions off in settings; behavior changes.
- [ ] Password fields and code/terminal inputs do not expose typed suggestions.

## Dev layout and terminals
- [ ] A persistent number row and scrollable Esc/Tab/Ctrl/Alt/Shift/Paste/Fn strip appear.
- [ ] Enter is still at the far right of the bottom row.
- [ ] Ctrl+C interrupts `sleep 30` in Termux.
- [ ] Open Vim, test Escape, arrows, Tab, Ctrl shortcuts, and F1–F12 where relevant.
- [ ] Copy text into Android clipboard, tap Paste in Termux; text inserts but Enter is never sent.

## Emoji/voice
- [ ] Emoji categories change and tapping an emoji inserts a glyph (including multi-codepoint emoji).
- [ ] Switching back to ABC returns to the previous typing mode.
- [ ] Tap the standalone microphone button; Android speech dialog opens when a provider is installed.
- [ ] Speak a phrase; its text appears in the original focused field without Enter.
- [ ] No external speech provider: fallback shows appropriate permission/error.
- [ ] No mic permission: normal typing still works.

## Themes and responsiveness
- [ ] Preview each of the seven themes.
- [ ] Press keys; quick **solid** accent flash, no spreading ripple.
- [ ] Test with navigation gestures and 3-button mode.
- [ ] Test portrait/landscape and compact sizing; bottom row remains fully tappable.
- [ ] Optional haptics/character bubbles can be disabled.

## Privacy
- [ ] Confirm AndroidManifest has no INTERNET permission.
- [ ] No persistent typing history, clipboard history or saved dictation.
