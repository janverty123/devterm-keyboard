package com.oasan.devtermkeyboard

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.max

/**
 * Termora onboarding: clear permission/activation status and native Material-like
 * cards without dependencies, advertisements, accounts or remote services.
 */
class MainActivity : Activity() {
    private val pageBackground = Color.rgb(13, 21, 34)
    private val surface = Color.rgb(23, 36, 52)
    private val elevated = Color.rgb(36, 53, 71)
    private val foreground = Color.rgb(238, 248, 251)
    private val secondary = Color.rgb(167, 186, 198)
    private val accent = Color.rgb(67, 228, 198)
    private val accentInk = Color.rgb(9, 35, 34)
    private val permissionRequest = 301

    private var enableState: TextView? = null
    private var selectedState: TextView? = null
    private var micState: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = pageBackground
        window.navigationBarColor = pageBackground

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBackground)
            setPadding(dp(19), dp(18), dp(19), dp(36))
        }

        val hero = card(panel, padded = 18)
        val brandLine = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brandLine.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_termora_mark)
            background = shape(Color.rgb(7, 27, 38), 18)
            setPadding(dp(5), dp(5), dp(5), dp(5))
            contentDescription = "Termora terminal key logo"
        }, LinearLayout.LayoutParams(dp(82), dp(82)).apply { rightMargin = dp(14) })

        val brandCopy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        brandCopy.addView(label("Termora", 32f, foreground, true))
        brandCopy.addView(label("Your desktop keys, on Android.", 12.5f, secondary))
        brandLine.addView(brandCopy, LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        hero.addView(brandLine)

        hero.addView(label("TERMINAL-READY INPUT", 11f, accent, true).apply {
            letterSpacing = 0.11f
            setPadding(0, dp(14), 0, dp(5))
        })
        hero.addView(label(
            "Esc, Ctrl, Alt, Shift, function keys, code symbols and clipboard controls. " +
                "All in a compact keyboard that stays out of your way.",
            14f, secondary
        ))

        heading(panel, "Get started")
        val enabledCard = card(panel)
        enabledCard.addView(label("01   Enable keyboard", 17f, foreground, true))
        enableState = label("Checking Android settings…", 13f, secondary)
            .also(enabledCard::addView)
        action(enabledCard, "Open keyboard settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        val selectCard = card(panel)
        selectCard.addView(label("02   Make Termora active", 17f, foreground, true))
        selectedState = label("Select Termora as your typing keyboard.", 13f, secondary)
            .also(selectCard::addView)
        action(selectCard, "Choose keyboard") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showInputMethodPicker()
        }

        heading(panel, "Voice typing")
        val micCard = card(panel)
        micCard.addView(label("🎙   Voice recognition", 17f, foreground, true))
        micState = label(
            "Optional. Uses an Android speech service, such as Google's when installed.",
            13f, secondary
        ).also(micCard::addView)
        action(micCard, "Allow microphone") {
            askForMic()
        }
        micCard.addView(label(
            "Speech providers may send audio over the internet. Termora never saves recordings.",
            12f, secondary
        ))

        heading(panel, "Personalize")
        val settingsCard = card(panel)
        settingsCard.addView(label("Keyboard theme", 14f, foreground, true))
        val themes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("system" to "Auto", "light" to "Light", "dark" to "Dark").forEach { (id, title) ->
            val selected = KeyboardPreferences.theme(this) == id
            val button = Button(this).apply {
                text = title
                setAllCaps(false)
                textSize = 13f
                setTextColor(if (selected) accentInk else foreground)
                background = shape(if (selected) accent else elevated, 12)
                setOnClickListener {
                    KeyboardPreferences.setTheme(this@MainActivity, id)
                    recreate()
                }
            }
            themes.addView(button, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                setMargins(dp(2), dp(5), dp(2), dp(3))
            })
        }
        settingsCard.addView(themes, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        settingToggle(settingsCard, "Compact keyboard",
            "Smaller key rows for more terminal space.",
            KeyboardPreferences.isCompact(this)) {
            KeyboardPreferences.setCompact(this, it)
        }
        settingToggle(settingsCard, "Keypress bubbles",
            "Circular letter previews, just above the keys.",
            KeyboardPreferences.hasKeyPreview(this)) {
            KeyboardPreferences.setKeyPreview(this, it)
        }
        settingToggle(settingsCard, "Haptic feedback",
            "A gentle vibration when tapping keys.",
            KeyboardPreferences.hasHaptics(this)) {
            KeyboardPreferences.setHaptics(this, it)
        }

        heading(panel, "Quick guide")
        val guide = card(panel)
        guide.addView(label("SHIFT   Tap once · double-tap or hold for Caps Lock", 13f, foreground))
        guide.addView(spacing(dp(9)))
        guide.addView(label("PASTE   Use the clipboard button on the toolbar.", 13f, foreground))
        guide.addView(spacing(dp(9)))
        guide.addView(label("VOICE   Tap the microphone after granting permission.", 13f, foreground))
        guide.addView(spacing(dp(9)))
        guide.addView(label("SWITCH   Hold Space for Android's keyboard chooser.", 13f, foreground))
        guide.addView(spacing(dp(9)))
        guide.addView(label("TERMUX   Clipboard text is inserted as terminal input, without automatically pressing Enter.", 12.5f, secondary))

        panel.addView(label("Termora 0.3.0  ·  Developer preview", 12f, secondary).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(8))
        })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            setBackgroundColor(pageBackground)
            addView(panel)
        }
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            val top: Int
            val bottom: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val system = insets.getInsets(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                )
                top = system.top
                bottom = system.bottom
            } else {
                @Suppress("DEPRECATION")
                top = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottom = insets.systemWindowInsetBottom
            }
            view.setPadding(0, max(0, top), 0, max(0, bottom))
            insets
        }
        setContentView(scroll)

        if (intent.getBooleanExtra("request_voice_permission", false)) {
            intent.removeExtra("request_voice_permission")
            panel.post { askForMic() }
        }
    }

    override fun onResume() {
        super.onResume()
        val ime = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val component = ComponentName(this, DeveloperKeyboardService::class.java)
        val enabled = ime.enabledInputMethodList.any { it.id == component.flattenToShortString() }
        val active = Settings.Secure.getString(contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD) == component.flattenToShortString()
        enableState?.text = if (enabled) "✓ Enabled in Android settings" else
            "Turn on Termora under on-screen keyboards."
        selectedState?.text = if (active) "✓ Termora is your selected keyboard" else
            "Select Termora from the Android keyboard picker."
        micState?.text = if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED) {
            "✓ Microphone enabled. Tap 🎙 on the keyboard toolbar to dictate."
        } else {
            "Optional. Grant microphone access to use your installed speech service."
        }
    }

    private fun askForMic() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), permissionRequest)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequest) onResume()
    }

    private fun heading(parent: LinearLayout, title: String) {
        parent.addView(label(title, 19f, foreground, true).apply {
            setPadding(dp(3), dp(25), 0, dp(11))
        })
    }

    private fun card(parent: LinearLayout, padded: Int = 17): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(surface, 22)
            setPadding(dp(padded), dp(padded), dp(padded), dp(padded))
            elevation = dp(2).toFloat()
        }
        parent.addView(card, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(11) })
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

    private fun action(parent: LinearLayout, title: String, click: () -> Unit) {
        val button = Button(this).apply {
            text = title
            setAllCaps(false)
            textSize = 14f
            setTextColor(accentInk)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = RippleDrawable(
                ColorStateList.valueOf(0x55000000),
                shape(accent, 13), null
            )
            setOnClickListener { click() }
        }
        parent.addView(button, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(49)
        ).apply { topMargin = dp(13) })
    }

    private fun settingToggle(
        parent: LinearLayout, title: String, description: String,
        checked: Boolean, onToggle: (Boolean) -> Unit
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(8))
        }
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(label(title, 14f, foreground, true))
        labels.addView(label(description, 12f, secondary))
        row.addView(labels, LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val toggle = android.widget.Switch(this).apply {
            isChecked = checked
            buttonTintListOrAccent()
            setOnCheckedChangeListener { _, selected -> onToggle(selected) }
            contentDescription = title
        }
        row.addView(toggle)
        parent.addView(row)
    }

    private fun android.widget.Switch.buttonTintListOrAccent() {
        thumbTintList = ColorStateList.valueOf(accent)
        trackTintList = ColorStateList.valueOf(Color.rgb(72, 111, 120))
    }

    private fun shape(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun spacing(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, height)
    }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
