package com.oasan.devtermkeyboard

enum class KeyboardMode { STANDARD, DEVELOPER }
enum class KeyboardLayer { LETTERS, SYMBOLS, NAVIGATION, FUNCTIONS, EMOJI }
enum class KeyboardModifier { CTRL, ALT, META }

sealed class KeyAction {
    data class Text(val value: String) : KeyAction()
    data class Hardware(val code: Int) : KeyAction()
    data class Modifier(val modifier: KeyboardModifier) : KeyAction()
    data class Layer(val layer: KeyboardLayer) : KeyAction()
    data class Shortcut(val code: Int, val meta: Int) : KeyAction()
    object Shift : KeyAction()
    object Copy : KeyAction()
    object Paste : KeyAction()
    object Cut : KeyAction()
    object SelectAll : KeyAction()
    object Picker : KeyAction()
    object Voice : KeyAction()
    object ModeToggle : KeyAction()
    object Settings : KeyAction()
}

data class KeySpec(
    val label: String,
    val action: KeyAction,
    val weight: Float = 1f,
    val hint: String = label
)
