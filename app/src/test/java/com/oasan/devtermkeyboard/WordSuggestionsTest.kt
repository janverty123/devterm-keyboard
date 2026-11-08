package com.oasan.devtermkeyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class WordSuggestionsTest {
    @Test fun commonMisspelling() {
        assertEquals("the", WordSuggestions.correct("teh"))
        assertEquals("Their", WordSuggestions.correct("Thier"))
    }
    @Test fun suggestionsForPrefix() {
        assertTrue(WordSuggestions.suggest("key").contains("keyboard"))
    }
    @Test fun doNotChangeKnownWord() {
        assertNull(WordSuggestions.correct("project"))
    }
}
