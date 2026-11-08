package com.oasan.devtermkeyboard

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
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
import android.widget.FrameLayout
import android.widget.ImageView
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
    private val recentEmoji = mutableListOf<String>()
    private var toolsOpen = false
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
            // No emoji or tool-panel content is allowed to draw over other rows.
            clipChildren = true
            clipToPadding = true
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
        toolsOpen = false
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
        panel.clipChildren = true
        panel.clipToPadding = true
        panel.setBackgroundColor(palette.background)
        suggestionBar = null
        panel.addView(makeHeader())
        if (toolsOpen) {
            renderToolsMenu(panel)
            return
        }

        if (mode == KeyboardMode.DEVELOPER && layer != KeyboardLayer.EMOJI) {
            val strip = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                clipChildren = true
                clipToPadding = true
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

    /** Gboard-inspired toolbar: tools menu, suggestions/quick actions, microphone. */
    private fun makeHeader(): LinearLayout {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(3))
        }
        val menuIcon = if (toolsOpen) R.drawable.ic_kx_back else R.drawable.ic_kx_tools
        line.addView(iconControl(menuIcon,
            if (toolsOpen) "Close tools menu" else "Keyboard tools",
            if (toolsOpen) palette.selected else palette.auxiliary
        ) {
            dispatch(KeyAction.Tools)
        }, LinearLayout.LayoutParams(dp(43), dp(40)).apply {
            rightMargin = dp(3)
        })

        if (!toolsOpen && mode == KeyboardMode.STANDARD &&
            KeyboardPreferences.predictions(this) &&
            WordSuggestions.allowed(currentInputEditorInfo)
        ) {
            suggestionBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            line.addView(suggestionBar, LinearLayout.LayoutParams(0, dp(40), 1f))
            updateSuggestions()
        } else {
            val quick = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            if (toolsOpen) {
                quick.addView(TextView(this).apply {
                    text = "Tools"
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(palette.foreground)
                    gravity = Gravity.CENTER_VERTICAL
                }, LinearLayout.LayoutParams(0, dp(40), 1f))
            } else {
                val modeTag = TextView(this).apply {
                    text = if (mode == KeyboardMode.DEVELOPER) "Dev" else "English"
                    textSize = 12f
                    setTextColor(palette.secondary)
                    gravity = Gravity.CENTER
                    contentDescription = "Keyboard mode: $text"
                }
                quick.addView(modeTag, LinearLayout.LayoutParams(0, dp(40), 1f))
                quick.addView(iconControl(R.drawable.ic_kx_clipboard, "Paste clipboard",
                    palette.background) { dispatch(KeyAction.Paste) },
                    LinearLayout.LayoutParams(dp(40), dp(40)))
                quick.addView(iconControl(R.drawable.ic_kx_emoji, "Open emoji keyboard",
                    palette.background) {
                    dispatch(KeyAction.Layer(KeyboardLayer.EMOJI))
                }, LinearLayout.LayoutParams(dp(40), dp(40)))
            }
            line.addView(quick, LinearLayout.LayoutParams(0, dp(40), 1f))
        }

        line.addView(iconControl(R.drawable.ic_kx_mic, "Voice typing",
            palette.background
        ) { dispatch(KeyAction.Voice) },
            LinearLayout.LayoutParams(dp(42), dp(40)).apply { leftMargin = dp(2) })
        return line
    }

    private fun iconControl(icon: Int, description: String, surface: Int,
                            onClick: () -> Unit): ImageView {
        return ImageView(this).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(palette.foreground)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(9), dp(9), dp(9), dp(9))
            background = rounded(surface, 14)
            contentDescription = description
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    /** Tool grid replaces the keys, never floats over them. */
    private fun renderToolsMenu(panel: LinearLayout) {
        data class Tool(val icon: Int, val name: String, val onTap: () -> Unit)
        val options = listOf(
            Tool(R.drawable.ic_kx_mode,
                if (mode == KeyboardMode.DEVELOPER) "Standard" else "Dev mode") {
                dispatch(KeyAction.ModeToggle)
            },
            Tool(R.drawable.ic_kx_emoji, "Emoji") {
                dispatch(KeyAction.Layer(KeyboardLayer.EMOJI))
            },
            Tool(R.drawable.ic_kx_clipboard, "Paste") {
                dispatch(KeyAction.Paste)
                toolsOpen = false
                render()
            },
            Tool(R.drawable.ic_kx_edit, "Text editing") {
                dispatch(KeyAction.Layer(KeyboardLayer.NAVIGATION))
            },
            Tool(R.drawable.ic_kx_palette, "Themes") {
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("open_section", "themes"))
            },
            Tool(R.drawable.ic_kx_settings, "Settings") {
                dispatch(KeyAction.Settings)
            },
            Tool(R.drawable.ic_kx_keyboard, "Switch input") {
                chooseKeyboard()
            },
            Tool(R.drawable.ic_kx_mic, "Voice input") {
                dispatch(KeyAction.Voice)
            },
            Tool(R.drawable.ic_kx_symbols, "Symbols") {
                dispatch(KeyAction.Layer(KeyboardLayer.SYMBOLS))
            }
        )
        val grid = GridLayout(this).apply {
            columnCount = 3
            clipChildren = true
            clipToPadding = true
            setPadding(dp(4), dp(7), dp(4), dp(4))
        }
        val height = if (resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE) 65 else 82
        options.forEachIndexed { index, tool ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                background = rounded(palette.auxiliary, 13)
                contentDescription = tool.name
                setOnClickListener { tool.onTap() }
            }
            cell.addView(ImageView(this).apply {
                setImageResource(tool.icon)
                imageTintList = ColorStateList.valueOf(palette.foreground)
                setPadding(dp(3), dp(3), dp(3), dp(3))
                contentDescription = null
            }, LinearLayout.LayoutParams(dp(27), dp(27)))
            cell.addView(TextView(this).apply {
                text = tool.name
                textSize = 11.5f
                setTextColor(palette.foreground)
                gravity = Gravity.CENTER
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(23)
            ))
            grid.addView(cell, GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(index / 3)
                columnSpec = GridLayout.spec(index % 3, 1f)
                width = 0
                this.height = dp(height)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }
        panel.addView(grid, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
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

    /** Sticky category bar + clipped scroll region + sticky bottom key row. */
    private fun renderEmoji(panel: LinearLayout) {
        val categoryStrip = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = true
            clipToPadding = true
        }
        val categories = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        EmojiCatalog.groups.forEachIndexed { index, group ->
            val category = iconControl(
                categoryIcon(group.name),
                group.name + " emoji",
                if (emojiGroup == index) palette.selected else palette.background
            ) {
                emojiGroup = index
                render()
            }
            category.imageTintList = ColorStateList.valueOf(
                if (emojiGroup == index) palette.accentText else palette.foreground
            )
            categories.addView(category, LinearLayout.LayoutParams(
                dp(43), dp(39)
            ).apply { setMargins(dp(3), dp(1), dp(3), dp(1)) })
        }
        categoryStrip.addView(categories)
        panel.addView(categoryStrip, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(44)
        ))

        val selectedGroup = EmojiCatalog.groups[
            emojiGroup.coerceIn(0, EmojiCatalog.groups.lastIndex)
        ]
        val emojis = if (selectedGroup.name == "Recent") {
            recentEmoji.toList()
        } else selectedGroup.emoji

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipToPadding = true
            clipChildren = true
            setPadding(dp(3), dp(3), dp(3), dp(6))
        }
        inner.addView(TextView(this).apply {
            text = selectedGroup.name
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.secondary)
            setPadding(dp(9), dp(3), 0, dp(6))
        })
        if (emojis.isEmpty()) {
            inner.addView(TextView(this).apply {
                text = "Your recently used emoji will appear here."
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(palette.secondary)
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(80)
            ))
        } else {
            val columns = 8
            val grid = GridLayout(this).apply {
                columnCount = columns
                clipChildren = true
                clipToPadding = true
            }
            val available = resources.displayMetrics.widthPixels - dp(19)
            val cellWidth = (available / columns - dp(2))
                .coerceAtLeast(dp(32)).coerceAtMost(dp(57))
            emojis.forEach { character ->
                val emojiKey = TextView(this).apply {
                    text = character
                    textSize = 25.5f
                    typeface = Typeface.DEFAULT
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                    isClickable = true
                    contentDescription = "Insert $character emoji"
                    setOnClickListener {
                        currentInputConnection?.commitText(character, 1)
                        recentEmoji.remove(character)
                        recentEmoji.add(0, character)
                        if (recentEmoji.size > 40) recentEmoji.removeAt(recentEmoji.lastIndex)
                    }
                }
                grid.addView(emojiKey, GridLayout.LayoutParams().apply {
                    width = cellWidth
                    height = dp(43)
                    setMargins(dp(1), dp(1), dp(1), dp(1))
                })
            }
            inner.addView(grid, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        // Only this fixed-height viewport scrolls. Neither the category toolbar
        // nor the bottom action keys are children of the ScrollView.
        val scroller = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = true
            clipToPadding = true
            isFillViewport = false
            addView(inner, ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        val frame = FrameLayout(this).apply {
            clipChildren = true
            clipToPadding = true
            addView(scroller, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }
        val emojiRows = if (mode == KeyboardMode.DEVELOPER) 4 else 3
        panel.addView(frame, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(keyHeight() * emojiRows + 9)
        ))

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listOf(
            KeySpec("ABC", KeyAction.Layer(KeyboardLayer.LETTERS), 1.4f),
            KeySpec("Space", KeyAction.Text(" "), 4.4f),
            KeySpec("Delete", KeyAction.Hardware(KeyEvent.KEYCODE_DEL), 1.2f,
                "Delete"),
            KeySpec("Enter", KeyAction.Hardware(KeyEvent.KEYCODE_ENTER), 1.3f,
                "Enter")
        ).forEach { spec ->
            footer.addView(createKey(spec, false), LinearLayout.LayoutParams(
                0, dp(keyHeight()), spec.weight
            ).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        }
        panel.addView(footer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(keyHeight() + 4)
        ))
    }

    private fun categoryIcon(category: String): Int = when (category) {
        "Recent" -> R.drawable.ic_kx_recent
        "Smileys" -> R.drawable.ic_kx_smile
        "People" -> R.drawable.ic_kx_people
        "Animals" -> R.drawable.ic_kx_animals
        "Food" -> R.drawable.ic_kx_food
        "Activities" -> R.drawable.ic_kx_activity
        "Travel" -> R.drawable.ic_kx_travel
        "Objects" -> R.drawable.ic_kx_objects
        "Symbols" -> R.drawable.ic_kx_symbols
        "Flags" -> R.drawable.ic_kx_flags
        else -> R.drawable.ic_kx_emoji
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

    /** VectorDrawable graphics replace emoji characters in keyboard controls. */
    private fun keyIcon(spec: KeySpec): Int? = when (val action = spec.action) {
        is KeyAction.Layer -> if (action.layer == KeyboardLayer.EMOJI)
            R.drawable.ic_kx_emoji else null
        is KeyAction.Hardware -> when (action.code) {
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL ->
                R.drawable.ic_kx_backspace
            KeyEvent.KEYCODE_ENTER -> R.drawable.ic_kx_enter
            else -> null
        }
        KeyAction.Picker -> R.drawable.ic_kx_globe
        else -> null
    }

    private fun paint(view: TextView, spec: KeySpec) {
        val color = keyColor(spec)
        val active = color == palette.selected
        val icon = keyIcon(spec)
        if (icon != null) {
            view.text = ""
            val glyph = getDrawable(icon)?.mutate()
            glyph?.setTint(if (active) palette.accentText else palette.foreground)
            glyph?.setBounds(0, 0, dp(23), dp(23))
            view.setCompoundDrawables(null, glyph, null, null)
        } else {
            view.setCompoundDrawables(null, null, null, null)
            view.text = when {
                spec.action === KeyAction.Shift -> "Shift"
                spec.action is KeyAction.Text && spec.action.value.length == 1 &&
                    spec.action.value[0].isLetter() && shouldUppercase() ->
                    spec.label.uppercase(Locale.ROOT)
                else -> spec.label
            }
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
                toolsOpen = false
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
            KeyAction.Tools -> {
                toolsOpen = !toolsOpen
                if (toolsOpen) finishComposition(false)
                render()
            }
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
                toolsOpen = false
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
