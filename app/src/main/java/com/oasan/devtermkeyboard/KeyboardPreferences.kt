package com.oasan.devtermkeyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

internal object KeyboardPreferences {
    private const val STORE = "keyboard_appearance"
    private const val THEME = "theme"
    private const val COMPACT = "compact"
    private const val HAPTIC = "haptic"
    fun theme(context: Context): String =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getString(THEME, "system") ?: "system"
    fun setTheme(context: Context, value: String) {
        if (value !in listOf("system", "light", "dark")) return
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().putString(THEME, value).apply()
    }
    fun isCompact(context: Context): Boolean =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getBoolean(COMPACT, false)
    fun setCompact(context: Context, value: Boolean) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().putBoolean(COMPACT, value).apply()
    }
    fun hasHaptics(context: Context): Boolean =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getBoolean(HAPTIC, false)
    fun setHaptics(context: Context, value: Boolean) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().putBoolean(HAPTIC, value).apply()
    }
    fun dark(context: Context): Boolean = when (theme(context)) {
        "dark" -> true
        "light" -> false
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }
}

internal data class KeyboardPalette(
    val background: Int,
    val key: Int,
    val auxiliary: Int,
    val selected: Int,
    val foreground: Int,
    val secondary: Int,
    val accentText: Int
) {
    companion object {
        fun forContext(context: Context) = if (KeyboardPreferences.dark(context)) KeyboardPalette(
            Color.rgb(27, 32, 40), Color.rgb(53, 62, 74),
            Color.rgb(78, 91, 108), Color.rgb(100, 211, 192),
            Color.rgb(250, 251, 253), Color.rgb(166, 185, 202), Color.rgb(12, 44, 42)
        ) else KeyboardPalette(
            Color.rgb(219, 226, 236), Color.WHITE, Color.rgb(185, 199, 216),
            Color.rgb(68, 145, 150), Color.rgb(35, 47, 61),
            Color.rgb(76, 96, 118), Color.WHITE
        )
    }
}
