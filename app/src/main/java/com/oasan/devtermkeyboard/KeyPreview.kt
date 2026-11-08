package com.oasan.devtermkeyboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import android.text.InputType

/** A short-lived, round character bubble shown while a printable key is held. */
internal class KeyPreview(private val context: Context) {
    private var popup: PopupWindow? = null

    fun show(anchor: View, character: String, dark: Boolean, inputType: Int) {
        dismiss()
        if (!KeyboardPreferences.hasKeyPreview(context)) return
        if (character.length != 1 || character[0].isWhitespace()) return
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) return

        val size = dp(54)
        val bubble = TextView(context).apply {
            text = character
            textSize = 24f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(if (dark) 0xFFFFFFFF.toInt() else 0xFF172532.toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (dark) 0xFF506476.toInt() else 0xFFFFFFFF.toInt())
                setStroke(dp(1), 0xFF6ED7C0.toInt())
            }
            elevation = dp(7).toFloat()
        }
        val window = PopupWindow(bubble, size, size, false).apply {
            isClippingEnabled = false
            isTouchable = false
            isFocusable = false
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            elevation = dp(5).toFloat()
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0x00000000))
        }
        try {
            window.showAsDropDown(anchor, (anchor.width - size) / 2,
                -anchor.height - size - dp(5))
            popup = window
            bubble.scaleX = 0.82f
            bubble.scaleY = 0.82f
            bubble.animate().scaleX(1f).scaleY(1f).setDuration(75L).start()
        } catch (_: RuntimeException) {
            window.dismiss()
        }
    }

    fun dismiss() {
        popup?.dismiss()
        popup = null
    }
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
