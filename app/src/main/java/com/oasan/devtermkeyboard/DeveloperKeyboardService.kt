package com.oasan.devtermkeyboard

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.max

/**
 * Lightweight Android IME. The input view is inset above the Android navigation
 * area, and rows are sized against the available screen height.
 * No networking, analytics or keyboard/clipboard history.
 */
class DeveloperKeyboardService : InputMethodService() {
    private var root: LinearLayout? = null
    private var toolScroll: HorizontalScrollView? = null
    private lateinit var palette: KeyboardPalette
    private var layer = KeyboardLayer.LETTERS
    private val modifiers = mutableSetOf<KeyboardModifier>()
    private val keyViews = mutableListOf<Pair<TextView, KeySpec>>()

    private var oneShotShift = false
    private var capsLock = false
    private var autoCaps = false
    private var lastShiftTap = 0L
    private var safeBottomPx = -1

    private val handler = Handler(Looper.getMainLooper())
    private var repeatTask: Runnable? = null
    private var repeatView: View? = null

    override fun onEvaluateFullscreenMode() = false

    override fun onCreateInputView(): View {
        palette = KeyboardPalette.forContext(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(palette.background)
            setPadding(dp(3), dp(3), dp(3), dp(36))
            clipChildren = false
            clipToPadding = false
        }
        // Android 15+ draws behind system bars. Honor the actual navigation
        // inset, while reserving a small fallback region on OEM devices that
        // report zero insets despite drawing a keyboard-switcher overlay.
        root?.setOnApplyWindowInsetsListener { v, insets ->
            val navigation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            val bottom = max(dp(36), navigation + dp(7))
            if (bottom != safeBottomPx) {
                safeBottomPx = bottom
                v.setPadding(dp(3), dp(3), dp(3), bottom)
            }
            insets
        }
        window?.window?.navigationBarColor = palette.background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.window?.isNavigationBarContrastEnforced = false
        }
        render()
        root?.requestApplyInsets()
        return requireNotNull(root)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        palette = KeyboardPalette.forContext(this)
        layer = KeyboardLayer.LETTERS
        clearRepeat()
        oneShotShift = false
        capsLock = false
        modifiers.clear()
        updateAutoCaps()
        root?.setBackgroundColor(palette.background)
        window?.window?.navigationBarColor = palette.background
        render()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (root != null) {
            palette = KeyboardPalette.forContext(this)
            render()
        }
    }

    override fun onFinishInput() {
        clearRepeat()
        modifiers.clear()
        oneShotShift = false
        capsLock = false
        super.onFinishInput()
    }

    override fun onDestroy() {
        clearRepeat()
        root = null
        super.onDestroy()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd
        )
        val wasAutoCaps = autoCaps
        updateAutoCaps()
        if (autoCaps != wasAutoCaps && !capsLock && !oneShotShift) updateLabels()
    }

    private fun updateAutoCaps() {
        val type = currentInputEditorInfo?.inputType ?: 0
        val flags = type and (
            InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or
                InputType.TYPE_TEXT_FLAG_CAP_WORDS or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            )
        autoCaps = (type and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
            flags != 0 && (currentInputConnection?.getCursorCapsMode(flags) ?: 0) != 0
    }

    /** Gboard-style compact layout: toolbar plus five rows, even in landscape. */
    private fun keyHeight(): Int {
        val config = resources.configuration
        val screen = config.screenHeightDp.takeIf { it > 0 } ?: 700
        val fraction = if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.55f else 0.40f
        val calculated = ((screen * fraction - 98f) / 5f).toInt().coerceIn(28, 46)
        return if (KeyboardPreferences.isCompact(this)) (calculated - 6).coerceAtLeast(25) else calculated
    }

    private fun render() {
        val panel = root ?: return
        clearRepeat()
        keyViews.clear()
        panel.removeAllViews()
        panel.setBackgroundColor(palette.background)

        val toolbar = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        toolScroll = toolbar
        val toolRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        KeyLayouts.toolbar().forEach { spec ->
            val width = when (spec.action) {
                KeyAction.Copy, KeyAction.Paste -> dp(51)
                else -> dp(44)
            }
            toolRow.addView(createKey(spec, true), LinearLayout.LayoutParams(
                width, dp(38)
            ).apply { setMargins(dp(2), dp(1), dp(2), dp(3)) })
        }
        toolbar.addView(toolRow)
        panel.addView(toolbar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(43)
        ))

        val height = keyHeight()
        KeyLayouts.rows(layer).forEachIndexed { index, specs ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                if (layer == KeyboardLayer.LETTERS && index == 2) {
                    setPadding(dp(12), 0, dp(12), 0)
                }
            }
            specs.forEach { spec ->
                row.addView(createKey(spec, false), LinearLayout.LayoutParams(
                    0, dp(height), spec.weight
                ).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
            }
            panel.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(height + 4)
            ))
        }
    }

    private fun createKey(spec: KeySpec, toolbar: Boolean): TextView {
        val view = TextView(this).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                if (spec.action is KeyAction.Text) android.graphics.Typeface.NORMAL
                else android.graphics.Typeface.BOLD
            )
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            includeFontPadding = false
            setPadding(dp(1), 0, dp(1), 0)
            isClickable = true
            isFocusable = false
            isLongClickable = true
            contentDescription = spec.hint
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (toolbar) 11f else when {
                spec.action is KeyAction.Text && spec.label.length == 1 -> 18f
                spec.label.length >= 7 -> 10.5f
                spec.label.length >= 4 -> 11f
                else -> 13f
            })
        }
        keyViews.add(view to spec)
        paint(view, spec)
        view.setOnClickListener {
            if (KeyboardPreferences.hasHaptics(this)) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            dispatch(spec.action)
        }
        when (val action = spec.action) {
            KeyAction.Shift -> view.setOnLongClickListener {
                capsLock = !capsLock
                oneShotShift = false
                lastShiftTap = 0L
                updateLabels()
                true
            }
            is KeyAction.Text -> if (action.value == " ") {
                view.contentDescription = "Space, hold to change keyboard"
                view.setOnLongClickListener {
                    chooseKeyboard()
                    true
                }
            }
            is KeyAction.Hardware -> if (isRepeatable(action.code)) {
                view.setOnLongClickListener {
                    startRepeat(view, action)
                    true
                }
            }
            else -> Unit
        }
        view.setOnTouchListener { target, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> animateKey(target, true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    animateKey(target, false)
                    if (target === repeatView) clearRepeat()
                }
            }
            false // preserves framework click and long-click accessibility behavior
        }
        return view
    }

    private fun paint(view: TextView, spec: KeySpec) {
        val selected = when (val a = spec.action) {
            KeyAction.Shift -> capsLock || oneShotShift
            is KeyAction.Modifier -> a.modifier in modifiers
            is KeyAction.Layer -> a.layer == layer
            else -> false
        }
        val auxiliary = spec.action !is KeyAction.Text
        val color = when {
            selected -> palette.selected
            auxiliary -> palette.auxiliary
            else -> palette.key
        }
        val label = when {
            spec.action === KeyAction.Shift && capsLock -> "⇪"
            spec.action === KeyAction.Shift -> "⇧"
            spec.action is KeyAction.Text && spec.action.value.length == 1 &&
                spec.action.value[0].isLetter() && shouldUppercase() ->
                spec.label.uppercase(Locale.ROOT)
            else -> spec.label
        }
        view.text = label
        view.setTextColor(if (selected) palette.accentText else palette.foreground)
        val surface = GradientDrawable().apply {
            cornerRadius = dp(6).toFloat()
            setColor(color)
        }
        view.background = RippleDrawable(
            ColorStateList.valueOf((palette.selected and 0x00FFFFFF) or 0x44000000),
            surface, null
        )
        view.elevation = dp(1).toFloat()
    }

    private fun updateLabels() {
        keyViews.forEach { (v, spec) -> paint(v, spec) }
    }

    private fun animateKey(view: View, down: Boolean) {
        if (!ValueAnimator.areAnimatorsEnabled()) return
        view.animate().cancel()
        view.animate()
            .scaleX(if (down) 0.95f else 1f)
            .scaleY(if (down) 0.93f else 1f)
            .setDuration(if (down) 65L else 105L)
            .start()
    }

    private fun shouldUppercase() = capsLock || oneShotShift || autoCaps

    private fun dispatch(action: KeyAction) {
        when (action) {
            is KeyAction.Layer -> {
                layer = action.layer
                render()
            }
            is KeyAction.Modifier -> {
                if (!modifiers.add(action.modifier)) modifiers.remove(action.modifier)
                updateLabels()
            }
            KeyAction.Shift -> {
                val now = SystemClock.uptimeMillis()
                if (!capsLock && oneShotShift && now - lastShiftTap < 400) {
                    capsLock = true
                    oneShotShift = false
                } else if (capsLock) {
                    capsLock = false
                    oneShotShift = false
                } else {
                    oneShotShift = !oneShotShift
                }
                lastShiftTap = now
                updateLabels()
            }
            is KeyAction.Text -> {
                val text = if (action.value.length == 1 &&
                    action.value[0].isLetter() && shouldUppercase()
                ) action.value.uppercase(Locale.ROOT) else action.value
                val shift = oneShotShift && text.any { it.isLetter() }
                KeyEventSender.type(
                    currentInputConnection, currentInputEditorInfo,
                    text, modifierMeta(includeShift = shift)
                )
                consumeOneShot()
            }
            is KeyAction.Hardware -> {
                sendHardware(action.code)
            }
            is KeyAction.Shortcut -> {
                KeyEventSender.sendKey(
                    currentInputConnection, action.code, modifierMeta() or action.meta
                )
                consumeOneShot()
            }
            KeyAction.Copy -> {
                copySelection()
                consumeOneShot()
            }
            KeyAction.Paste -> {
                pasteClipboard()
                consumeOneShot()
            }
            KeyAction.Cut -> {
                currentInputConnection?.performContextMenuAction(android.R.id.cut)
                consumeOneShot()
            }
            KeyAction.SelectAll -> {
                currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
                consumeOneShot()
            }
            KeyAction.Picker -> chooseKeyboard()
        }
    }

    private fun sendHardware(code: Int) {
        val connection = currentInputConnection ?: return
        val type = currentInputEditorInfo?.inputType ?: InputType.TYPE_NULL
        if (code == KeyEvent.KEYCODE_DEL && type != InputType.TYPE_NULL &&
            modifierMeta() == 0 && !oneShotShift
        ) {
            connection.deleteSurroundingTextInCodePoints(1, 0)
        } else if (code == KeyEvent.KEYCODE_ENTER && type != InputType.TYPE_NULL &&
            modifierMeta() == 0 && !oneShotShift &&
            currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_NONE
        ) {
            if (!sendDefaultEditorAction(true)) {
                KeyEventSender.sendKey(connection, code)
            }
        } else {
            KeyEventSender.sendKey(connection, code, modifierMeta(includeShift = oneShotShift))
        }
        consumeOneShot()
    }

    private fun modifierMeta(includeShift: Boolean = oneShotShift): Int {
        var state = 0
        if (KeyboardModifier.CTRL in modifiers) {
            state = state or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        }
        if (KeyboardModifier.ALT in modifiers) {
            state = state or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        }
        if (KeyboardModifier.META in modifiers) {
            state = state or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        }
        if (includeShift) {
            state = state or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        }
        return state
    }

    private fun consumeOneShot() {
        val hadModifiers = oneShotShift || modifiers.isNotEmpty()
        val oldAutoCaps = autoCaps
        oneShotShift = false
        modifiers.clear()
        updateAutoCaps()
        if (hadModifiers || oldAutoCaps != autoCaps) updateLabels()
    }

    private fun clipboard() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private fun copySelection() {
        val connection = currentInputConnection ?: return
        if (connection.performContextMenuAction(android.R.id.copy)) return
        val selected = connection.getSelectedText(0)?.toString()
        if (!selected.isNullOrEmpty()) {
            clipboard().setPrimaryClip(ClipData.newPlainText("Selected text", selected))
        }
    }

    private fun pasteClipboard() {
        val connection = currentInputConnection ?: return
        if (connection.performContextMenuAction(android.R.id.paste)) return
        val clip = clipboard().primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this) ?: return
        connection.commitText(text, 1)
    }

    private fun chooseKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    private fun isRepeatable(code: Int) = code in listOf(
        KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL,
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN
    )

    private fun startRepeat(view: View, action: KeyAction.Hardware) {
        clearRepeat()
        repeatView = view
        val repeating = object : Runnable {
            override fun run() {
                if (!view.isPressed) {
                    clearRepeat()
                    return
                }
                sendHardware(action.code)
                // sendHardware resets one-shot state but repeat remains active.
                handler.postDelayed(this, 75L)
            }
        }
        repeatTask = repeating
        handler.post(repeating)
    }

    private fun clearRepeat() {
        repeatTask?.let { handler.removeCallbacks(it) }
        repeatTask = null
        repeatView = null
    }

    private fun dp(px: Int): Int =
        (px * resources.displayMetrics.density + 0.5f).toInt()
}
