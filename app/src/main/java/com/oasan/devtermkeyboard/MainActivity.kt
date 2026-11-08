package com.oasan.devtermkeyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Setup screen. The keyboard itself appears through Android's system IME picker. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val background = Color.rgb(16, 20, 28)
        val textColor = Color.rgb(241, 246, 250)
        val muted = Color.rgb(172, 190, 206)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(background)
        }

        fun label(text: String, size: Float, color: Int, bold: Boolean = false) {
            layout.addView(TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                typeface = Typeface.create(Typeface.MONOSPACE, if (bold) Typeface.BOLD else Typeface.NORMAL)
                setPadding(0, 0, 0, dp(14))
            })
        }

        fun actionButton(text: String, callback: () -> Unit) {
            val button = Button(this).apply {
                this.text = text
                setAllCaps(false)
                setTextColor(textColor)
                textSize = 15f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                this.background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(Color.rgb(37, 134, 124))
                }
                setOnClickListener { callback() }
            }
            layout.addView(button, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)
            ).apply { bottomMargin = dp(12) })
        }

        label("⌨ DEVTERM KEYBOARD", 26f, textColor, true)
        label("A compact developer keyboard for Android terminals and code editors.", 15f, muted)
        label("SETUP", 14f, textColor, true)
        label("1. Enable DevTerm Keyboard in Android's on-screen keyboard settings.\n\n2. Tap Choose keyboard and select DevTerm Keyboard.\n\n3. Open Termux, SSH, or a code editor and start typing.", 14f, muted)

        actionButton("1  Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        actionButton("2  Choose keyboard") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }

        label("LAYERS", 14f, textColor, true)
        label("ABC — QWERTY and numbers\nSYM — programming punctuation\nNAV — cursor, editing and Ctrl shortcuts\nFN — F1–F12 and PC keys (Meta, Caps, etc.)\n\nCTRL / ALT / SHIFT / META are one-shot modifiers: tap one, then the desired key.", 13f, muted)
        label("PRIVACY", 14f, textColor, true)
        label("No internet permission, analytics, keyboard logs, or clipboard history. Everything runs on your device.", 13f, muted)
        label("Developer preview • v0.1.0", 11f, muted)

        val scroll = ScrollView(this).apply {
            setFillViewport(true)
            addView(layout)
            isVerticalScrollBarEnabled = false
        }
        setContentView(scroll)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
