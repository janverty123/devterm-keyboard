package com.oasan.devtermkeyboard

import android.view.KeyEvent

/** User-editable quick shortcuts. Each action has an original VectorDrawable icon. */
internal data class ToolbarShortcut(
    val id: String, val title: String, val icon: Int, val action: KeyAction
)

internal object ToolbarShortcuts {
    val items: List<ToolbarShortcut> = listOf(
        ToolbarShortcut("clipboard", "Paste", R.drawable.ic_kx_clipboard, KeyAction.Paste),
        ToolbarShortcut("emoji", "Emoji", R.drawable.ic_kx_emoji, KeyAction.Layer(KeyboardLayer.EMOJI)),
        ToolbarShortcut("symbols", "Symbols", R.drawable.ic_kx_symbols, KeyAction.Layer(KeyboardLayer.SYMBOLS)),
        ToolbarShortcut("navigation", "Text editing", R.drawable.ic_kx_edit, KeyAction.Layer(KeyboardLayer.NAVIGATION)),
        ToolbarShortcut("function", "Function keys", R.drawable.ic_kx_keyboard, KeyAction.Layer(KeyboardLayer.FUNCTIONS)),
        ToolbarShortcut("settings", "Settings", R.drawable.ic_kx_settings, KeyAction.Settings),
        ToolbarShortcut("mode", "Switch mode", R.drawable.ic_kx_mode, KeyAction.ModeToggle),
        ToolbarShortcut("keyboard", "Switch keyboard", R.drawable.ic_kx_globe, KeyAction.Picker),
        ToolbarShortcut("voice", "Voice", R.drawable.ic_kx_mic, KeyAction.Voice),
        ToolbarShortcut("copy", "Copy", R.drawable.ic_kx_clipboard_check, KeyAction.Copy)
    )
    val defaults = listOf("clipboard", "emoji")
    fun lookup(id: String): ToolbarShortcut = items.firstOrNull { it.id == id } ?: items.first()
}
