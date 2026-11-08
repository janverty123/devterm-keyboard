package com.oasan.devtermkeyboard

import android.view.KeyEvent

/** All key definitions live here so layouts can be edited without touching IME logic. */
object KeyLayouts {
    private fun t(label: String, output: String = label, weight: Float = 1f) =
        KeySpec(label, KeyAction.Text(output), weight)

    private fun k(label: String, code: Int, weight: Float = 1f) =
        KeySpec(label, KeyAction.Hardware(code), weight)

    private fun m(label: String, modifier: KeyboardModifier) =
        KeySpec(label, KeyAction.Modifier(modifier), 1.35f)

    private fun l(label: String, layer: KeyboardLayer) =
        KeySpec(label, KeyAction.Layer(layer), 1.45f)

    private fun hotkey(label: String, code: Int) =
        KeySpec(label, KeyAction.Shortcut(code, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON), 1.25f)

    fun utilityRow(): List<KeySpec> = listOf(
        k("ESC", KeyEvent.KEYCODE_ESCAPE),
        k("TAB", KeyEvent.KEYCODE_TAB),
        m("CTRL", KeyboardModifier.CTRL),
        m("ALT", KeyboardModifier.ALT),
        k("←", KeyEvent.KEYCODE_DPAD_LEFT),
        k("↓", KeyEvent.KEYCODE_DPAD_DOWN),
        k("↑", KeyEvent.KEYCODE_DPAD_UP),
        k("→", KeyEvent.KEYCODE_DPAD_RIGHT),
        l("FN", KeyboardLayer.FUNCTIONS)
    )

    fun rows(layer: KeyboardLayer): List<List<KeySpec>> = when (layer) {
        KeyboardLayer.LETTERS -> listOf(
            "1234567890".map { t(it.toString()) },
            "qwertyuiop".map { t(it.toString()) },
            "asdfghjkl".map { t(it.toString()) },
            listOf(m("SHIFT", KeyboardModifier.SHIFT)) +
                "zxcvbnm".map { t(it.toString()) } +
                k("⌫", KeyEvent.KEYCODE_DEL, 1.5f),
            listOf(
                l("SYM", KeyboardLayer.SYMBOLS),
                t(","), t("/"),
                t("SPACE", " ", 4.5f),
                t("."),
                k("↵", KeyEvent.KEYCODE_ENTER, 1.5f),
                l("NAV", KeyboardLayer.NAVIGATION)
            )
        )
        KeyboardLayer.SYMBOLS -> listOf(
            listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")").map { t(it) },
            listOf("[", "]", "{", "}", "<", ">", "\\", "|", "/", "?").map { t(it) },
            listOf("-", "_", "=", "+", ";", ":", "'", "\"", "`", "~").map { t(it) },
            listOf(
                l("ABC", KeyboardLayer.LETTERS),
                m("SHIFT", KeyboardModifier.SHIFT),
                t("SPACE", " ", 4.5f),
                k("⌫", KeyEvent.KEYCODE_DEL, 1.5f),
                k("↵", KeyEvent.KEYCODE_ENTER, 1.5f),
                l("NAV", KeyboardLayer.NAVIGATION)
            )
        )
        KeyboardLayer.NAVIGATION -> listOf(
            listOf(
                k("ESC", KeyEvent.KEYCODE_ESCAPE),
                k("TAB", KeyEvent.KEYCODE_TAB),
                k("HOME", KeyEvent.KEYCODE_MOVE_HOME),
                k("END", KeyEvent.KEYCODE_MOVE_END),
                k("PGUP", KeyEvent.KEYCODE_PAGE_UP),
                k("PGDN", KeyEvent.KEYCODE_PAGE_DOWN)
            ),
            listOf(
                k("INS", KeyEvent.KEYCODE_INSERT),
                k("DEL", KeyEvent.KEYCODE_FORWARD_DEL),
                k("←", KeyEvent.KEYCODE_DPAD_LEFT),
                k("↑", KeyEvent.KEYCODE_DPAD_UP),
                k("↓", KeyEvent.KEYCODE_DPAD_DOWN),
                k("→", KeyEvent.KEYCODE_DPAD_RIGHT)
            ),
            listOf(
                hotkey("^C", KeyEvent.KEYCODE_C),
                hotkey("^D", KeyEvent.KEYCODE_D),
                hotkey("^Z", KeyEvent.KEYCODE_Z),
                hotkey("^L", KeyEvent.KEYCODE_L),
                hotkey("^A", KeyEvent.KEYCODE_A),
                hotkey("^E", KeyEvent.KEYCODE_E)
            ),
            listOf(
                hotkey("^K", KeyEvent.KEYCODE_K),
                hotkey("^U", KeyEvent.KEYCODE_U),
                hotkey("^W", KeyEvent.KEYCODE_W),
                hotkey("^R", KeyEvent.KEYCODE_R),
                hotkey("^P", KeyEvent.KEYCODE_P),
                hotkey("^N", KeyEvent.KEYCODE_N)
            ),
            listOf(
                l("ABC", KeyboardLayer.LETTERS),
                l("SYM", KeyboardLayer.SYMBOLS),
                m("SHIFT", KeyboardModifier.SHIFT),
                t("SPACE", " ", 3.5f),
                k("⌫", KeyEvent.KEYCODE_DEL, 1.4f),
                k("↵", KeyEvent.KEYCODE_ENTER, 1.4f)
            )
        )
        KeyboardLayer.FUNCTIONS -> listOf(
            (1..4).map { k("F$it", KeyEvent.KEYCODE_F1 + it - 1) },
            (5..8).map { k("F$it", KeyEvent.KEYCODE_F1 + it - 1) },
            (9..12).map { k("F$it", KeyEvent.KEYCODE_F1 + it - 1) },
            listOf(
                k("CAPS", KeyEvent.KEYCODE_CAPS_LOCK),
                k("NUM", KeyEvent.KEYCODE_NUM_LOCK),
                k("SCRLK", KeyEvent.KEYCODE_SCROLL_LOCK),
                k("PAUSE", KeyEvent.KEYCODE_BREAK),
                k("PRTSC", KeyEvent.KEYCODE_SYSRQ),
                k("MENU", KeyEvent.KEYCODE_MENU)
            ),
            listOf(
                l("ABC", KeyboardLayer.LETTERS),
                l("SYM", KeyboardLayer.SYMBOLS),
                l("NAV", KeyboardLayer.NAVIGATION),
                m("META", KeyboardModifier.META),
                k("TAB", KeyEvent.KEYCODE_TAB),
                k("↵", KeyEvent.KEYCODE_ENTER)
            )
        )
    }
}
