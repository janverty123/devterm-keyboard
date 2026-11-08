package com.oasan.devtermkeyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.view.KeyEvent
import java.util.Locale

/** System-wide developer keyboard: ordinary text via commitText, terminal keys via events. */
class DeveloperKeyboardService : InputMethodService() {
    private var root: LinearLayout? = null
    private var currentLayer = KeyboardLayer.LETTERS
    private val activeModifiers = mutableSetOf<KeyboardModifier>()

    private val background = Color.rgb(16, 20, 28)
    private val keyBackground = Color.rgb(37, 46, 61)
    private val actionBackground = Color.rgb(43, 61, 79)
    private val selectedBackground = Color.rgb(37, 134, 124)
    private val foreground = Color.rgb(241, 246, 250)
    private val secondary = Color.rgb(167, 187, 205)

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
            setPadding(dp(3), dp(5), dp(3), dp(7))
        }
        render()
        return requireNotNull(root)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentLayer = KeyboardLayer.LETTERS
        activeModifiers.clear()
        render()
    }

    override fun onFinishInput() {
        activeModifiers.clear()
        super.onFinishInput()
    }

    private fun render() {
        val panel = root ?: return
        panel.removeAllViews()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), dp(3))
        }
        header.addView(TextView(this).apply {
            text = "⌨ DEVTERM / ${currentLayer.name}"
            setTextColor(secondary)
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }, LinearLayout.LayoutParams(0, dp(22), 1f))
        header.addView(TextView(this).apply {
            text = "MODIFIERS: next key"
            setTextColor(secondary)
            textSize = 9f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        })
        panel.addView(header)

        val strip = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setFillViewport(true)
        }
        val shortcuts = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        KeyLayouts.utilityRow().forEach { spec ->
            val button = makeKey(spec)
            shortcuts.addView(button, LinearLayout.LayoutParams(dp(50), dp(44)).apply {
                setMargins(dp(2), dp(1), dp(2), dp(3))
            })
        }
        strip.addView(shortcuts)
        panel.addView(strip)

        KeyLayouts.rows(currentLayer).forEach { row ->
            val keyboardRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.forEach { spec ->
                keyboardRow.addView(makeKey(spec), LinearLayout.LayoutParams(
                    0, dp(48), spec.weight
                ).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                })
            }
            panel.addView(keyboardRow, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun makeKey(spec: KeySpec): Button {
        val selected = when (val action = spec.action) {
            is KeyAction.Modifier -> action.modifier in activeModifiers
            is KeyAction.Layer -> action.layer == currentLayer
            else -> false
        }
        val isAction = spec.action !is KeyAction.Text
        val label = if (currentLayer == KeyboardLayer.LETTERS &&
            KeyboardModifier.SHIFT in activeModifiers &&
            spec.action is KeyAction.Text && spec.label.length == 1
        ) spec.label.uppercase(Locale.ROOT) else spec.label

        return Button(this).apply {
            text = label
            contentDescription = spec.accessibilityLabel
            setAllCaps(false)
            textSize = when {
                label.length >= 5 -> 10f
                label.length >= 3 -> 12f
                else -> 15f
            }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(foreground)
            isSoundEffectsEnabled = false
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            setPadding(0, 0, 0, 0)
            gravity = Gravity.CENTER
            elevation = dp(1).toFloat()
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(when {
                    selected -> selectedBackground
                    isAction -> actionBackground
                    else -> keyBackground
                })
            }
            setOnClickListener { handleKey(spec.action) }
        }
    }

    private fun handleKey(action: KeyAction) {
        when (action) {
            is KeyAction.Modifier -> {
                if (!activeModifiers.add(action.modifier)) activeModifiers.remove(action.modifier)
                render()
            }
            is KeyAction.Layer -> {
                currentLayer = action.layer
                render()
            }
            is KeyAction.Text -> {
                val shift = KeyboardModifier.SHIFT in activeModifiers
                val output = if (shift && action.value.length == 1 &&
                    action.value[0].isLetter()
                ) action.value.uppercase(Locale.ROOT) else action.value
                KeyEventSender.type(currentInputConnection, currentInputEditorInfo, output, modifiers())
                finishOneShotModifiers()
            }
            is KeyAction.Hardware -> {
                KeyEventSender.sendKey(currentInputConnection, action.keyCode, modifiers())
                finishOneShotModifiers()
            }
            is KeyAction.Shortcut -> {
                KeyEventSender.sendKey(currentInputConnection, action.keyCode, modifiers() or action.metaState)
                finishOneShotModifiers()
            }
        }
    }

    private fun finishOneShotModifiers() {
        if (activeModifiers.isNotEmpty()) {
            activeModifiers.clear()
            render()
        }
    }

    private fun modifiers(): Int {
        var meta = 0
        if (KeyboardModifier.SHIFT in activeModifiers) {
            meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        }
        if (KeyboardModifier.CTRL in activeModifiers) {
            meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        }
        if (KeyboardModifier.ALT in activeModifiers) {
            meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        }
        if (KeyboardModifier.META in activeModifiers) {
            meta = meta or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        }
        return meta
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
