package com.oasan.devtermkeyboard

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
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
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.math.max

/**
 * Keyboard presentation/dispatch only. Layouts, preferences, predictions, emoji
 * and voice each have dedicated modules. No network access and no input history.
 */
class DeveloperKeyboardService : InputMethodService() {
    private var root: LinearLayout? = null
    private lateinit var palette: KeyboardPalette
    private var mode = KeyboardMode.DEVELOPER
    private var layer = KeyboardLayer.LETTERS
    private val modifiers = mutableSetOf<KeyboardModifier>()
    private val keyViews = mutableListOf<Pair<TextView, KeySpec>>()
    private var suggestionBar: LinearLayout? = null
    private var emojiGroup = 0
    private var oneShotShift = false
    private var capsLock = false
    private var autoCaps = false
    private var lastShiftTap = 0L
    private var safeBottomPx = -1
    private val composing = StringBuilder()
    private var previousWord = ""
    private var voiceTarget: String? = null
    private var pendingSpeech: String? = null
    private val preview by lazy { KeyPreview(this) }

    private val handler = Handler(Looper.getMainLooper())
    private var repeatTask: Runnable? = null
    private var repeatView: View? = null

    override fun onEvaluateFullscreenMode() = false

    override fun onCreateInputView(): View {
        palette = KeyboardPalette.forContext(this)
        mode = KeyboardPreferences.mode(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(palette.background)
            setPadding(dp(3), dp(3), dp(3), dp(46))
            clipChildren = false
            clipToPadding = false
        }
        root?.setOnApplyWindowInsetsListener { view, insets ->
            val navigation = if (Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            val bottom = max(dp(46), navigation + dp(8))
            if (bottom != safeBottomPx) {
                safeBottomPx = bottom
                view.setPadding(dp(3), dp(3), dp(3), bottom)
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
        // Do not commit the previous field's composition into a newly focused app.
        composing.setLength(0)
        mode = KeyboardPreferences.mode(this)
        palette = KeyboardPalette.forContext(this)
        layer = KeyboardLayer.LETTERS
        previousWord = ""
        oneShotShift = false
        capsLock = false
        modifiers.clear()
        updateAutoCaps()
        render()
        deliverPendingSpeech()
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
        preview.dismiss()
        finishComposition(false)
        modifiers.clear()
        oneShotShift = false
        capsLock = false
        super.onFinishInput()
    }

    override fun onDestroy() {
        clearRepeat()
        preview.dismiss()
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
        val previous = autoCaps
        updateAutoCaps()
        if (previous != autoCaps && !oneShotShift && !capsLock) updateLabels()
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

    private fun shouldCompose(): Boolean =
        mode == KeyboardMode.STANDARD &&
        (KeyboardPreferences.predictions(this) || KeyboardPreferences.autocorrect(this)) &&
        WordSuggestions.allowed(currentInputEditorInfo)

    private fun keyHeight(): Int {
        val config = resources.configuration
        val screen = config.screenHeightDp.takeIf { it > 0 } ?: 700
        val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
        val rows = if (mode == KeyboardMode.STANDARD) 4 else 5
        val fraction = if (isLandscape) 0.65f else if (mode == KeyboardMode.STANDARD) 0.48f else 0.55f
        val toolbarSpace = if (mode == KeyboardMode.DEVELOPER) 137f else 91f
        val calculated = ((screen * fraction - toolbarSpace) / rows).toInt().coerceIn(31, 49)
        return if (KeyboardPreferences.isCompact(this)) (calculated - 6).coerceAtLeast(26) else calculated
    }

    private fun render() {
        val panel = root ?: return
        clearRepeat()
        preview.dismiss()
        keyViews.clear()
        panel.removeAllViews()
        panel.setBackgroundColor(palette.background)
        suggestionBar = null
        panel.addView(makeHeader())

        if (mode == KeyboardMode.DEVELOPER && layer != KeyboardLayer.EMOJI) {
            val strip = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
            }
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            KeyLayouts.toolbar().forEach { spec ->
                val width = if (spec.label.length >= 4) dp(59) else dp(44)
                row.addView(createKey(spec, true), LinearLayout.LayoutParams(
                    width, dp(37)
                ).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
            }
            strip.addView(row)
            panel.addView(strip, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(41)
            ))
        }

        if (layer == KeyboardLayer.EMOJI) {
            renderEmoji(panel)
            return
        }

        val height = keyHeight()
        KeyLayouts.rows(mode, layer).forEachIndexed { index, specs ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                if (layer == KeyboardLayer.LETTERS &&
                    index == (if (mode == KeyboardMode.STANDARD) 1 else 2)
                ) setPadding(dp(12), 0, dp(12), 0)
            }
            specs.forEach { spec ->
                row.addView(createKey(spec, false), LinearLayout.LayoutParams(
                    0, dp(height), spec.weight
                ).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
            }
            panel.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(height + 4)
            ))
            if (ValueAnimator.areAnimatorsEnabled()) {
                row.alpha = 0.82f
                row.animate().alpha(1f).setDuration(95L).start()
            }
        }
    }

    private fun makeHeader(): LinearLayout {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(3), dp(2), dp(3), dp(3))
        }
        val pill = TextView(this).apply {
            text = if (mode == KeyboardMode.STANDARD) "Aa  Standard" else ">_  Dev"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(palette.foreground)
            contentDescription = "Switch between Standard and Developer keyboard modes"
            background = rounded(palette.auxiliary, 18)
            setOnClickListener { dispatch(KeyAction.ModeToggle) }
        }
        line.addView(pill, LinearLayout.LayoutParams(dp(100), dp(39)).apply {
            rightMargin = dp(4)
        })

