package com.oasan.devtermkeyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

internal object KeyboardPreferences {
    private const val STORE = "keyboard_preferences_v2"
    private fun store(ctx: Context) = ctx.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    fun mode(ctx: Context): KeyboardMode = when (store(ctx).getString("mode", "DEVELOPER")) {
        "STANDARD" -> KeyboardMode.STANDARD
        else -> KeyboardMode.DEVELOPER
    }
    fun setMode(ctx: Context, mode: KeyboardMode) =
        store(ctx).edit().putString("mode", mode.name).apply()

    fun theme(ctx: Context): String = store(ctx).getString("theme", "system") ?: "system"
    fun setTheme(ctx: Context, value: String) {
        if (value in THEMES.map { it.first }) store(ctx).edit().putString("theme", value).apply()
    }

    fun isCompact(ctx: Context): Boolean = store(ctx).getBoolean("compact", false)
    fun setCompact(ctx: Context, value: Boolean) = store(ctx).edit().putBoolean("compact", value).apply()
    fun hasHaptics(ctx: Context): Boolean = store(ctx).getBoolean("haptic", false)
    fun setHaptics(ctx: Context, value: Boolean) = store(ctx).edit().putBoolean("haptic", value).apply()
    fun hasKeyPreview(ctx: Context): Boolean = store(ctx).getBoolean("key_preview", true)
    fun setKeyPreview(ctx: Context, value: Boolean) =
        store(ctx).edit().putBoolean("key_preview", value).apply()
    fun predictions(ctx: Context): Boolean = store(ctx).getBoolean("predictions", true)
    fun setPredictions(ctx: Context, value: Boolean) =
        store(ctx).edit().putBoolean("predictions", value).apply()
    fun autocorrect(ctx: Context): Boolean = store(ctx).getBoolean("autocorrect", true)
    fun setAutocorrect(ctx: Context, value: Boolean) =
        store(ctx).edit().putBoolean("autocorrect", value).apply()

    val THEMES = listOf(
        "system" to "System",
        "light" to "Cloud",
        "dark" to "Charcoal",
        "midnight" to "Midnight",
        "mint" to "Mint",
        "slate" to "Slate",
        "rose" to "Rose"
    )

    fun dark(ctx: Context): Boolean = when (theme(ctx)) {
        "dark", "midnight", "slate" -> true
        "light", "mint", "rose" -> false
        else -> (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
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
        private fun p(bg: String, key: String, alt: String, accent: String,
                      fg: String, hint: String, ink: String) = KeyboardPalette(
            Color.parseColor(bg), Color.parseColor(key), Color.parseColor(alt),
            Color.parseColor(accent), Color.parseColor(fg),
            Color.parseColor(hint), Color.parseColor(ink)
        )
        fun forContext(context: Context): KeyboardPalette = when (KeyboardPreferences.theme(context)) {
            "light" -> p("#E8EDF4","#FFFFFF","#CDD8E5","#518EAD","#27384B","#64788D","#FFFFFF")
            "dark" -> p("#262B33","#474F5C","#606C7B","#91CDD4","#F7F9FA","#C2CCD4","#163034")
            "midnight" -> p("#111B2D","#26364A","#34485C","#72DACA","#F0F8FF","#AFC4D7","#0F3332")
            "mint" -> p("#E0F5EF","#FFFFFF","#BCDCD4","#289D87","#18392F","#4F7F72","#FFFFFF")
            "slate" -> p("#232B3A","#364356","#4F6079","#8DAEDC","#F4F7FC","#B7C7DB","#17263F")
            "rose" -> p("#F5E6EA","#FFFFFF","#E5C5D0","#B86B8A","#422D37","#937181","#FFFFFF")
            else -> if (KeyboardPreferences.dark(context)) {
                p("#242A35","#414C5B","#5A6879","#8EC9CD","#F4F7FC","#B8C7D5","#173239")
            } else {
                p("#E7EDF5","#FFFFFF","#CFD9E6","#558DAB","#293B4E","#657B91","#FFFFFF")
            }
        }
    }
}
