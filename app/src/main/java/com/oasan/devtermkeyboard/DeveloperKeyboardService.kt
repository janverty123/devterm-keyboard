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
import android.graphics.drawable.LayerDrawable
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
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
    private var voiceStatusText: TextView? = null
    private var voiceHeaderActive = false
    private val inlineVoice by lazy {
        InlineVoiceController(this,
            onState = { state, partial ->
                val activeNow = state != InlineVoiceController.State.IDLE
                if (voiceHeaderActive != activeNow) refreshHeader()
                if (activeNow) {
                    voiceStatusText?.text = when (state) {
                        InlineVoiceController.State.STARTING -> "Starting…"
                        InlineVoiceController.State.LISTENING -> if (partial.isNullOrBlank())
                            "Listening…" else "Listening…  " + partial.take(36)
                        InlineVoiceController.State.PROCESSING -> "Processing…"
                        InlineVoiceController.State.IDLE -> ""
                    }
                }
            },
            onFinalText = { text ->
                if (currentInputConnection != null &&
                    currentInputEditorInfo?.packageName == voiceTarget) {
                    finishComposition(false)
                    currentInputConnection?.commitText(text, 1)
                }
            },
            onFailure = { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        )
    }
    private var emojiGroup = 0
    private val recentEmoji = mutableListOf<String>()
    private var toolsOpen = false
    private var customizingTools = false
    private var selectedShortcutSlot = 0
    private var emojiSearchMode = false
    private var emojiSearchQuery = ""
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
        inlineVoice.stop()
        // Do not commit the previous field's composition into a newly focused app.
        composing.setLength(0)
        mode = KeyboardPreferences.mode(this)
        palette = KeyboardPalette.forContext(this)
        layer = KeyboardLayer.LETTERS
        toolsOpen = false
        customizingTools = false
        emojiSearchMode = false
        emojiSearchQuery = ""
        previousWord = ""
        oneShotShift = false
        capsLock = false
        modifiers.clear()
        updateAutoCaps()
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
        inlineVoice.stop()
        clearRepeat()
        preview.dismiss()
        finishComposition(false)
        modifiers.clear()
        oneShotShift = false
        capsLock = false
        super.onFinishInput()
    }

    override fun onDestroy() {
        inlineVoice.stop()
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
        if (customizingTools) {
            renderShortcutEditor(panel)
            return
        }
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
        voiceHeaderActive = inlineVoice.active
        voiceStatusText = null
        if (inlineVoice.active) {
            line.addView(iconControl(
                R.drawable.ic_kx_back, "Stop voice typing", palette.key
            ) {
                inlineVoice.stop()
            }, LinearLayout.LayoutParams(dp(43), dp(40)).apply {
                rightMargin = dp(3)
            })
            voiceStatusText = TextView(this).apply {
                text = when (inlineVoice.state) {
                    InlineVoiceController.State.STARTING -> "Starting…"
                    InlineVoiceController.State.LISTENING -> "Listening…"
                    InlineVoiceController.State.PROCESSING -> "Processing…"
                    InlineVoiceController.State.IDLE -> ""
                }
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(palette.foreground)
                contentDescription = "Voice typing status"
            }
            line.addView(voiceStatusText, LinearLayout.LayoutParams(0, dp(40), 1f))
            line.addView(iconControl(
                R.drawable.ic_kx_chevron, "Stop listening", palette.auxiliary
            ) { inlineVoice.stop() }, LinearLayout.LayoutParams(dp(38), dp(40)))
            line.addView(iconControl(
                R.drawable.ic_kx_mic, "Microphone active. Tap to stop",
                palette.selected
            ) { inlineVoice.stop() }, LinearLayout.LayoutParams(dp(44), dp(40)).apply {
                leftMargin = dp(4)
            })
            return line
        }
        val menuIcon = if (toolsOpen || customizingTools) R.drawable.ic_kx_back else R.drawable.ic_kx_tools
        line.addView(iconControl(menuIcon,
            if (toolsOpen || customizingTools) "Back to tools" else "Keyboard tools",
            if (toolsOpen || customizingTools) palette.selected else palette.auxiliary
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
                KeyboardPreferences.toolbarSlots(this@DeveloperKeyboardService).forEach { id ->
                    val shortcut = ToolbarShortcuts.lookup(id)
                    quick.addView(iconControl(shortcut.icon, shortcut.title, palette.background) {
                        dispatch(shortcut.action)
                    }, LinearLayout.LayoutParams(dp(40), dp(40)))
                }
            }
            line.addView(quick, LinearLayout.LayoutParams(0, dp(40), 1f))
        }

        line.addView(iconControl(R.drawable.ic_kx_mic, "Voice typing",
            palette.background
        ) { dispatch(KeyAction.Voice) },
            LinearLayout.LayoutParams(dp(42), dp(40)).apply { leftMargin = dp(2) })
        return line
    }

    private fun refreshHeader() {
        val panel = root ?: return
        if (panel.childCount == 0) return
        panel.removeViewAt(0)
        panel.addView(makeHeader(), 0)
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
            },
            Tool(R.drawable.ic_kx_customize, "Customize") {
                customizingTools = true
                toolsOpen = false
                selectedShortcutSlot = 0
                render()
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

    /** Gboard-inspired pinning with both drag/drop and an accessible tap-to-assign path. */
    private fun renderShortcutEditor(panel: LinearLayout) {
        val viewport = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = true
            clipToPadding = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), dp(4), dp(7), dp(12))
        }
        content.addView(TextView(this).apply {
            text = "Customize toolbar"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.foreground)
            setPadding(dp(8), dp(6), 0, dp(6))
        })
        content.addView(TextView(this).apply {
            text = "Tap a slot and then a tool, or hold and drag a tool into a slot."
            textSize = 12f
            setTextColor(palette.secondary)
            setPadding(dp(8), 0, dp(8), dp(12))
        })
        val slots = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        KeyboardPreferences.toolbarSlots(this).forEachIndexed { index, id ->
            val shortcut = ToolbarShortcuts.lookup(id)
            val cell = shortcutTile(shortcut, index == selectedShortcutSlot).apply {
                contentDescription = "Pinned shortcut " + (index + 1) + ": " + shortcut.title
                setOnClickListener {
                    selectedShortcutSlot = index
                    render()
                }
                setOnDragListener { _, event ->
                    when (event.action) {
                        android.view.DragEvent.ACTION_DRAG_STARTED ->
                            event.localState is String
                        android.view.DragEvent.ACTION_DROP -> {
                            val dragged = event.localState as? String
                            if (dragged != null) {
                                KeyboardPreferences.setToolbarSlot(
                                    this@DeveloperKeyboardService, index, dragged
                                )
                                selectedShortcutSlot = index
                                render()
                                true
                            } else false
                        }
                        else -> true
                    }
                }
            }
            slots.addView(cell, LinearLayout.LayoutParams(
                0, dp(70), 1f
            ).apply { setMargins(dp(4), 0, dp(4), dp(8)) })
        }
        content.addView(slots)
        content.addView(TextView(this).apply {
            text = "Available shortcuts"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.foreground)
            setPadding(dp(8), dp(8), 0, dp(7))
        })
        val choices = GridLayout(this).apply {
            columnCount = 2
            clipChildren = true
            clipToPadding = true
        }
        ToolbarShortcuts.items.forEachIndexed { index, shortcut ->
            val cell = shortcutTile(shortcut, false).apply {
                contentDescription = "Assign " + shortcut.title
                setOnClickListener {
                    KeyboardPreferences.setToolbarSlot(
                        this@DeveloperKeyboardService, selectedShortcutSlot, shortcut.id
                    )
                    selectedShortcutSlot = (selectedShortcutSlot + 1) % 2
                    render()
                }
                setOnLongClickListener {
                    val clip = ClipData.newPlainText("shortcut", shortcut.id)
                    startDragAndDrop(clip, View.DragShadowBuilder(this), shortcut.id, 0)
                    true
                }
            }
            choices.addView(cell, GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(index / 2)
                columnSpec = GridLayout.spec(index % 2, 1f)
                width = 0
                height = dp(59)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }
        content.addView(choices)
        val footer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun footerButton(label: String, click: () -> Unit) = TextView(this).apply {
            text = label
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(palette.foreground)
            background = rounded(palette.auxiliary, 12)
            setOnClickListener { click() }
        }
        footer.addView(footerButton("Reset") {
            KeyboardPreferences.resetToolbar(this)
            render()
        }, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
            setMargins(dp(4), 0, dp(4), 0)
        })
        footer.addView(footerButton("Done") {
            customizingTools = false
            toolsOpen = false
            render()
        }, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
            setMargins(dp(4), 0, dp(4), 0)
        })
        content.addView(footer)
        viewport.addView(content)
        panel.addView(viewport, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(keyHeight() * (if (mode == KeyboardMode.DEVELOPER) 5 else 4) + 9)
        ))
    }

    private fun shortcutTile(shortcut: ToolbarShortcut, active: Boolean): LinearLayout {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(8), dp(4))
            background = rounded(if (active) palette.selected else palette.auxiliary, 13)
            isClickable = true
            isFocusable = true
        }
        tile.addView(ImageView(this).apply {
            setImageResource(shortcut.icon)
            imageTintList = ColorStateList.valueOf(
                if (active) palette.accentText else palette.foreground
            )
            contentDescription = null
        }, LinearLayout.LayoutParams(dp(24), dp(24)).apply {
            rightMargin = dp(8)
        })
        tile.addView(TextView(this).apply {
            text = shortcut.title
            textSize = 12f
            setTextColor(if (active) palette.accentText else palette.foreground)
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, dp(34), 1f))
        return tile
    }

    private fun updateSuggestions() {
        val container = suggestionBar ?: return
        container.removeAllViews()
        val options = WordSuggestions.suggest(composing.toString(), previousWord)
        options.take(3).forEachIndexed { index, word ->
            if (index > 0) {
                container.addView(View(this).apply {
                    setBackgroundColor((palette.secondary and 0x00FFFFFF) or 0x44000000)
                    contentDescription = null
                }, LinearLayout.LayoutParams(dp(1), dp(20)).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                })
            }
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
    /** Dedicated emoji picker. Search uses our own keys so no second IME can overlap it. */
    private fun renderEmoji(panel: LinearLayout) {
        val searchBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(palette.auxiliary, 14)
            clipChildren = true
            setPadding(dp(4), 0, dp(4), 0)
        }
        searchBar.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_kx_search)
            imageTintList = ColorStateList.valueOf(palette.foreground)
            contentDescription = "Search emojis"
            setPadding(dp(7), dp(7), dp(7), dp(7))
        }, LinearLayout.LayoutParams(dp(36), dp(36)))
        searchBar.addView(TextView(this).apply {
            text = if (emojiSearchQuery.isEmpty()) "Search emojis" else emojiSearchQuery
            textSize = 14f
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.START
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(if (emojiSearchQuery.isEmpty()) palette.secondary else palette.foreground)
            setOnClickListener {
                if (!emojiSearchMode) {
                    emojiSearchMode = true
                    render()
                }
            }
            contentDescription = if (emojiSearchMode) "Emoji search query: " + emojiSearchQuery
                else "Tap to search emojis"
        }, LinearLayout.LayoutParams(0, dp(38), 1f))
        searchBar.addView(iconControl(
            if (emojiSearchMode) R.drawable.ic_kx_back else R.drawable.ic_kx_search,
            if (emojiSearchMode) "Close emoji search" else "Start emoji search",
            palette.auxiliary
        ) {
            if (emojiSearchMode) {
                emojiSearchMode = false
                emojiSearchQuery = ""
            } else emojiSearchMode = true
            render()
        }, LinearLayout.LayoutParams(dp(39), dp(38)))
        panel.addView(searchBar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(39)
        ).apply { setMargins(dp(8), dp(3), dp(8), dp(4)) })

        if (!emojiSearchMode) {
            val categories = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                clipChildren = true
                clipToPadding = true
            }
            val categoryRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            EmojiCatalog.displayedGroups().forEachIndexed { index, group ->
                val cell = iconControl(categoryIcon(group.name),
                    group.name + " emojis",
                    if (emojiGroup == index) palette.selected else palette.background
                ) {
                    emojiGroup = index
                    render()
                }
                cell.imageTintList = ColorStateList.valueOf(
                    if (index == emojiGroup) palette.accentText else palette.foreground
                )
                categoryRow.addView(cell, LinearLayout.LayoutParams(
                    dp(43), dp(39)
                ).apply { setMargins(dp(3), dp(1), dp(3), dp(1)) })
            }
            categories.addView(categoryRow)
            panel.addView(categories, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(43)
            ))
        }

        val selectedGroup = EmojiCatalog.displayedGroups()[
            emojiGroup.coerceIn(0, EmojiCatalog.displayedGroups().lastIndex)
        ]
        val emojis = if (emojiSearchMode) {
            if (emojiSearchQuery.isBlank()) {
                if (recentEmoji.isNotEmpty()) recentEmoji.toList()
                else EmojiCatalog.groups[1].emoji.take(56)
            } else EmojiCatalog.search(emojiSearchQuery)
        } else if (selectedGroup.name == "Recent") recentEmoji.toList()
        else selectedGroup.emoji

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = true
            clipToPadding = true
            setPadding(dp(3), dp(3), dp(3), dp(6))
        }
        inner.addView(TextView(this).apply {
            text = if (emojiSearchMode) {
                if (emojiSearchQuery.isBlank()) "Try searching: heart, dog, fire..." else
                    "Results for: " + emojiSearchQuery
            } else selectedGroup.name
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.secondary)
            setPadding(dp(8), dp(2), 0, dp(4))
        })
        if (emojis.isEmpty()) {
            inner.addView(TextView(this).apply {
                text = if (emojiSearchMode) "No matching emojis" else
                    "Recently used emojis appear here."
                gravity = Gravity.CENTER
                setTextColor(palette.secondary)
                textSize = 13f
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70)
            ))
        } else {
            val columns = if (resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE) 10 else 8
            val grid = GridLayout(this).apply {
                columnCount = columns
                clipChildren = true
                clipToPadding = true
            }
            val cellWidth = ((resources.displayMetrics.widthPixels - dp(24)) /
                columns - dp(2)).coerceAtLeast(dp(22)).coerceAtMost(dp(57))
            emojis.forEach { character ->
                val key = TextView(this).apply {
                    text = character
                    textSize = if (columns == 10) 22f else 24f
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
                grid.addView(key, GridLayout.LayoutParams().apply {
                    width = cellWidth
                    height = dp(43)
                    setMargins(dp(1), dp(1), dp(1), dp(1))
                })
            }
            inner.addView(grid)
        }
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = true
            clipToPadding = true
            isFillViewport = false
            addView(inner, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        val viewport = FrameLayout(this).apply {
            clipChildren = true
            clipToPadding = true
            addView(scroll, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }
        val viewportHeight = if (emojiSearchMode) dp(90) else dp(
            ((if (mode == KeyboardMode.DEVELOPER) 4 else 3) * keyHeight() - 20)
                .coerceAtLeast(92)
        )
        panel.addView(viewport, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, viewportHeight
        ))

        if (emojiSearchMode) {
            renderEmojiSearchKeys(panel)
        } else {
            val footer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            listOf(
                KeySpec("ABC", KeyAction.Layer(KeyboardLayer.LETTERS), 1.4f),
                KeySpec("Space", KeyAction.Text(" "), 4.4f),
                KeySpec("Delete", KeyAction.Hardware(KeyEvent.KEYCODE_DEL), 1.2f),
                KeySpec("Enter", KeyAction.Hardware(KeyEvent.KEYCODE_ENTER), 1.3f)
            ).forEach { spec ->
                footer.addView(createKey(spec, false), LinearLayout.LayoutParams(
                    0, dp(keyHeight()), spec.weight
                ).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
            }
            panel.addView(footer, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(keyHeight() + 4)
            ))
        }
    }

    /** Query keyboard in the IME itself: no recursive soft-keyboard launch. */
    private fun renderEmojiSearchKeys(panel: LinearLayout) {
        val rows = KeyLayouts.rows(KeyboardMode.STANDARD, KeyboardLayer.LETTERS).take(3)
        rows.forEach { specs ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            specs.forEach { spec ->
                row.addView(createKey(spec, false), LinearLayout.LayoutParams(
                    0, dp(35), spec.weight
                ).apply { setMargins(dp(2), dp(1), dp(2), dp(1)) })
            }
            panel.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(37)
            ))
        }
        val last = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun action(label: String, weight: Float, click: () -> Unit) {
            val key = TextView(this).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(palette.foreground)
                background = rounded(palette.auxiliary, 9)
                setOnClickListener { click() }
            }
            last.addView(key, LinearLayout.LayoutParams(0, dp(37), weight).apply {
                setMargins(dp(2), dp(1), dp(2), dp(1))
            })
        }
        action("ABC", 1.2f) {
            emojiSearchMode = false
            emojiSearchQuery = ""
            dispatch(KeyAction.Layer(KeyboardLayer.LETTERS))
        }
        action("Clear", 1.2f) {
            emojiSearchQuery = ""
            render()
        }
        action("Space", 3.5f) {
            emojiSearchQuery = (emojiSearchQuery + " ").take(40)
            render()
        }
        action("Done", 1.2f) {
            emojiSearchMode = false
            emojiSearchQuery = ""
            render()
        }
        panel.addView(last, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(39)
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
        "More" -> R.drawable.ic_kx_symbols
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

    /**
     * Center VectorDrawable keys using a background LayerDrawable.
     * TextView top compound drawables shifted Emoji, Backspace and Enter upward.
     */
    private fun paint(view: TextView, spec: KeySpec) {
        val color = keyColor(spec)
        val active = color == palette.selected
        val icon = keyIcon(spec)
        view.setCompoundDrawables(null, null, null, null)
        val surface = rounded(color, 10)
        if (icon != null) {
            view.text = ""
            val glyph = getDrawable(icon)?.mutate()
            glyph?.setTint(if (active) palette.accentText else palette.foreground)
            if (glyph != null) {
                view.background = LayerDrawable(arrayOf(surface, glyph)).apply {
                    setLayerGravity(1, Gravity.CENTER)
                    setLayerWidth(1, dp(24))
                    setLayerHeight(1, dp(24))
                }
            } else view.background = surface
        } else {
            view.text = when {
                spec.action === KeyAction.Shift -> "Shift"
                spec.action is KeyAction.Text && spec.action.value.length == 1 &&
                    spec.action.value[0].isLetter() && shouldUppercase() ->
                    spec.label.uppercase(Locale.ROOT)
                else -> spec.label
            }
            view.background = surface
        }
        view.setTextColor(if (active) palette.accentText else palette.foreground)
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
        val drawable = when (val background = v.background) {
            is GradientDrawable -> background
            is LayerDrawable -> background.getDrawable(0) as? GradientDrawable
            else -> null
        } ?: return
        val start = if (down) keyColor(spec) else palette.selected
        val end = if (down) palette.selected else keyColor(spec)
        val animation = ValueAnimator.ofArgb(start, end)
        animation.duration = if (down) 55L else 110L
        animation.addUpdateListener { drawable.setColor(it.animatedValue as Int) }
        animation.start()
    }

    private fun shouldUppercase() = capsLock || oneShotShift || autoCaps

    private fun dispatch(a: KeyAction) {
        // Emoji search keys edit our local query, not the host application's text.
        if (layer == KeyboardLayer.EMOJI && emojiSearchMode) {
            when (a) {
                is KeyAction.Text -> {
                    if (a.value.length == 1 && !Character.isISOControl(a.value[0])) {
                        val character = if (shouldUppercase()) a.value.uppercase(Locale.ROOT)
                            else a.value
                        emojiSearchQuery = (emojiSearchQuery + character).take(40)
                        oneShotShift = false
                        render()
                    }
                    return
                }
                is KeyAction.Hardware -> if (a.code == KeyEvent.KEYCODE_DEL) {
                    if (emojiSearchQuery.isNotEmpty()) {
                        val end = Character.offsetByCodePoints(
                            emojiSearchQuery, emojiSearchQuery.length, -1
                        )
                        emojiSearchQuery = emojiSearchQuery.substring(0, end)
                    }
                    render()
                    return
                }
                else -> Unit
            }
        }
        when (a) {
            is KeyAction.Layer -> {
                finishComposition(false)
                toolsOpen = false
                if (a.layer != KeyboardLayer.EMOJI) {
                    emojiSearchMode = false
                    emojiSearchQuery = ""
                }
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
                if (customizingTools) {
                    customizingTools = false
                    toolsOpen = true
                } else {
                    toolsOpen = !toolsOpen
                    if (toolsOpen) finishComposition(false)
                }
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
                customizingTools = false
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
        val type = currentInputEditorInfo?.inputType ?: 0
        val variation = type and InputType.TYPE_MASK_VARIATION
        if (variation in setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_NUMBER_VARIATION_PASSWORD
        )) return
        if (inlineVoice.active) {
            inlineVoice.stop()
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Grant microphone access once to use inline dictation.",
                Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("request_microphone", true)
            })
            return
        }
        voiceTarget = currentInputEditorInfo?.packageName
        val wasMenuVisible = toolsOpen || customizingTools
        toolsOpen = false
        customizingTools = false
        finishComposition(false)
        if (wasMenuVisible) render()
        inlineVoice.start()
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
