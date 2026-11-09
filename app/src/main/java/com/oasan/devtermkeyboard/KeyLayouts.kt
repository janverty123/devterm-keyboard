package com.oasan.devtermkeyboard

import android.view.KeyEvent

/** Independent standard and developer layouts. Enter is always the last bottom-row key. */
internal object KeyLayouts {
    private fun t(label: String, value: String = label, weight: Float = 1f) =
        KeySpec(label, KeyAction.Text(value), weight)
    private fun k(label: String, code: Int, weight: Float = 1f) =
        KeySpec(label, KeyAction.Hardware(code), weight)
    private fun m(label: String, mod: KeyboardModifier) =
        KeySpec(label, KeyAction.Modifier(mod), 1.15f)
    private fun l(label: String, layer: KeyboardLayer) =
        KeySpec(label, KeyAction.Layer(layer), 1.25f)
    private fun hotkey(label: String, code: Int) =
        KeySpec(label, KeyAction.Shortcut(code, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON))

    fun toolbar(): List<KeySpec> = listOf(
        k("Esc", KeyEvent.KEYCODE_ESCAPE),
        k("Tab", KeyEvent.KEYCODE_TAB),
        m("Ctrl", KeyboardModifier.CTRL),
        m("Alt", KeyboardModifier.ALT),
        KeySpec("Shift", KeyAction.Shift, 1.4f),
        KeySpec("Paste", KeyAction.Paste, 1.4f),
        KeySpec("Copy", KeyAction.Copy, 1.4f),
        l("Nav", KeyboardLayer.NAVIGATION),
        l("Fn", KeyboardLayer.FUNCTIONS),
        k("←", KeyEvent.KEYCODE_DPAD_LEFT),
        k("↓", KeyEvent.KEYCODE_DPAD_DOWN),
        k("↑", KeyEvent.KEYCODE_DPAD_UP),
        k("→", KeyEvent.KEYCODE_DPAD_RIGHT),
        m("Meta", KeyboardModifier.META)
    )

    private fun footer(symbols: Boolean = false): List<KeySpec> = listOf(
        l(if (symbols) "ABC" else "?123",
            if (symbols) KeyboardLayer.LETTERS else KeyboardLayer.SYMBOLS),
        t(",", ",", 0.8f),
        KeySpec("Emoji", KeyAction.Layer(KeyboardLayer.EMOJI), 1.0f, "Emojis"),
        t("English", " ", 4.3f),
        t(".", ".", 0.8f),
        k("↵", KeyEvent.KEYCODE_ENTER, 1.55f)
    )

    fun rows(mode: KeyboardMode, layer: KeyboardLayer): List<List<KeySpec>> = when (layer) {
        KeyboardLayer.LETTERS -> {
            val letters = listOf(
                "qwertyuiop".map { t(it.toString()) },
                "asdfghjkl".map { t(it.toString()) },
                listOf(KeySpec("Shift", KeyAction.Shift, 1.7f,
                    "Shift, double tap or hold to lock uppercase")) +
                    "zxcvbnm".map { t(it.toString()) } +
                    k("⌫", KeyEvent.KEYCODE_DEL, 1.6f),
                footer()
            )
            if (mode == KeyboardMode.DEVELOPER) {
                listOf("1234567890".map { t(it.toString()) }) + letters
            } else letters
        }
        KeyboardLayer.SYMBOLS -> {
            val symbols = listOf(
                listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")").map { t(it) },
                listOf("[", "]", "{", "}", "<", ">", "\\", "|", "/", "?").map { t(it) },
                listOf("-", "_", "=", "+", ";", ":", "'", "\"", "`", "~").map { t(it) },
                listOf(
                    l("ABC", KeyboardLayer.LETTERS),
                    KeySpec("Shift", KeyAction.Shift, 1.6f),
                    KeySpec("Emoji", KeyAction.Layer(KeyboardLayer.EMOJI), 1.0f),
                    t("space", " ", 4.0f),
                    k("⌫", KeyEvent.KEYCODE_DEL, 1.4f),
                    k("↵", KeyEvent.KEYCODE_ENTER, 1.5f)
                )
            )
            if (mode == KeyboardMode.DEVELOPER) symbols.take(3) +
                listOf(listOf("//", "->", "=>", "&&", "||", "!=", "==", "::", "%%").map { t(it) }) +
                listOf(symbols.last()) else symbols
        }
        KeyboardLayer.NAVIGATION -> listOf(
            listOf(k("Esc", KeyEvent.KEYCODE_ESCAPE), k("Tab", KeyEvent.KEYCODE_TAB),
                k("Home", KeyEvent.KEYCODE_MOVE_HOME), k("End", KeyEvent.KEYCODE_MOVE_END),
                k("PgUp", KeyEvent.KEYCODE_PAGE_UP), k("PgDn", KeyEvent.KEYCODE_PAGE_DOWN)),
            listOf(k("Ins", KeyEvent.KEYCODE_INSERT), k("Del", KeyEvent.KEYCODE_FORWARD_DEL),
                k("←", KeyEvent.KEYCODE_DPAD_LEFT), k("↑", KeyEvent.KEYCODE_DPAD_UP),
                k("↓", KeyEvent.KEYCODE_DPAD_DOWN), k("→", KeyEvent.KEYCODE_DPAD_RIGHT)),
            listOf(KeySpec("Copy", KeyAction.Copy), KeySpec("Paste", KeyAction.Paste),
                KeySpec("Cut", KeyAction.Cut), KeySpec("All", KeyAction.SelectAll),
                hotkey("Undo", KeyEvent.KEYCODE_Z), hotkey("Redo", KeyEvent.KEYCODE_Y)),
            listOf(hotkey("^C", KeyEvent.KEYCODE_C), hotkey("^D", KeyEvent.KEYCODE_D),
                hotkey("^Z", KeyEvent.KEYCODE_Z), hotkey("^L", KeyEvent.KEYCODE_L),
                hotkey("^A", KeyEvent.KEYCODE_A), hotkey("^E", KeyEvent.KEYCODE_E)),
            listOf(l("ABC", KeyboardLayer.LETTERS), l("?123", KeyboardLayer.SYMBOLS),
                KeySpec("Shift", KeyAction.Shift, 1.4f),
                t("space", " ", 3.5f), k("⌫", KeyEvent.KEYCODE_DEL, 1.3f),
                k("↵", KeyEvent.KEYCODE_ENTER, 1.5f))
        )
        KeyboardLayer.FUNCTIONS -> listOf(
            (1..4).map { k("F$it", KeyEvent.KEYCODE_F1 + it - 1) },
            (5..8).map { k("F$it", KeyEvent.KEYCODE_F1 + it - 1) },
            (9..12).map { k("F$it", KeyEvent.KEYCODE_F1 + it - 1) },
            listOf(k("Caps", KeyEvent.KEYCODE_CAPS_LOCK),
                k("Num", KeyEvent.KEYCODE_NUM_LOCK), k("ScrLk", KeyEvent.KEYCODE_SCROLL_LOCK),
                k("Pause", KeyEvent.KEYCODE_BREAK), k("PrtSc", KeyEvent.KEYCODE_SYSRQ),
                k("Menu", KeyEvent.KEYCODE_MENU)),
            listOf(l("ABC", KeyboardLayer.LETTERS), l("?123", KeyboardLayer.SYMBOLS),
                l("Nav", KeyboardLayer.NAVIGATION),
                m("Meta", KeyboardModifier.META),
                t("space", " ", 3.2f), k("↵", KeyEvent.KEYCODE_ENTER, 1.5f))
        )
        KeyboardLayer.EMOJI -> emptyList()
    }
}
