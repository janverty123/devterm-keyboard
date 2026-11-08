package com.oasan.devtermkeyboard

import android.view.KeyEvent

/** Declarative key definitions shared by every phone and screen orientation. */
internal object KeyLayouts {
    private fun text(label: String, output: String = label, weight: Float = 1f) =
        KeySpec(label, KeyAction.Text(output), weight)
    private fun key(label: String, code: Int, weight: Float = 1f) =
        KeySpec(label, KeyAction.Hardware(code), weight)
    private fun modifier(label: String, modifier: KeyboardModifier) =
        KeySpec(label, KeyAction.Modifier(modifier), 1.15f)
    private fun layer(label: String, layer: KeyboardLayer) =
        KeySpec(label, KeyAction.Layer(layer), 1.25f)
    private fun shortcut(label: String, code: Int) =
        KeySpec(label, KeyAction.Shortcut(code,
            KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON), 1.1f)

    /** Scrollable toolbar. Copy / Paste are among its first six items. */
    fun toolbar() = listOf(
        key("Esc", KeyEvent.KEYCODE_ESCAPE),
        key("Tab", KeyEvent.KEYCODE_TAB),
        modifier("Ctrl", KeyboardModifier.CTRL),
        modifier("Alt", KeyboardModifier.ALT),
        KeySpec("⇧", KeyAction.Shift, 1.15f, "Shift, double tap or hold for Caps Lock"),
        KeySpec("Mic", KeyAction.Voice, 1.15f, "Voice typing"),
        KeySpec("Copy", KeyAction.Copy, 1.25f),
        KeySpec("Paste", KeyAction.Paste, 1.25f),
        key("←", KeyEvent.KEYCODE_DPAD_LEFT),
        key("→", KeyEvent.KEYCODE_DPAD_RIGHT),
        key("↑", KeyEvent.KEYCODE_DPAD_UP),
        key("↓", KeyEvent.KEYCODE_DPAD_DOWN),
        layer("Fn", KeyboardLayer.FUNCTIONS),
        modifier("Meta", KeyboardModifier.META)
    )

