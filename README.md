# Keylix — an Android keyboard for everyday typing and developers

Keylix is a native Kotlin Android system keyboard with a **Standard** mode for everyday writing and a **Dev** mode for Termux, SSH, command-line tools, and code editing. It is not Gboard and does not use Gboard's proprietary source or assets.

## v0.7.0 — Inline voice, polished key alignment and new launcher icon

- **Dictation stays on the keyboard:** Tap Keylix's microphone to start Android SpeechRecognizer directly inside the IME, instead of launching a voice-typing Activity. The header shows **Starting…**, **Listening…**, or **Processing…**, plus native Back, stop and mic icons. Partial recognition is previewed; final speech is committed once without pressing Enter. Tap the mic or Back to cancel.
- **First-use microphone permission:** Keylix requests RECORD_AUDIO through the setup screen. This system permission dialog is the only separate view; normal dictation keeps the current text field and keyboard visible. If no recognizer is installed, the keyboard explains how to enable one. Some Android speech services may need network access or a downloaded language model.
- **Centered vector key icons:** Emoji, Backspace, Enter and keyboard-switcher drawables are centered by a LayerDrawable, not a top-aligned compound text drawable.
- **Gboard-inspired bottom row:** ?123, comma, Emoji, Space, period, Enter — with cleaner, consistently rounded keycaps.
- **Predictive separators:** Thin dividers between the three word suggestions, with equal widths.
- **New launcher brand:** A crisp **green keyboard** on a **dark navy** background (editable native vector asset), replacing the previous white-outline icon.
- **No proprietary Gboard assets:** Android system speech APIs and independent VectorDrawables are used.

The APK is a prerelease. A successful build does not prove real-device microphone behavior; test on Android 16 with Google Speech Services installed and microphone permission granted.

## v0.6.0 — Custom toolbar and searchable emoji

- **Emoji search:** Open Emoji, tap the magnifying-glass search field, then type a keyword using Keylix's compact search keyboard. Emoji results are filtered by searchable Unicode names and common English aliases such as "heart", "dog", "party", "fire" and "philippines". Search does not launch another Android keyboard and therefore cannot cover the picker.
- **Toolbar customization:** Open the four-square Tools menu → **Customize**. Choose either of the two pinned slots and tap an available shortcut to assign it, or long-press and drag a shortcut onto a slot. Use Reset to restore the defaults (Paste and Emoji). The selections are stored locally.
- **Fixed navigation and function access:** Dev Navigation and Function Keys are available through the tools menu as well as Dev mode.
- **Emoji containment:** The emoji category strip and bottom action row remain outside a clipped, fixed-height scrolling viewport; the search input and its results are separate from the host text field.
- **More Unicode emoji:** Extended category with newer symbols, skin tones and sequences when supported by the Android system emoji font.
- **Control icons:** All toolbar actions use native Android VectorDrawable icons, including a new Customize icon. Colorful emoji are rendered only in the emoji picker itself.

The previous v0.5.1 source did not build due to a missing catalog method. v0.6.0 includes that method and runs GitHub Actions unit tests plus APK compilation. Physical Android 16 device testing is still required for drag-and-drop, gesture navigation and emoji clipping.

## v0.5.1 — Menu and emoji scrolling refinement

The tools menu now has a fixed, scrollable viewport and includes Dev navigation and Function keys. The emoji picker uses a bounded scrolling region, responsive 8/10-column grids and an expanded **More** category of Unicode emojis. Android's emoji font supplies the colorful glyph artwork; newer emoji depend on device font support.

## v0.5 — Gboard-inspired tools menu and emoji picker

The keyboard uses familiar mobile-keyboard interaction patterns drawn from Gboard's toolbar and tools grid, while all control icons and the app logo are original Android VectorDrawable graphics. No button uses an emoji character as a substitute for an icon.

- **Tools menu:** The leftmost four-squares icon opens a nine-tile tools panel (Mode, Emoji, Paste, Editing, Themes, Settings, Keyboard switch, Voice, Symbols), replacing the keyboard rows instead of floating over them. A back arrow returns to the keys.
- **Simplified upper toolbar:** Tools button, word suggestions or compact shortcuts, clipboard, emoji button, and vector microphone. There is no always-visible Settings gear; settings and themes live in the tools panel.
- **Developer keys:** Dev mode retains its scrollable Esc, Tab, Ctrl, Alt, Shift, Paste, Copy, Nav, F-keys, arrows and Meta strip below the main toolbar.
- **Real vector controls:** Microphone, clipboard, emoji picker, tools menu, settings, backspace, Enter and the emoji-category tabs use SVG-style Android VectorDrawable paths. Emoji *content* itself remains Unicode and uses the phone's system color emoji font, avoiding broken skin-tone and ZWJ sequences.
- **Emoji picker:** Ten categories including recent emojis, smileys, people, animals, food, activities, travel, objects, symbols and flags. Category icons stay pinned on top; the emoji grid scrolls only inside a fixed clipped viewport, and ABC/Space/Delete/Enter remain pinned below. The data set is larger than v0.4.
- **Original accents:** Keys retain a subtle solid-color press flash without a spreading ripple.

## Install

Download **[Keylix v0.5.0 APK from GitHub Releases](https://github.com/janverty123/devterm-keyboard/releases/tag/v0.5.0)**. Under Assets, install `Keylix-v0.5.0.apk` directly on Android, not the source ZIP. This is a debug-signed prerelease: if an older debug APK has a different signing key, Android may require uninstalling it first.

## Enable and use

1. Install and open Keylix.
2. Enable the keyboard in Android system settings, then select it as your active keyboard.
3. Tap the **grid/tools** icon on the keyboard toolbar for Modes, Themes, Editing and other tools.
4. Tap the vector **emoji** button for the emoji picker; scroll the grid, not the category toolbar.
5. Tap the vector microphone for Android's installed speech-recognition provider when available.

## Privacy and reliability

Keylix has no INTERNET permission, analytics or background typing logs. The optional speech provider may process voice on its own servers; normal typing does not require microphone permission. The clipboard Paste action inserts text without automatically pressing Enter, which is important inside Termux. The basic offline English prediction/autocorrect is optional in Standard mode only; it is not a copy of Gboard's proprietary prediction system.

### Emoji rendering / Google's emoji

Android provides a system color emoji font, commonly Google's **Noto Color Emoji**, so emoji characters can render in the system's native color style without shipping hundreds of manually copied images. The Noto Emoji project is open source under published licenses; this build uses the system font rather than bundling its assets. See [Google's Noto Emoji repository](https://github.com/googlefonts/noto-emoji) for license details.

## Development

Kotlin, Android `InputMethodService`, Android SDK 35, JDK 17, Gradle 8.11.1. Run `gradle --no-daemon testDebugUnitTest assembleDebug` or use the provided GitHub Actions workflow. App package ID remains `com.oasan.devtermkeyboard` for upgrade continuity. Sources under `app/src/main/java/com/oasan/devtermkeyboard`.

Physical testing is still necessary on Android 16 gesture navigation, landscape, Termux, emoji grid scrolling and voice recognition. See [docs/TESTING.md](docs/TESTING.md) and [docs/PLAN.md](docs/PLAN.md).
