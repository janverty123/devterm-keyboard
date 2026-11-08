# Keylix roadmap

## v0.4 — Restructure and dual-mode foundation
- [x] One-word app branding, original minimal keyboard logo
- [x] Standard typing layout and developer terminal mode
- [x] Persistent mode selection in app settings and keyboard header
- [x] Reposition rightmost Enter, textual Shift and independent microphone
- [x] Implement categorized system-rendered Unicode emoji picker
- [x] Add simple on-device English word suggestions and spelling correction
- [x] Add toggles for suggestions and autocorrect and disable them in dev/password inputs
- [x] Switch to solid accent key flash, no ripple animation
- [x] Add seven keyboard themes, updated setup UI and appearance settings
- [x] Voice recognition activity with external-provider path and fallback
- [x] Basic unit tests for correction and suggestions
- [ ] Android device tests: Android 16 portrait/landscape/gesture bar, Termux, messaging apps
- [ ] Advanced predictive language models, multilingual dictionaries and grammar assistance
- [ ] Stable signing key for seamless updates, regression tests and automated emulator coverage

## Architecture
- `DeveloperKeyboardService.kt`: IME UI state, composing text and dispatch
- `KeyLayouts.kt`: Standard vs Dev, symbols, navigation and F-keys
- `WordSuggestions.kt`: small local English lexicon and one-edit autocorrection
- `EmojiCatalog.kt`: system emoji categories without SVG dependencies
- `VoiceRecognitionActivity.kt`: recognizer handoff and local result callback
- `KeyPreview.kt`: optional character preview
- `KeyboardPreferences.kt`: local mode, themes, and typing preferences
- `MainActivity.kt`: setup, settings, theme chooser, test field

## Known differences from Gboard
Gboard uses proprietary language models, neural predictions, multilanguage vocabularies, gesture typing and internal Google services; Keylix implements only basic suggestions and autocorrection and invokes public Android speech-recognition interfaces. No claim of full feature equivalence.

## v0.5 menu and emoji overhaul
- [x] Study Gboard's compact toolbar and expanded tool grid
- [x] Replace menu control emoji/glyph icons with standalone Android vector artwork
- [x] Move Settings and Mode selection to a full tools menu
- [x] Keep developer shortcuts in their own strip under the general toolbar
- [x] Rebuild emoji scroller with clipped viewport and pinned category/footer rows
- [x] Add Recent, Activities, Objects and Flags emoji categories
- [x] Continue rendering real emoji via Android system color emoji font
- [ ] Validate on Android 16 hardware with gesture nav and high-velocity scrolls
- [ ] Add emoji search and editable/favorite shortcut tiles
- [ ] Accessibility testing and stable release signing
