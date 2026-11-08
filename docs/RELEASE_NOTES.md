# Keylix v0.5.1 — Refined Gboard-inspired menu and emoji containment

- Refined the keyboard's tools menu into a compact, bounded scroll viewport so extended tools do not increase the IME height or cover the keys.
- Added Dev navigation and Function keys to the tools grid, making developer features accessible without permanent top-row clutter.
- Fixed emoji viewport height and tightened clipping to keep emoji rows from scrolling over the category controls and bottom buttons.
- Responsive emoji grid: 8 columns in portrait, 10 in landscape; slightly smaller glyphs for narrow cells.
- Added a **More** emoji category with additional Unicode sequences, including skin tones, accessibility, newer animals, objects and joined emoji.
- Preserved original Android VectorDrawable toolbar icons (microphone, emoji, clipboard, backspace, Enter, tools). No control-button emoji placeholders.
- Gboard was used as a layout and interaction reference, not as a source of proprietary icons, code, or emoji assets.
- Device rendering depends on the installed Android emoji font; newer Unicode characters may show as fallback boxes on older Android releases.

## Testing
GitHub Actions builds and unit tests run on commit. Check portrait/landscape, gesture navigation, long emoji category scrolling and Termux shortcuts on a real Android device.
