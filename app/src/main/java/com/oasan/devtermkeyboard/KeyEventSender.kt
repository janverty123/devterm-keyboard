package com.oasan.devtermkeyboard

import android.os.SystemClock
import android.text.InputType
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/**
 * Android IMEs normally commit text. For TYPE_NULL terminal editors we prefer
 * hardware-style key events. This also supports Ctrl/Alt combinations.
 * Recipient applications ultimately decide how to interpret the events.
 */
internal object KeyEventSender {
    private data class Mapping(val keyCode: Int, val needsShift: Boolean = false)

    fun type(
        connection: InputConnection?,
        editorInfo: EditorInfo?,
        text: String,
        metaState: Int
    ) {
        if (connection == null) return
        val rawEditor = editorInfo?.inputType == InputType.TYPE_NULL
        val withModifier = (metaState and (
            KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON or KeyEvent.META_META_ON
        )) != 0
        val char = text.singleOrNull()
        val mapping = char?.let(::characterToKey)
        if (mapping != null && (rawEditor || withModifier)) {
            val charShift = if (mapping.needsShift) {
                KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
            } else 0
            sendKey(connection, mapping.keyCode, metaState or charShift)
        } else {
            connection.commitText(text, 1)
        }
    }

    fun sendKey(connection: InputConnection?, keyCode: Int, metaState: Int = 0) {
        if (connection == null) return
        val time = SystemClock.uptimeMillis()
        val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        val down = KeyEvent(
            time, time, KeyEvent.ACTION_DOWN, keyCode, 0, metaState,
            KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags, InputDevice.SOURCE_KEYBOARD
        )
        val up = KeyEvent(
            time, time, KeyEvent.ACTION_UP, keyCode, 0, metaState,
            KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags, InputDevice.SOURCE_KEYBOARD
        )
        connection.sendKeyEvent(down)
        connection.sendKeyEvent(up)
    }

    private fun characterToKey(char: Char): Mapping? {
        if (char in 'a'..'z') return Mapping(KeyEvent.KEYCODE_A + char.code - 'a'.code)
        if (char in 'A'..'Z') return Mapping(KeyEvent.KEYCODE_A + char.code - 'A'.code, true)
        if (char in '0'..'9') return Mapping(KeyEvent.KEYCODE_0 + char.code - '0'.code)
        return when (char) {
            ' ' -> Mapping(KeyEvent.KEYCODE_SPACE)
            '-' -> Mapping(KeyEvent.KEYCODE_MINUS)
            '=' -> Mapping(KeyEvent.KEYCODE_EQUALS)
            '[' -> Mapping(KeyEvent.KEYCODE_LEFT_BRACKET)
            ']' -> Mapping(KeyEvent.KEYCODE_RIGHT_BRACKET)
            '\\' -> Mapping(KeyEvent.KEYCODE_BACKSLASH)
            ';' -> Mapping(KeyEvent.KEYCODE_SEMICOLON)
            '\'' -> Mapping(KeyEvent.KEYCODE_APOSTROPHE)
            ',' -> Mapping(KeyEvent.KEYCODE_COMMA)
            '.' -> Mapping(KeyEvent.KEYCODE_PERIOD)
            '/' -> Mapping(KeyEvent.KEYCODE_SLASH)
            '`' -> Mapping(KeyEvent.KEYCODE_GRAVE)
            '!' -> Mapping(KeyEvent.KEYCODE_1, true)
            '@' -> Mapping(KeyEvent.KEYCODE_2, true)
            '#' -> Mapping(KeyEvent.KEYCODE_3, true)
            '$' -> Mapping(KeyEvent.KEYCODE_4, true)
            '%' -> Mapping(KeyEvent.KEYCODE_5, true)
            '^' -> Mapping(KeyEvent.KEYCODE_6, true)
            '&' -> Mapping(KeyEvent.KEYCODE_7, true)
            '*' -> Mapping(KeyEvent.KEYCODE_8, true)
            '(' -> Mapping(KeyEvent.KEYCODE_9, true)
            ')' -> Mapping(KeyEvent.KEYCODE_0, true)
            '_' -> Mapping(KeyEvent.KEYCODE_MINUS, true)
            '+' -> Mapping(KeyEvent.KEYCODE_EQUALS, true)
            '{' -> Mapping(KeyEvent.KEYCODE_LEFT_BRACKET, true)
            '}' -> Mapping(KeyEvent.KEYCODE_RIGHT_BRACKET, true)
            '|' -> Mapping(KeyEvent.KEYCODE_BACKSLASH, true)
            ':' -> Mapping(KeyEvent.KEYCODE_SEMICOLON, true)
            '"' -> Mapping(KeyEvent.KEYCODE_APOSTROPHE, true)
            '<' -> Mapping(KeyEvent.KEYCODE_COMMA, true)
            '>' -> Mapping(KeyEvent.KEYCODE_PERIOD, true)
            '?' -> Mapping(KeyEvent.KEYCODE_SLASH, true)
            '~' -> Mapping(KeyEvent.KEYCODE_GRAVE, true)
            else -> null
        }
    }
}
