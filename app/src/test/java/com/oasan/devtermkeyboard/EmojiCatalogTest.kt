package com.oasan.devtermkeyboard

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiCatalogTest {
    @Test fun hasExtendedCategories() {
        val names = EmojiCatalog.groups.map { it.name }
        assertEquals("Recent", names.first())
        assertTrue(names.containsAll(listOf("Smileys", "People", "Animals", "Food",
            "Activities", "Travel", "Objects", "Symbols", "Flags")))
        assertTrue(EmojiCatalog.groups.filter { it.name != "Recent" }
            .all { it.emoji.isNotEmpty() })
    }

    @Test fun containsMultiCodepointEmojis() {
        assertTrue(EmojiCatalog.groups.any { group ->
            group.emoji.any { it == "🏳️‍🌈" }
        })
        assertTrue(EmojiCatalog.groups.flatMap { it.emoji }.size > 450)
    }
}
