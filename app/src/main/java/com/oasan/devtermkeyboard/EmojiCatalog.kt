package com.oasan.devtermkeyboard

/** Android already renders Unicode emoji using its system color emoji font.
 *  Separate SVG files would break multi-codepoint emoji and skin-tone sequences.
 */
internal object EmojiCatalog {
    data class Group(val name: String, val emoji: List<String>)
    val groups = listOf(
        Group("Recent", emptyList()),
        Group("Smileys",listOf("🫠","🫣","🫤","🫨","🥸","😶‍🌫️","😮‍💨","😵‍💫","❤️‍🔥","❤️‍🩹",
            "😀","😃","😄","😁","😆","😅","😂","🤣","🥲","☺️","😊","😇","🙂",
            "🙃","😉","😌","😍","🥰","😘","😗","😙","😚","😋","😛","😜",
            "🤪","😝","🤑","🤗","🤭","🫢","🤫","🤔","🫡","🤐","🤨","😐",
            "😑","😶","🫥","😏","😒","🙄","😬","🤥","😔","😪","🤤","😴",
            "😷","🤒","🤕","🤢","🤧","🥵","🥶","🥴","😵","🤯","🥳","😎",
            "🤓","🧐","😕","😟","🙁","☹️","😮","😯","😲","😳","🥺","🥹",
            "😦","😧","😨","😰","😥","😢","😭","😱","😖","😣","😞","😓",
            "😩","😫","🥱","😤","😡","😠","🤬","😈","👿","💀","👻","👽"
        )),
        Group("People",listOf("🧑‍🚀","👩‍🚀","👨‍🚀","🧑‍🚒","🧑‍⚕️","🧑‍🏫","🧑‍🎤","🧑‍🍳","🧑‍🔧","🧑‍✈️","🧑‍⚖️","🧑‍🌾","👩‍❤️‍👩","👨‍❤️‍👨","👩‍👩‍👧","👨‍👨‍👧","👍🏻","👍🏼","👍🏽","👍🏾","👍🏿","👋🏻","👋🏼","👋🏽","👋🏾","👋🏿","🙏🏻","🙏🏼","🙏🏽","🙏🏾","🙏🏿",
            "👋","🤚","🖐️","✋","🖖","🫱","🫲","🫳","🫴","👌","🤌","🤏",
            "✌️","🤞","🫰","🤟","🤘","🤙","👈","👉","👆","👇","☝️","👍",
            "👎","✊","👊","🤛","🤜","👏","🙌","🫶","👐","🤲","🙏","💪",
            "🦾","🦿","🦵","🦶","👂","🦻","👃","🧠","🫀","🫁","🦷","👀",
            "👅","👄","🧑","👨","👩","👶","🧒","👦","👧","👴","👵","👷",
            "👮","🧑‍💻","👩‍💻","👨‍💻","🧑‍🎓","🧑‍🔬"
        )),
        Group("Animals",listOf("🦦","🦫","🦥","🦨","🦡","🦔","🦭","🦩","🦚","🦜","🦢","🦤","🦪","🪸","🪼","🪻","🪷","🪴","🌵","🌴","🌱","🌿","🍁","🍂","🍃",
            "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷",
            "🐸","🐵","🙈","🙉","🙊","🐒","🐔","🐧","🐦","🐤","🦆","🦅",
            "🦉","🦇","🐺","🐗","🐴","🦄","🐝","🐛","🦋","🐌","🐞",
            "🐜","🦟","🕷️","🐢","🐍","🦎","🦖","🐙","🦑","🦐","🦀",
            "🐡","🐠","🐟","🐬","🐳","🦈","🐊","🦓","🦍","🦧","🐘",
            "🦛","🦏","🐪","🦒","🦘","🦬","🐃","🐂","🐄","🐎","🐖"
        )),
        Group("Food",listOf("🫘","🥜","🌰","🫒","🫚","🫛","🫑","🫓","🥞","🧇","🥧","🍮","🍭","🥤","🧋","🧃","🧉","🍵","🫖","🍶","🍷","🍸","🍹","🍺",
            "🍏","🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍈","🍒",
            "🍑","🥭","🍍","🥥","🥝","🍅","🍆","🥑","🥦","🥬","🥒","🌶️",
            "🌽","🥕","🧄","🧅","🥔","🍠","🥐","🥯","🍞","🥖","🥨",
            "🧀","🥚","🍳","🥓","🥩","🍗","🍖","🌭","🍔","🍟","🍕",
            "🥪","🥙","🌮","🌯","🫔","🥗","🥘","🍝","🍜","🍲","🍛",
            "🍣","🍱","🥟","🍤","🍙","🍚","🍘","🍥","🥠","🍢","🍡",
            "🍦","🍧","🍨","🍩","🍪","🎂","🍰","🧁","🍫","🍬","☕"
        )),
        Group("Activities", listOf(
            "⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🪀","🏓",
            "🏸","🏒","🏑","🥍","🏏","🥅","⛳","🪁","🏹","🎣","🤿","🥊",
            "🥋","🎽","🛹","🛼","🛷","⛸️","🥌","🎿","⛷️","🏂","🏋️",
            "🤸","⛹️","🏌️","🏄","🚣","🏊","🚴","🧗","🤼","🤽","🧘",
            "🎳","🎯","🎮","🕹️","🎰","🧩","♟️","🎲","🃏","🀄","🎴",
            "🎨","🎭","🎬","🎤","🎧","🎼","🎹","🥁","🎷","🎺","🎸","🎻",
            "🎪","🎟️","🎫","🏆","🥇","🥈","🥉","🏅","🎖️","🎀","🎁",
            "🎂","🎈","🎉","🎊","🎇","🎆","🧨","🪄","🪅","🪩"
        )),
        Group("Travel",listOf("🚈","🚊","🚉","🚋","🚍","🚔","🚖","🚘","🚝","🚞","🚟","🚠","🚡","🚢","🛶","⛴️","🛥️","🚤","🛟","🛰️","🏰","🏯","🏟️","🏛️","⛪","🕌","🕍","⛩️","🛕","🗼","🗽","🗿","🏔️","⛰️","🌋","🏕️","🏖️","🏜️","🏝️","🏞️","🌅","🌄","🌃","🌆","🌉","🌌",
            "🚗","🚕","🚙","🚌","🚎","🏎️","🚓","🚑","🚒","🚚","🚛","🚜",
            "🛵","🏍️","🚲","🛴","🚏","🚆","🚇","🚂","🚄","✈️","🛫",
            "🛬","🚀","🛸","🚁","⛵","🚢","🛳️","⚓","🗺️","🌍","🌎",
            "🌏","🌐","🌙","🌚","🌞","⭐","🌟","🌈","☀️","⛅","🌧️",
            "❄️","🔥","💧","🌊","🌸","🌹","🌻","🌲","🌳","🍀","☘️"
        )),
        Group("Objects", listOf(
            "📞","☎️","📟","📠","🔋","🪫","🔌","🖨️","📷","📸","📹","🎥",
            "📽️","🎞️","📺","📻","🎙️","🎚️","🎛️","📡","🧭","⏱️","⏲️",
            "⏰","🕰️","⌚","📱","💻","🖥️","⌨️","🖱️","🖲️","💽","💾",
            "💿","📀","🧮","🕯️","🔦","🪔","🧯","🛢️","🪙","💰","💵",
            "💶","💷","💴","💳","💎","⚖️","🪜","🧰","🔨","🪓","⚒️",
            "🛠️","⛏️","🔩","⚙️","🧲","🪚","🪛","🔫","🪤","🧪","🧫",
            "🧬","🔬","🔭","📘","📗","📙","📚","📖","📕","📓","📒",
            "📔","📃","📜","📄","📰","🗞️","📑","🔖","🏷️","✉️","📧",
            "📨","📩","📤","📥","📦","📫","📪","📬","📭","🗳️","✏️",
            "✒️","🖋️","🖊️","🖌️","🖍️","📝","📌","📍","📎","🖇️",
            "📏","📐","✂️","🗃️","🗄️","🗑️","🔒","🔓","🔐","🔑"
        )),
        Group("Symbols",listOf("🩷","🩵","🩶","💓","💞","💠","💥","💫","💬","🗯️","💭","💤","🔴","🟠","🟡","🟢","🔵","🟣","⚫","⚪","🟤","🔺","🔻","🔶","🔷","🔸","🔹","🔳","🔲","▪️","▫️","◾","◽","◼️","◻️","⬛","⬜","🟧","🟨","🟩","🟦","🟪","🟫",
            "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","💕","💖",
            "💗","💘","💝","💟","✅","❌","❎","✔️","✖️","➕","➖","➗",
            "➰","➿","‼️","⁉️","❓","❔","❕","❗","💯","💢","♻️","⚠️",
            "🚫","🔞","🔔","🔕","🔒","🔓","🔑","🗝️","🛠️","⚙️","🔧",
            "💻","⌨️","🖥️","🖱️","📱","📂","📁","📄","📊","📈","📉",
            "💡","📌","🔍","🔎","✉️","📨","📬","🆗","🆒","🆕","🆓"
        )),
        Group("Flags", listOf(
            "🏳️","🏴","🏁","🚩","🏳️‍🌈","🏳️‍⚧️","🇵🇭","🇺🇸","🇬🇧","🇨🇦",
            "🇦🇺","🇳🇿","🇯🇵","🇰🇷","🇨🇳","🇮🇳","🇮🇩","🇲🇾","🇸🇬","🇹🇭",
            "🇻🇳","🇹🇼","🇭🇰","🇩🇪","🇫🇷","🇪🇸","🇮🇹","🇵🇹","🇳🇱","🇧🇪",
            "🇨🇭","🇸🇪","🇳🇴","🇩🇰","🇫🇮","🇮🇸","🇮🇪","🇵🇱","🇺🇦","🇬🇷",
            "🇹🇷","🇦🇪","🇸🇦","🇶🇦","🇮🇱","🇪🇬","🇲🇦","🇳🇬","🇰🇪","🇿🇦",
            "🇧🇷","🇦🇷","🇨🇱","🇨🇴","🇵🇪","🇲🇽","🇨🇷","🇨🇺","🇯🇲","🇧🇸",
            "🇵🇰","🇧🇩","🇳🇵","🇱🇰","🇲🇳","🇰🇿","🇷🇴","🇭🇺","🇦🇹","🇨🇿"
        ))
    )

