package com.oasan.devtermkeyboard

import android.app.Activity
import android.content.Intent
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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.max

/**
 * Native setup/settings screen. IME settings are stored locally and applied
 * the next time the keyboard appears; no INTERNET permission is requested.
 */
class MainActivity : Activity() {
    private val background = Color.rgb(23, 29, 38)
    private val foreground = Color.rgb(245, 248, 252)
    private val muted = Color.rgb(174, 192, 210)
    private val accent = Color.rgb(100, 211, 192)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(this@MainActivity.background)
            setPadding(dp(22), dp(26), dp(22), dp(24))
        }

        fun heading(title: String) {
            panel.addView(TextView(this).apply {
                text = title
                textSize = 13f
                setTextColor(accent)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(0, dp(16), 0, dp(8))
            })
        }

        fun text(message: String, size: Float = 14f) {
            panel.addView(TextView(this).apply {
                this.text = message
                textSize = size
                setTextColor(muted)
                setLineSpacing(dp(2).toFloat(), 1f)
                setPadding(0, 0, 0, dp(13))
            })
        }

        fun button(title: String, primary: Boolean = false, click: () -> Unit) {
            val b = Button(this).apply {
                this.text = title
                textSize = 14f
                setAllCaps(false)
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(if (primary) this@MainActivity.background else this@MainActivity.foreground)
                background = GradientDrawable().apply {
                    setColor(if (primary) accent else Color.rgb(58, 70, 84))
                    cornerRadius = dp(13).toFloat()
                }
                setOnClickListener { click() }
            }
            panel.addView(b, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
            ).apply { bottomMargin = dp(9) })
        }

        panel.addView(TextView(this).apply {
            text = "⌨  DEVTERM"
            textSize = 28f
            setTextColor(this@MainActivity.foreground)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(7))
        })
        text("Developer keyboard for Termux, code editors and SSH. Optimized for narrow screens and Android navigation gestures.")

        heading("GET STARTED")
        button("1    Enable DevTerm Keyboard", primary = true) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        button("2    Choose active keyboard") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }

        heading("APPEARANCE")
        val theme = KeyboardPreferences.theme(this)
        val themeLabel = when (theme) {
            "light" -> "Light"
            "dark" -> "Dark"
            else -> "Follow system"
        }
        button("Keyboard theme: $themeLabel") {
            val next = when (theme) {
                "system" -> "light"
                "light" -> "dark"
                else -> "system"
            }
            KeyboardPreferences.setTheme(this, next)
            recreate()
        }
        button("Keyboard height: " + if (KeyboardPreferences.isCompact(this)) "Compact" else "Standard") {
            KeyboardPreferences.setCompact(this, !KeyboardPreferences.isCompact(this))
            recreate()
        }
        button("Haptic feedback: " + if (KeyboardPreferences.hasHaptics(this)) "On" else "Off") {
            KeyboardPreferences.setHaptics(this, !KeyboardPreferences.hasHaptics(this))
            recreate()
        }

        heading("SHORTCUTS")
        text("Hold SPACE to open the Android keyboard chooser. Tap ⇧ once to capitalize the next letter; double-tap or hold ⇧ to lock uppercase. Tap ⇧ again to unlock.")
        text("Swipe the compact top toolbar for Esc, Tab, Ctrl, Alt, Copy, Paste, arrows and function keys. Use ?123, Nav and Fn for additional developer keys.")

        heading("PRIVACY")
        text("Everything runs locally. No internet permission, analytics, keyboard history or clipboard storage. Copy and Paste only access the clipboard when you tap them.")
        text("Developer preview • v0.2.0 • Device compatibility may vary.", 11f)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(panel)
            setBackgroundColor(this@MainActivity.background)
        }
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            val top: Int
            val bottom: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val system = insets.getInsets(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
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
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
