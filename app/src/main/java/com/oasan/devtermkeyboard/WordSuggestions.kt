package com.oasan.devtermkeyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import java.util.Locale

/**
 * Small, fully offline English word predictor with conservative spelling
 * corrections. It deliberately does not train on or persist typed text.
 * Not a proprietary Gboard language model.
 */
internal object WordSuggestions {
    private val vocabulary = (
        "the and you that with this have from your they will what there their about would " +
        "which when where who why how could should people think because before after between " +
        "first last next good great better best right left down over under above below through " +
        "again also very more much many some any each every other another these those " +
        "something anything nothing someone everyone person man woman child children family " +
        "friend friends home house school work office today tomorrow yesterday morning evening " +
        "night afternoon year years month months week weeks day days time times hour hours " +
        "hello hi hey thanks thank please welcome yes no okay fine sure maybe sorry really " +
        "therefore however although while since until into onto within without around along " +
        "hello world english language word words text typing keyboard keys key mobile phone " +
        "android app application developer development coding code terminal command computer " +
        "project programming function functions variable variables testing test build release " +
        "feature features settings standard layout custom simple correct correction suggestion " +
        "suggestions spell spelling check checker predict prediction predictive autocorrect " +
        "fast slow quick faster easier easy hard difficult clean modern nice design animation " +
        "smooth new old update version change changed changing open close copy paste cut " +
        "select selected type typed write writing read reading draw drawing create creating " +
        "implement implemented improve improved improve fixing fixed fix error errors bug bugs " +
        "issue issues working works worked work around settings theme color light dark " +
        "start stop finish running done completed current future past real local device " +
        "your my me he she him her our us we it its his them her they their theirs " +
        "to for of in on at by with without and or but if then else so not " +
        "is are am was were be been being do does did done can cannot will would " +
        "might may must should shall have has had having need needed want wanted " +
        "use using used uses make made makes making take took taken taking give gave " +
        "given giving get got getting go goes went going come came coming see saw " +
        "seen seeing know knew known knowing tell told telling say said saying " +
        "ask asked asking answer answered answering help helped helping show showed " +
        "showing look looked looking find found finding leave left leaving " +
        "keep kept keeping let set put run ran running move moved moving " +
        "good bad great excellent awesome amazing nice cool happy sad love lovely " +
        "beautiful beautiful bigger smaller large small short long tall high low " +
        "hot cold warm cool strong weak full empty quiet loud near far " +
        "one two three four five six seven eight nine ten hundred thousand " +
        "somebody anybody everybody nobody somewhere anywhere everywhere nowhere " +
        "game games play player music song songs listen listening audio video " +
        "picture photo image images phone screen displays keyboard keyboarding " +
        "touch press click tap hold shift space enter backspace delete " +
        "audio microphone voice speech recognize recognition language google " +
        "privacy secure private safe security permission permissions " +
        "data file files folder directory folders system network offline online " +
        "connection internet browser website websites email address message messages " +
        "send sent receive received chat chatgpt conversation conversations " +
        "what why when where which who whose whom whose whether because " +
        "could should would through thought though enough good morning thanks " +
        "them then than their there they're its it's you're your " +
        "doesn't don't can't won't aren't isn't we've they have"
    ).split(Regex("\\s+")).filter { it.isNotBlank() }.distinct()
    private val rank = vocabulary.withIndex().associate { it.value to it.index }
    private val commonMistakes = mapOf(
        "teh" to "the", "adn" to "and", "taht" to "that", "recieve" to "receive",
        "woudl" to "would", "shoudl" to "should", "thier" to "their",
        "wierd" to "weird", "becuase" to "because", "definately" to "definitely",
        "dont" to "don't", "cant" to "can't", "doesnt" to "doesn't",
        "youre" to "you're", "im" to "I'm", "ive" to "I've", "thsi" to "this",
        "hte" to "the", "wiht" to "with", "whcih" to "which"
    )
    private val nextWords = mapOf(
        "i" to listOf("am","have","will"),
        "you" to listOf("are","can","have"),
        "thank" to listOf("you","you very much","you so much"),
        "good" to listOf("morning","night","luck"),
        "how" to listOf("are","do","can"),
        "can" to listOf("you","we","i"),
        "what" to listOf("is","are","do"),
        "the" to listOf("best","next","new"),
        "it" to listOf("is","was","will")
    )

    fun allowed(info: EditorInfo?): Boolean {
        val type = info?.inputType ?: return false
        if (type == InputType.TYPE_NULL) return false
        if ((type and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return false
        if ((type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) return false
        return (type and InputType.TYPE_MASK_VARIATION) !in setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_FILTER
        )
    }

    fun suggest(prefix: String, previous: String = ""): List<String> {
        val word = prefix.lowercase(Locale.ROOT)
        if (word.isEmpty()) return nextWords[previous.lowercase(Locale.ROOT)]?.take(3)
            ?: listOf("the", "I", "you")
        if (word.length > 36) return listOf(prefix)
        val matches = mutableListOf<String>()
        matches += prefix
        commonMistakes[word]?.let { matches += matchCase(it, prefix) }
        vocabulary.asSequence().filter { it.startsWith(word) && it != word }
            .take(20).forEach { matches += matchCase(it, prefix) }
        if (matches.size < 3 && word.length >= 3 && word !in rank) {
            vocabulary.asSequence().filter { it.length in (word.length - 1)..(word.length + 1) }
                .filter { singleEditAway(word, it) }
                .take(8).forEach { matches += matchCase(it, prefix) }
        }
        return matches.distinct().take(3)
    }

    fun correct(word: String): String? {
        val lower = word.lowercase(Locale.ROOT)
        if (lower.length < 3 || lower.length > 22 || word.any { it.isDigit() }) return null
        if (lower in rank || word != word.lowercase(Locale.ROOT) &&
            word != word.replaceFirstChar { it.titlecase(Locale.ROOT) }) return null
        commonMistakes[lower]?.let { return matchCase(it, word) }
        val result = vocabulary.asSequence().filter { it.length in (lower.length - 1)..(lower.length + 1) }
            .filter { singleEditAway(lower, it) }
            .minByOrNull { rank[it] ?: Int.MAX_VALUE } ?: return null
        // Only autocorrect to very high-frequency words, reducing false positives.
        return if ((rank[result] ?: Int.MAX_VALUE) < 55) matchCase(result, word) else null
    }

    private fun matchCase(candidate: String, reference: String) =
        if (reference.firstOrNull()?.isUpperCase() == true) {
            candidate.replaceFirstChar { it.titlecase(Locale.ROOT) }
        } else candidate

    /** Zero or one substitution, insertion, removal or adjacent transposition. */
    private fun singleEditAway(a: String, b: String): Boolean {
        if (a == b) return false
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        if (a.length == b.length) {
            val differences = a.indices.filter { a[it] != b[it] }
            return differences.size == 1 || differences.size == 2 &&
                differences[1] == differences[0] + 1 &&
                a[differences[0]] == b[differences[1]] &&
                a[differences[1]] == b[differences[0]]
        }
        val longer = if (a.length > b.length) a else b
        val shorter = if (a.length > b.length) b else a
        var i = 0
        var j = 0
        var skipped = false
        while (i < longer.length && j < shorter.length) {
            if (longer[i] != shorter[j]) {
                if (skipped) return false
                skipped = true
                i++
            } else { i++; j++ }
        }
        return true
    }
}