    /** Newer Unicode sequences. Actual artwork is supplied by the system emoji font. */
    private val extras = listOf(
        "🫩","🫆","🫟","🫠","🫣","🫤","🫨","🫪","🫫","🫬","🫯",
        "🫱🏻","🫱🏼","🫱🏽","🫱🏾","🫱🏿",
        "🫲🏻","🫲🏼","🫲🏽","🫲🏾","🫲🏿",
        "🫶🏻","🫶🏼","🫶🏽","🫶🏾","🫶🏿",
        "🦻🏻","🦻🏼","🦻🏽","🦻🏾","🦻🏿",
        "🧑‍🦯","🧑‍🦽","🧑‍🦼","🧑‍🍼","🧑‍🤝‍🧑",
        "🫎","🫏","🪿","🪼","🪽","🪻","🪷","🫚","🫛","🪸","🪺","🪹",
        "🪏","🪉","🪊","🪇","🪈","🪕","🪘","🪗","🪆","🪅","🪩","🪬",
        "🪭","🪮","🪯","🪝","🪜","🪞","🪟","🪠","🪡","🪢","🪣","🪤",
        "🩻","🩼","🩹","🩺","🩸","🛜","🫧","🫗","🫙","🫖","🫕","🫔",
        "🩷","🩵","🩶","❤️‍🔥","❤️‍🩹","🏳️‍⚧️","🏳️‍🌈","🏴‍☠️","👁️‍🗨️",
        "🙂‍↕️","🙂‍↔️","🐦‍🔥","🍋‍🟩","⛓️‍💥","🧑‍🧑‍🧒","🧑‍🧑‍🧒‍🧒"
    ).distinct()
    fun displayedGroups(): List<Group> = groups + Group("More", extras)
    fun allEmoji(): List<String> = displayedGroups().flatMap { it.emoji }.distinct()

