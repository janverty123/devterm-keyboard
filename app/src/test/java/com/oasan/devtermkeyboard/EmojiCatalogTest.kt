package com.oasan.devtermkeyboard

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiCatalogTest {
    @Test fun missingExtendedGroupRegression() {
        assertTrue(EmojiCatalog.displayedGroups().any { it.name == "More" })
        assertTrue(EmojiCatalog.allEmoji().contains("🫎"))
    }

    @Test fun keywordSearch() {
        assertTrue(EmojiCatalog.search("fire").contains("🔥"))
        assertTrue(EmojiCatalog.search("philippines").contains("🇵🇭"))
        assertTrue(EmojiCatalog.search("heart").contains("❤️"))
        assertTrue(EmojiCatalog.search("").isEmpty())
    }

    @Test fun noDuplicateGroups() {
        assertEquals(EmojiCatalog.displayedGroups().map { it.name }.distinct().size,
            EmojiCatalog.displayedGroups().size)
    }
}