        if (mode == KeyboardMode.STANDARD &&
            KeyboardPreferences.predictions(this) && WordSuggestions.allowed(currentInputEditorInfo)
        ) {
            suggestionBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            line.addView(suggestionBar, LinearLayout.LayoutParams(0, dp(39), 1f))
            updateSuggestions()
        } else {
            val title = TextView(this).apply {
                text = if (mode == KeyboardMode.DEVELOPER) "ESC   CTRL   ALT" else "Aa  •  English"
                textSize = 11f
                setTextColor(palette.secondary)
                gravity = Gravity.CENTER
            }
            line.addView(title, LinearLayout.LayoutParams(0, dp(39), 1f))
        }

        val mic = TextView(this).apply {
            text = "🎙"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(palette.accentText)
            background = rounded(palette.selected, 18)
            contentDescription = "Start voice typing"
            setOnClickListener { dispatch(KeyAction.Voice) }
        }
        line.addView(mic, LinearLayout.LayoutParams(dp(41), dp(40)).apply {
            leftMargin = dp(3)
        })

        val settings = TextView(this).apply {
            text = "⚙"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(palette.foreground)
            contentDescription = "Keyboard settings"
            setOnClickListener { dispatch(KeyAction.Settings) }
        }
        line.addView(settings, LinearLayout.LayoutParams(dp(34), dp(40)))
        return line
    }

    private fun updateSuggestions() {
        val container = suggestionBar ?: return
        container.removeAllViews()
        val options = WordSuggestions.suggest(composing.toString(), previousWord)
        options.take(3).forEach { word ->
            val cell = TextView(this).apply {
                text = word
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                setTextColor(palette.foreground)
                textSize = 13f
                setPadding(dp(3), 0, dp(3), 0)
                contentDescription = "Insert suggestion $word"
                setOnClickListener {
                    val input = currentInputConnection ?: return@setOnClickListener
                    if (composing.isNotEmpty()) {
                        input.commitText(word + " ", 1)
                        composing.setLength(0)
                    } else {
                        input.commitText(word + " ", 1)
                    }
                    previousWord = word
                    updateSuggestions()
                }
            }
            container.addView(cell, LinearLayout.LayoutParams(0, dp(38), 1f))
        }
    }

    private fun renderEmoji(panel: LinearLayout) {
        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val categoryRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        EmojiCatalog.groups.forEachIndexed { index, group ->
            val button = TextView(this).apply {
                text = group.icon
                textSize = 23f
                gravity = Gravity.CENTER
                background = rounded(
                    if (index == emojiGroup) palette.selected else palette.auxiliary, 12
                )
                contentDescription = group.name + " emojis"
                setOnClickListener { emojiGroup = index; render() }
            }
            categoryRow.addView(button, LinearLayout.LayoutParams(
                dp(51), dp(40)
            ).apply { setMargins(dp(3), dp(2), dp(3), dp(2)) })
        }
        categoryScroll.addView(categoryRow)
        panel.addView(categoryScroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
        ))

        val scroller = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }
        val cells = GridLayout(this).apply { columnCount = 8 }
        val itemWidth = ((resources.displayMetrics.widthPixels - dp(20)) / 8).coerceAtLeast(dp(33))
        EmojiCatalog.groups[emojiGroup.coerceIn(0, EmojiCatalog.groups.lastIndex)].emoji
            .forEach { character ->
                val item = TextView(this).apply {
                    text = character
                    textSize = 24f
                    gravity = Gravity.CENTER
                    contentDescription = "Insert $character emoji"
                    isClickable = true
                    setOnClickListener {
                        currentInputConnection?.commitText(character, 1)
                    }
                }
                cells.addView(item, GridLayout.LayoutParams().apply {
                    width = itemWidth
                    height = dp(42)
                    setMargins(dp(1), dp(2), dp(1), dp(2))
                })
            }
        scroller.addView(cells)
        panel.addView(scroller, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(keyHeight() * (if (mode == KeyboardMode.STANDARD) 3 else 4) + 12)
        ))

        val footer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            KeySpec("ABC", KeyAction.Layer(KeyboardLayer.LETTERS), 1.5f),
            KeySpec("space", KeyAction.Text(" "), 4f),
            KeySpec("⌫", KeyAction.Hardware(KeyEvent.KEYCODE_DEL), 1f),
            KeySpec("↵", KeyAction.Hardware(KeyEvent.KEYCODE_ENTER), 1.5f)
        ).forEach { spec ->
            footer.addView(createKey(spec, false), LinearLayout.LayoutParams(
                0, dp(keyHeight()), spec.weight
            ).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        }
        panel.addView(footer)
    }

    private fun createKey(spec: KeySpec, utility: Boolean): TextView {
        val view = TextView(this).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            typeface = if (spec.action is KeyAction.Text) Typeface.DEFAULT
                else Typeface.create("sans-serif-medium", Typeface.BOLD)
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            includeFontPadding = false
            setPadding(dp(1), 0, dp(1), 0)
            isClickable = true
            isFocusable = false
            isLongClickable = true
            contentDescription = spec.hint
            setTextSize(TypedValue.COMPLEX_UNIT_SP,
                if (utility) 11.5f else when {
                    spec.action is KeyAction.Text && spec.label.length == 1 -> 20f
                    spec.label.length >= 6 -> 11f
                    spec.label.length >= 4 -> 12f
                    else -> 15f
                }
            )
        }
        keyViews.add(view to spec)
        paint(view, spec)
        view.setOnClickListener {
            if (KeyboardPreferences.hasHaptics(this)) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            dispatch(spec.action)
        }
        when (val a = spec.action) {
            KeyAction.Shift -> view.setOnLongClickListener {
                capsLock = !capsLock
                oneShotShift = false
                lastShiftTap = 0L
                updateLabels()
                true
            }
            is KeyAction.Text -> if (a.value == " ") {
                view.setOnLongClickListener { chooseKeyboard(); true }
            }
            is KeyAction.Hardware -> if (a.code in listOf(
                KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN
            )) view.setOnLongClickListener { startRepeat(view, a); true }
            else -> Unit
        }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    flash(v, spec, true)
                    if (spec.action is KeyAction.Text) {
                        val raw = spec.action.value
                        val shown = if (raw.length == 1 && raw[0].isLetter() &&
                            shouldUppercase()) raw.uppercase(Locale.ROOT) else raw
                        preview.show(v, shown, KeyboardPreferences.dark(this),
                            currentInputEditorInfo?.inputType ?: 0)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    preview.dismiss()
                    flash(v, spec, false)
                    if (v === repeatView) clearRepeat()
                }
            }
            false
        }
        return view
    }

    private fun keyColor(spec: KeySpec): Int {
        val active = when (val a = spec.action) {
            KeyAction.Shift -> capsLock || oneShotShift
            is KeyAction.Modifier -> a.modifier in modifiers
            is KeyAction.Layer -> a.layer == layer
            else -> false
        }
        return when {
            active -> palette.selected
            spec.action !is KeyAction.Text -> palette.auxiliary
            else -> palette.key
        }
    }

    private fun paint(view: TextView, spec: KeySpec) {
        val color = keyColor(spec)
        val active = color == palette.selected
        view.text = when {
            spec.action === KeyAction.Shift -> "Shift"
            spec.action is KeyAction.Text && spec.action.value.length == 1 &&
                spec.action.value[0].isLetter() && shouldUppercase() ->
                spec.label.uppercase(Locale.ROOT)
            else -> spec.label
        }
        view.setTextColor(if (active) palette.accentText else palette.foreground)
        view.background = rounded(color, 7)
        view.elevation = dp(1).toFloat()
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun updateLabels() {
        keyViews.forEach { (view, spec) -> paint(view, spec) }
    }

    /** Solid accent flash: no expanding ripple, no big pop-up animations. */
    private fun flash(v: View, spec: KeySpec, down: Boolean) {
        val drawable = v.background as? GradientDrawable ?: return
        val start = if (down) keyColor(spec) else palette.selected
        val end = if (down) palette.selected else keyColor(spec)
        val animation = ValueAnimator.ofArgb(start, end)
        animation.duration = if (down) 55L else 110L
        animation.addUpdateListener { drawable.setColor(it.animatedValue as Int) }
        animation.start()
    }

    private fun shouldUppercase() = capsLock || oneShotShift || autoCaps

    private fun dispatch(a: KeyAction) {
        when (a) {
            is KeyAction.Layer -> {
                finishComposition(false)
                layer = a.layer
                render()
            }
            is KeyAction.Modifier -> {
                if (!modifiers.add(a.modifier)) modifiers.remove(a.modifier)
                updateLabels()
            }
            KeyAction.Shift -> {
                val now = SystemClock.uptimeMillis()
                if (!capsLock && oneShotShift && now - lastShiftTap < 400L) {
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
                typeText(a.value)
                consumeOneShot()
            }
            is KeyAction.Hardware -> {
                sendHardware(a.code)
            }
            is KeyAction.Shortcut -> {
                finishComposition(false)
                KeyEventSender.sendKey(currentInputConnection, a.code, modifierMeta() or a.meta)
                consumeOneShot()
            }
            KeyAction.Copy -> {
                currentInputConnection?.performContextMenuAction(android.R.id.copy)
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
            KeyAction.Voice -> startVoice()
            KeyAction.Settings -> {
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            KeyAction.ModeToggle -> {
                finishComposition(false)
                mode = if (mode == KeyboardMode.STANDARD) KeyboardMode.DEVELOPER
                    else KeyboardMode.STANDARD
                KeyboardPreferences.setMode(this, mode)
                layer = KeyboardLayer.LETTERS
                oneShotShift = false
                modifiers.clear()
                render()
            }
        }
    }

    private fun typeText(raw: String) {
        val connection = currentInputConnection ?: return
        val value = if (raw.length == 1 && raw[0].isLetter() && shouldUppercase()) {
            raw.uppercase(Locale.ROOT)
        } else raw

        if (shouldCompose()) {
            if (value.length == 1 && (value[0].isLetter() ||
                (value[0] == '\'' && composing.isNotEmpty()))
            ) {
                composing.append(value)
                if (!connection.setComposingText(composing.toString(), 1)) {
                    composing.setLength(0)
                    connection.commitText(value, 1)
                }
                updateSuggestions()
                return
            }
            if (composing.isNotEmpty()) {
                finishComposition(raw == " ")
                if (raw == " ") return
            }
        }
        KeyEventSender.type(connection, currentInputEditorInfo, value, modifierMeta())
        if (value == " ") previousWord = ""
        updateSuggestions()
    }

    private fun finishComposition(isSpace: Boolean) {
        if (composing.isEmpty()) return
        val original = composing.toString()
        val corrected = if (isSpace && KeyboardPreferences.autocorrect(this) &&
            mode == KeyboardMode.STANDARD) WordSuggestions.correct(original) else null
        val result = corrected ?: original
        // This replaces Android's composing span rather than inserting twice.
        currentInputConnection?.commitText(result + if (isSpace) " " else "", 1)
        composing.setLength(0)
        previousWord = result
        updateSuggestions()
    }

    private fun sendHardware(code: Int) {
        val connection = currentInputConnection ?: return
        if (code == KeyEvent.KEYCODE_DEL && composing.isNotEmpty()) {
            composing.deleteCharAt(composing.lastIndex)
            if (composing.isEmpty()) connection.setComposingText("", 1)
            else connection.setComposingText(composing.toString(), 1)
            updateSuggestions()
            consumeOneShot()
            return
        }
        finishComposition(false)
        val type = currentInputEditorInfo?.inputType ?: InputType.TYPE_NULL
        if (code == KeyEvent.KEYCODE_DEL && type != InputType.TYPE_NULL &&
            modifierMeta() == 0 && !oneShotShift
        ) {
            connection.deleteSurroundingTextInCodePoints(1, 0)
        } else if (code == KeyEvent.KEYCODE_ENTER && type != InputType.TYPE_NULL &&
            modifierMeta() == 0 && !oneShotShift &&
            (currentInputEditorInfo?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION !=
            EditorInfo.IME_ACTION_NONE
        ) {
            if (!sendDefaultEditorAction(true)) KeyEventSender.sendKey(connection, code)
        } else {
            KeyEventSender.sendKey(connection, code, modifierMeta())
        }
        consumeOneShot()
    }

    private fun modifierMeta(): Int {
        var state = 0
        if (KeyboardModifier.CTRL in modifiers) state = state or
            KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (KeyboardModifier.ALT in modifiers) state = state or
            KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (KeyboardModifier.META in modifiers) state = state or
            KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        if (oneShotShift) state = state or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        return state
    }

    private fun consumeOneShot() {
        val had = oneShotShift || modifiers.isNotEmpty()
        oneShotShift = false
        modifiers.clear()
        updateAutoCaps()
        if (had) updateLabels()
    }

    private fun pasteClipboard() {
        val connection = currentInputConnection ?: return
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = manager.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0) ?: return
        val value = item.coerceToText(this) ?: return
        if (value.isEmpty()) return
        finishComposition(false)
        // Termux accepts committed text more consistently than edit-menu actions.
        // Avoid automatically pressing Enter, especially with shell commands.
        if (!connection.commitText(value, 1)) {
            connection.performContextMenuAction(android.R.id.paste)
        }
    }

    private fun chooseKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showInputMethodPicker()
    }

    private fun startVoice() {
        val variation = (currentInputEditorInfo?.inputType ?: 0) and
            InputType.TYPE_MASK_VARIATION
        if (variation in setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_NUMBER_VARIATION_PASSWORD
        )) return
        voiceTarget = currentInputEditorInfo?.packageName
        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode != android.app.Activity.RESULT_OK) return
                val spoken = resultData?.getString("spoken_text")?.trim().orEmpty()
                if (spoken.isEmpty()) return
                if (currentInputConnection != null &&
                    currentInputEditorInfo?.packageName == voiceTarget
                ) {
                    finishComposition(false)
                    currentInputConnection?.commitText(spoken, 1)
                } else {
                    pendingSpeech = spoken
                }
            }
        }
        startActivity(Intent(this, VoiceRecognitionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("result_receiver", receiver)
        })
    }

    private fun deliverPendingSpeech() {
        val text = pendingSpeech ?: return
        if (currentInputEditorInfo?.packageName != voiceTarget) {
            pendingSpeech = null
            return
        }
        if (currentInputConnection?.commitText(text, 1) == true) pendingSpeech = null
    }

    private fun startRepeat(view: View, key: KeyAction.Hardware) {
        clearRepeat()
        repeatView = view
        val runnable = object : Runnable {
            override fun run() {
                if (!view.isPressed) {
                    clearRepeat()
                    return
                }
                sendHardware(key.code)
                handler.postDelayed(this, 75L)
            }
        }
        repeatTask = runnable
        handler.post(runnable)
    }

    private fun clearRepeat() {
        repeatTask?.let { handler.removeCallbacks(it) }
        repeatTask = null
        repeatView = null
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