    /** Common English aliases complement Android's Unicode character names. */
    private val aliases = mapOf(
        "😂" to "laugh laughing tears funny lol", "🤣" to "rolling laugh rofl",
        "🥹" to "tears proud touched", "🥲" to "happy sad tear",
        "🥰" to "love hearts affection", "😍" to "love heart eyes",
        "😘" to "kiss love", "😇" to "angel innocent", "🥺" to "pleading please",
        "😭" to "cry crying sob sad", "😢" to "cry tear sad",
        "🤬" to "swearing angry curse", "😡" to "angry mad",
        "😎" to "cool sunglasses", "🤓" to "nerd glasses",
        "🔥" to "fire hot lit flame", "💯" to "hundred perfect score",
        "❤️" to "love heart red", "💔" to "broken heart breakup",
        "🩷" to "pink heart love", "💕" to "two hearts love",
        "👍" to "thumbs up yes okay like", "👎" to "thumbs down dislike no",
        "🙏" to "prayer please thanks folded hands",
        "👏" to "clap applause", "👋" to "wave waving hi hello goodbye",
        "🤝" to "handshake deal agreement", "💪" to "muscle strong flex",
        "🐱" to "cat kitten pet", "🐶" to "dog puppy pet",
        "🐻" to "bear", "🐼" to "panda", "🐸" to "frog",
        "🌹" to "rose flower love", "🌸" to "flower cherry blossom",
        "🌞" to "sun sunny smile", "🌧️" to "rain rainy",
        "🌈" to "rainbow", "⚡" to "lightning electricity",
        "🎉" to "celebrate celebration party confetti", "🎂" to "birthday cake",
        "🎁" to "gift present", "🥳" to "party celebrate",
        "🍕" to "pizza", "🍔" to "hamburger burger", "🍟" to "fries chips",
        "☕" to "coffee tea hot drink", "🍵" to "tea cup",
        "🍎" to "apple red fruit", "🍌" to "banana fruit",
        "🚀" to "rocket space launch", "✈️" to "plane airplane flight travel",
        "🚗" to "car automobile", "🏠" to "home house",
        "💻" to "laptop computer coding code", "⌨️" to "keyboard",
        "🖥️" to "computer desktop monitor", "📱" to "mobile phone",
        "⚙️" to "settings gear", "🔧" to "tool wrench",
        "✅" to "check success done yes", "❌" to "cross x wrong no",
        "⭐" to "star favorite", "🌟" to "star shining sparkle",
        "🇵🇭" to "philippines filipino flag", "🇺🇸" to "united states usa american flag",
        "🇯🇵" to "japan japanese flag", "🇰🇷" to "korea korean flag",
        "🏳️‍🌈" to "rainbow pride lgbt flag",
        "🏳️‍⚧️" to "transgender flag pride"
    )

