package com.oasan.devtermkeyboard

enum class KeyboardLayer { LETTERS, SYMBOLS, NAVIGATION, FUNCTIONS }
enum class KeyboardModifier { SHIFT, CTRL, ALT, META }

sealed class KeyAction {
    data class Text(val value: String) : KeyAction()
    data class Hardware(val keyCode: Int) : KeyAction()
    data class Modifier(val modifier: KeyboardModifier) : KeyAction()
    data class Layer(val layer: KeyboardLayer) : KeyAction()
    data class Shortcut(val keyCode: Int, val metaState: Int) : KeyAction()
}
data class KeySpec(
    val label: String,
    val action: KeyAction,
    val weight: Float = 1f,
    val accessibilityLabel: String = label
)
