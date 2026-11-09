package com.oasan.devtermkeyboard

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import kotlin.math.max

/** Simple, accessible, Gboard-inspired setup and personalization. */
class MainActivity : Activity() {
    private val ink = Color.rgb(29, 42, 58)
    private val hint = Color.rgb(107, 121, 136)
    private val canvas = Color.rgb(247, 249, 252)
    private val accent = Color.rgb(37, 124, 138)
    private val selected = Color.rgb(215, 240, 241)
    private var enabledStatus: TextView? = null
    private var selectedStatus: TextView? = null
    private var microphoneStatus: TextView? = null
    private val microphoneRequestCode = 3047

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = canvas
        window.navigationBarColor = canvas
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(36))
            setBackgroundColor(canvas)
        }
        val hero = card(body)
        val brandLine = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brandLine.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_keylix_keyboard)
            background = round(Color.rgb(15, 28, 49), 18)
            setPadding(dp(5), dp(5), dp(5), dp(5))
            contentDescription = "Keylix keyboard icon"
        }, LinearLayout.LayoutParams(dp(75), dp(75)).apply { rightMargin = dp(14) })
        val brandCopy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brandCopy.addView(label("Keylix", 31f, ink, true))
        brandCopy.addView(label("Everyday typing. Developer power.", 12.5f, hint))
        brandLine.addView(brandCopy)
        hero.addView(brandLine)
        hero.addView(label(
            "Two keyboard modes for messages, coding, and terminal commands.",
            13.5f, hint
        ).apply { setPadding(0, dp(12), 0, 0) })

        heading(body, "Get started")
        val start = card(body)
        enabledStatus = label("Checking keyboard status…", 13.5f, hint).also(start::addView)
        selectedStatus = label("Checking selection…", 13.5f, hint).also(start::addView)
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        action(buttons, "1  Enable", true) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        action(buttons, "2  Choose", false) {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        start.addView(buttons)

        heading(body, "Keyboard mode")
        val modes = card(body)
        modes.addView(label(
            "Standard for messages. Dev for Termux, navigation, programming symbols and F-keys.",
            13f, hint
        ))
        val selector = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        KeyboardMode.entries.forEach { option ->
            val active = KeyboardPreferences.mode(this) == option
            val modeButton = TextView(this).apply {
                text = if (option == KeyboardMode.STANDARD) "Aa  Standard" else ">_  Developer"
                gravity = Gravity.CENTER
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (active) accent else ink)
                background = round(if (active) selected else canvas, 12)
                contentDescription = "Select " + option.name.lowercase() + " mode"
                setOnClickListener {
                    KeyboardPreferences.setMode(this@MainActivity, option)
                    recreate()
                }
            }
            selector.addView(modeButton, LinearLayout.LayoutParams(
                0, dp(48), 1f
            ).apply { setMargins(dp(3), dp(12), dp(3), 0) })
        }
        modes.addView(selector)

        heading(body, "Text correction")
        val textSettings = card(body)
        toggle(textSettings, "Suggestions",
            "Offline English word predictions in Standard mode.",
            KeyboardPreferences.predictions(this)) {
            KeyboardPreferences.setPredictions(this, it)
        }
        divider(textSettings)
        toggle(textSettings, "Autocorrect",
            "Conservative spelling fixes, never in Dev mode or passwords.",
            KeyboardPreferences.autocorrect(this)) {
            KeyboardPreferences.setAutocorrect(this, it)
        }

        heading(body, "Themes")
        val themeCard = card(body)
        val themeGrid = GridLayout(this).apply { columnCount = 2 }
        KeyboardPreferences.THEMES.forEachIndexed { index, entry ->
            val id = entry.first
            val title = entry.second
            val active = KeyboardPreferences.theme(this) == id
            val dotColor = when (id) {
                "light" -> Color.rgb(235, 241, 248)
                "dark" -> Color.rgb(61, 72, 87)
                "midnight" -> Color.rgb(17, 27, 45)
                "mint" -> Color.rgb(104, 192, 170)
                "slate" -> Color.rgb(65, 83, 109)
                "rose" -> Color.rgb(223, 159, 180)
                else -> Color.rgb(164, 189, 206)
            }
            val chip = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(5), dp(5), dp(5))
                background = round(if (active) selected else canvas, 11)
                contentDescription = "Theme " + title
                setOnClickListener {
                    KeyboardPreferences.setTheme(this@MainActivity, id)
                    recreate()
                }
            }
            chip.addView(View(this).apply { background = round(dotColor, 12) },
                LinearLayout.LayoutParams(dp(24), dp(24)).apply { rightMargin = dp(8) })
            chip.addView(label(title + if (active) " ✓" else "", 13f,
                if (active) accent else ink, active))
            themeGrid.addView(chip, GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(index / 2)
                columnSpec = GridLayout.spec(index % 2, 1f)
                width = 0
                height = dp(47)
                setMargins(dp(3), dp(4), dp(3), dp(4))
            })
        }
        themeCard.addView(themeGrid)

        heading(body, "Preferences")
        val preferences = card(body)
        toggle(preferences, "Compact key height",
            "More screen space for Termux and code editors.",
            KeyboardPreferences.isCompact(this)) {
            KeyboardPreferences.setCompact(this, it)
        }
        divider(preferences)
        toggle(preferences, "Letter popups",
            "Small character preview when tapping.",
            KeyboardPreferences.hasKeyPreview(this)) {
            KeyboardPreferences.setKeyPreview(this, it)
        }
        divider(preferences)
        toggle(preferences, "Haptic feedback",
            "Gentle vibration for keypresses.",
            KeyboardPreferences.hasHaptics(this)) {
            KeyboardPreferences.setHaptics(this, it)
        }

        heading(body, "Voice typing")
        val voice = card(body)
        voice.addView(label("Speak without leaving the keyboard", 14.5f, ink, true))
        voice.addView(label(
            "Tap the microphone on the keyboard. The toolbar will show Listening… " +
            "while your installed Android speech service recognizes your voice. " +
            "Press Back or the microphone to stop.",
            12.5f, hint
        ).apply { setPadding(0, dp(8), 0, dp(8)) })
        microphoneStatus = label("Checking microphone permission…", 13f, hint)
            .also(voice::addView)
        val microphoneActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        action(microphoneActions, "Allow microphone", true) {
            askMicrophonePermission()
        }
        voice.addView(microphoneActions)
        voice.addView(label(
            "Voice recognition may use Google's or another installed Android provider. " +
            "That provider may send audio online; Keylix itself does not save audio.",
            12f, hint
        ).apply { setPadding(0, dp(8), 0, 0) })

        heading(body, "Emojis")
        val emojiInfo = card(body)
        emojiInfo.addView(label(
            "Open the emoji key from the bottom row. Search emoji names or browse " +
            "categories without leaving the keyboard.",
            12.5f, hint
        ))

        heading(body, "Try typing")
        val test = card(body)
        test.addView(EditText(this).apply {
            hint = "Test typing, predictions and emojis…"
            minLines = 2
            textSize = 15f
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = round(canvas, 11)
        })
        body.addView(label("Keylix v0.6.0  •  Developer preview", 12f, hint).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, dp(14))
        })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            setBackgroundColor(canvas)
            addView(body)
        }
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            val top: Int
            val bottom: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                )
                top = bars.top
                bottom = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                top = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottom = insets.systemWindowInsetBottom
            }
            v.setPadding(0, max(0, top), 0, max(0, bottom))
            insets
        }
        setContentView(scroll)
        if (intent.getBooleanExtra("request_microphone", false)) {
            intent.removeExtra("request_microphone")
            scroll.post { askMicrophonePermission() }
        }
        if (intent.getStringExtra("open_section") == "themes") {
            scroll.post { scroll.smoothScrollTo(0, themeCard.top) }
        }
    }

    override fun onResume() {
        super.onResume()
        val ime = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val id = ComponentName(this, DeveloperKeyboardService::class.java).flattenToShortString()
        enabledStatus?.text = if (ime.enabledInputMethodList.any { it.id == id })
            "✓ Keyboard enabled in Android" else "Enable Keylix in Android keyboard settings."
        selectedStatus?.text = if (Settings.Secure.getString(contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD) == id)
            "✓ Keylix is active" else "Select Keylix to start typing."
        microphoneStatus?.text = if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) "✓ Microphone is enabled for inline voice typing."
        else "Microphone access is optional. Tap Allow microphone to enable it."
    }

    private fun askMicrophonePermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            microphoneStatus?.text = "✓ Microphone is already enabled."
            return
        }
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), microphoneRequestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == microphoneRequestCode) {
            microphoneStatus?.text = if (grantResults.firstOrNull() ==
                PackageManager.PERMISSION_GRANTED
            ) "✓ Microphone enabled. Return to any text field and tap the mic."
            else "Microphone access was denied. You can enable it later in Android settings."
        }
    }

    private fun heading(parent: LinearLayout, value: String) {
        parent.addView(label(value, 18f, ink, true).apply {
            setPadding(dp(4), dp(20), dp(4), dp(9))
        })
    }
    private fun card(parent: LinearLayout): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = round(Color.WHITE, 19)
            setPadding(dp(17), dp(16), dp(17), dp(16))
            elevation = dp(1).toFloat()
        }
        parent.addView(card, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(5) })
        return card
    }
    private fun label(value: String, size: Float, color: Int, strong: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            setLineSpacing(dp(2).toFloat(), 1f)
            typeface = Typeface.create(
                if (strong) "sans-serif-medium" else "sans-serif",
                if (strong) Typeface.BOLD else Typeface.NORMAL
            )
        }
    private fun action(parent: LinearLayout, title: String, primary: Boolean, click: () -> Unit) {
        parent.addView(Button(this).apply {
            text = title
            setAllCaps(false)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (primary) Color.WHITE else accent)
            background = round(if (primary) accent else selected, 12)
            setOnClickListener { click() }
        }, LinearLayout.LayoutParams(0, dp(49), 1f).apply {
            rightMargin = dp(6)
            topMargin = dp(12)
        })
    }
    private fun toggle(parent: LinearLayout, title: String, description: String,
                       value: Boolean, update: (Boolean) -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        val words = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        words.addView(label(title, 14.5f, ink, true))
        words.addView(label(description, 12.3f, hint))
        row.addView(words, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ))
        row.addView(Switch(this).apply {
            isChecked = value
            thumbTintList = ColorStateList.valueOf(accent)
            trackTintList = ColorStateList.valueOf(Color.rgb(144, 188, 192))
            contentDescription = title
            setOnCheckedChangeListener { _, checked -> update(checked) }
        })
        parent.addView(row)
    }
    private fun divider(parent: LinearLayout) {
        parent.addView(View(this).apply { setBackgroundColor(Color.rgb(224, 232, 240)) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
                .apply { topMargin = dp(6); bottomMargin = dp(6) })
    }
    private fun round(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}