    /** Search both curated aliases and official Unicode character names on device. */
    fun search(query: String, maxResults: Int = 180): List<String> {
        val terms = query.trim().lowercase(java.util.Locale.ROOT).split(
            Regex("\\s+")
        ).filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        val indexed = displayedGroups().flatMap { group ->
            group.emoji.map { emoji -> emoji to group.name }
        }.distinctBy { it.first }
        val matches = indexed.mapNotNull { (emoji, category) ->
            val desc = aliases[emoji].orEmpty() + " " + unicodeName(emoji) + " " + category.lowercase()
            if (!terms.all { desc.contains(it) }) return@mapNotNull null
            val alias = aliases[emoji].orEmpty()
            val score = when {
                alias.startsWith(terms[0]) -> 0
                alias.contains(terms[0]) -> 1
                unicodeName(emoji).startsWith(terms[0]) -> 2
                else -> 3
            }
            Triple(emoji, score, category)
        }
        return matches.sortedWith(compareBy<Triple<String,Int,String>> { it.second }
            .thenBy { it.third }).take(maxResults).map { it.first }
    }

    private fun unicodeName(value: String): String {
        val names = StringBuilder()
        var offset = 0
        while (offset < value.length) {
            val codePoint = Character.codePointAt(value, offset)
            try {
                val name = Character.getName(codePoint)
                if (name != null) names.append(name.lowercase(java.util.Locale.ROOT)).append(' ')
            } catch (_: IllegalArgumentException) { }
            offset += Character.charCount(codePoint)
        }
        return names.toString()
    }
}