    fun rows(layer: KeyboardLayer): List<List<KeySpec>> = when (layer) {
        KeyboardLayer.LETTERS -> listOf(
            "1234567890".map { text(it.toString()) },
            "qwertyuiop".map { text(it.toString()) },
            "asdfghjkl".map { text(it.toString()) },
            listOf(KeySpec("⇧", KeyAction.Shift, 1.55f, "Shift / hold for Caps Lock")) +
                "zxcvbnm".map { text(it.toString()) } +
                key("⌫", KeyEvent.KEYCODE_DEL, 1.55f),
            listOf(
                layer("?123", KeyboardLayer.SYMBOLS),
                text(","),
                KeySpec("🌐", KeyAction.Picker, 0.85f, "Change keyboard"),
                text("English (US)", " ", 4.1f),
                text("."),
                key("↵", KeyEvent.KEYCODE_ENTER, 1.55f),
                layer("Nav", KeyboardLayer.NAVIGATION)
            )
        )
        KeyboardLayer.SYMBOLS -> listOf(
            listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")").map { text(it) },
            listOf("[", "]", "{", "}", "<", ">", "\\", "|", "/", "?").map { text(it) },
            listOf("-", "_", "=", "+", ";", ":", "'", "\"", "`", "~").map { text(it) },
            listOf("//", "->", "=>", "&&", "||", "!=", "==", "::", "%%").map { text(it) },
            listOf(
                layer("ABC", KeyboardLayer.LETTERS),
                KeySpec("⇧", KeyAction.Shift, 1.15f, "Shift / Caps Lock"),
                KeySpec("🌐", KeyAction.Picker, 0.85f, "Change keyboard"),
                text("space", " ", 4.1f),
                key("⌫", KeyEvent.KEYCODE_DEL, 1.5f),
                key("↵", KeyEvent.KEYCODE_ENTER, 1.5f),
                layer("Nav", KeyboardLayer.NAVIGATION)
            )
        )
        KeyboardLayer.NAVIGATION -> listOf(
            listOf(
                key("Esc", KeyEvent.KEYCODE_ESCAPE), key("Tab", KeyEvent.KEYCODE_TAB),
                key("Home", KeyEvent.KEYCODE_MOVE_HOME), key("End", KeyEvent.KEYCODE_MOVE_END),
                key("PgUp", KeyEvent.KEYCODE_PAGE_UP), key("PgDn", KeyEvent.KEYCODE_PAGE_DOWN)
            ),
            listOf(
                key("Ins", KeyEvent.KEYCODE_INSERT), key("Del", KeyEvent.KEYCODE_FORWARD_DEL),
                key("←", KeyEvent.KEYCODE_DPAD_LEFT), key("↑", KeyEvent.KEYCODE_DPAD_UP),
                key("↓", KeyEvent.KEYCODE_DPAD_DOWN), key("→", KeyEvent.KEYCODE_DPAD_RIGHT)
            ),
            listOf(
                KeySpec("⇧", KeyAction.Shift, 1.0f, "Shift, double tap or hold for Caps Lock"),
                KeySpec("Copy", KeyAction.Copy), KeySpec("Paste", KeyAction.Paste),
                KeySpec("Cut", KeyAction.Cut), KeySpec("All", KeyAction.SelectAll),
                shortcut("Undo", KeyEvent.KEYCODE_Z),
                shortcut("Redo", KeyEvent.KEYCODE_Y)
            ),
            listOf(
                shortcut("^C", KeyEvent.KEYCODE_C), shortcut("^D", KeyEvent.KEYCODE_D),
                shortcut("^Z", KeyEvent.KEYCODE_Z), KeySpec("C⇧V", KeyAction.Shortcut(KeyEvent.KEYCODE_V,
                    KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON or
                    KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)),
                shortcut("^A", KeyEvent.KEYCODE_A), shortcut("^E", KeyEvent.KEYCODE_E)
            ),
            listOf(
                layer("ABC", KeyboardLayer.LETTERS), layer("?123", KeyboardLayer.SYMBOLS),
                layer("Fn", KeyboardLayer.FUNCTIONS),
                KeySpec("🌐", KeyAction.Picker, 0.85f, "Change keyboard"),
                text("space", " ", 3.6f),
                key("⌫", KeyEvent.KEYCODE_DEL, 1.25f),
                key("↵", KeyEvent.KEYCODE_ENTER, 1.25f)
            )
        )
        KeyboardLayer.FUNCTIONS -> listOf(
            (1..4).map { key("F$it", KeyEvent.KEYCODE_F1 + it - 1) },
            (5..8).map { key("F$it", KeyEvent.KEYCODE_F1 + it - 1) },
            (9..12).map { key("F$it", KeyEvent.KEYCODE_F1 + it - 1) },
            listOf(
                key("Caps", KeyEvent.KEYCODE_CAPS_LOCK),
                key("Num", KeyEvent.KEYCODE_NUM_LOCK),
                key("ScrLk", KeyEvent.KEYCODE_SCROLL_LOCK),
                key("Pause", KeyEvent.KEYCODE_BREAK),
                key("PrtSc", KeyEvent.KEYCODE_SYSRQ),
                key("Menu", KeyEvent.KEYCODE_MENU)
            ),
            listOf(
                layer("ABC", KeyboardLayer.LETTERS), layer("?123", KeyboardLayer.SYMBOLS),
                layer("Nav", KeyboardLayer.NAVIGATION),
                modifier("Meta", KeyboardModifier.META),
                KeySpec("🌐", KeyAction.Picker, 0.85f, "Change keyboard"),
                text("space", " ", 2.3f),
                key("↵", KeyEvent.KEYCODE_ENTER)
            )
        )
    }
}
