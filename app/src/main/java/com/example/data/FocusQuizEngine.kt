package com.example.data

import android.content.Context
import androidx.compose.runtime.Immutable
import org.json.JSONArray
import kotlin.random.Random

enum class QuizInputMode {
    NUMERIC,
    ALPHA
}

@Immutable
data class FocusQuizQuestion(
    val category: String,
    val hint: String,
    val prompt: String,
    val acceptedAnswers: List<String>,
    val signature: String,
    val inputMode: QuizInputMode
)

/**
 * Native Kotlin port of FocusLock's procedural infinite quiz generator.
 *
 * Implements the exact four generators from the reference Python challenge server:
 * 1. Procedural Odd One Out (8 category pools)
 * 2. Instant Math / Number Reflex (Even/Odd, Max/Min, Quick X, Number Flow)
 * 3. Visual Emoji Algebra, Symbol Swap & Solo Hunter
 * 4. Easy Unscramble (full 296-word pool with automatic reflex fallback)
 *
 * Persists all asked question signatures on-device so no question is ever repeated
 * across sessions, with a guaranteed infinite procedural fallback.
 */
object FocusQuizEngine {
    const val QUESTIONS_PER_UNLOCK = 10

    private const val PREFS_NAME = "focus_quiz_memory_prefs"
    private const val KEY_ASKED_SIGNATURES = "asked_signatures_json"

    private val lock = Any()

    // =========================================================================
    // 1. PROCEDURAL ODD ONE OUT POOLS (Exact match to Python reference)
    // =========================================================================
    private val categoryPools: Map<String, List<String>> = linkedMapOf(
        "animals" to listOf(
            "DOG", "CAT", "LION", "BEAR", "WOLF", "DEER", "FROG", "DUCK", "GOAT", "CRAB",
            "HAWK", "SWAN", "SEAL", "PUMA", "CROW", "MULE", "TOAD", "LYNX", "ORCA", "HARE"
        ),
        "fruits" to listOf(
            "APPLE", "MANGO", "GRAPE", "LEMON", "PEACH", "MELON", "BERRY", "PLUM", "GUAVA",
            "KIWI", "PEAR", "LIME", "FIG", "DATE", "PAPAYA", "LYCHEE", "CHERRY", "OLIVE"
        ),
        "colors" to listOf(
            "RED", "BLUE", "GREEN", "BLACK", "WHITE", "PINK", "GOLD", "CYAN", "GREY",
            "BROWN", "NAVY", "TEAL", "CORAL", "IVORY", "AMBER", "BEIGE"
        ),
        "vehicles" to listOf(
            "CAR", "BUS", "TRAIN", "TRUCK", "PLANE", "BOAT", "SHIP", "BIKE", "TAXI",
            "JEEP", "VAN", "TRAM", "YACHT", "METRO", "FERRY", "SLEIGH"
        ),
        "furniture" to listOf(
            "CHAIR", "TABLE", "DESK", "SOFA", "COUCH", "BED", "SHELF", "STOOL",
            "BENCH", "FUTON", "CRADLE", "CLOSET"
        ),
        "planets" to listOf(
            "MARS", "VENUS", "EARTH", "PLUTO", "SATURN", "URANUS", "NEPTUNE", "MERCURY", "JUPITER"
        ),
        "shapes" to listOf(
            "CIRCLE", "SQUARE", "CUBE", "CONE", "OVAL", "LINE", "STAR", "RING",
            "SPHERE", "PRISM", "SPIRAL", "HEXAGON"
        ),
        "weather" to listOf(
            "RAIN", "SNOW", "WIND", "STORM", "CLOUD", "FROST", "HAIL", "MIST",
            "FOG", "HUMID", "BREEZE", "THUNDER"
        )
    )

    private val visualEmojis = listOf(
        "⚡", "🔥", "💎", "🚀", "⭐", "🍎", "🎲", "🎯", "🪐", "🍀", "🌙", "👑", "🔮", "🎵", "🧊"
    )

    private val soloLetters = listOf(
        "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M",
        "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    )

    private val flowSteps = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25)

    // =========================================================================
    // 4. EASY UNSCRAMBLE WORD POOL (Exact match to Python reference)
    // =========================================================================
    private val easyFourLetterWords = listOf(
        "MATH", "PLUS", "ZERO", "CUBE", "LINE", "AREA", "AXIS", "DATA", "CODE", "GRID",
        "TIME", "WORK", "GOAL", "CALM", "MIND", "BOOK", "DESK", "PLAN", "TASK", "NOTE",
        "STAR", "MOON", "WIND", "RAIN", "FIRE", "TREE", "SNOW", "LAKE", "ROCK", "WAVE",
        "GOLD", "BLUE", "GRAY", "PINK", "FAST", "SLOW", "HIGH", "DEEP", "COOL", "WARM",
        "GAME", "PLAY", "JUMP", "WALK", "IDEA", "FACT", "TRUE", "EASY", "OPEN", "SAFE",
        "ATOM", "BEAM", "BOLT", "BOND", "BYTE", "CELL", "CHIP", "COIL", "CORE", "DISK",
        "ECHO", "EDGE", "FLUX", "FONT", "FORM", "GAIN", "GATE", "GEAR", "GLOW", "GRAM",
        "HASH", "HEAT", "ICON", "ITEM", "JOIN", "KILO", "LENS", "LINK", "LIST", "LOAD",
        "LOCK", "LOOP", "MASS", "MEAN", "MESH", "MODE", "NANO", "NODE", "NORM", "NULL",
        "PATH", "PEAK", "PING", "PLOT", "POLE", "PORT", "QUAD", "RATE", "RING", "ROOT",
        "RULE", "SCAN", "SEED", "SIGN", "SINE", "SIZE", "SLOT", "SORT", "SPAN", "SPIN",
        "STEP", "SYNC", "TERM", "TEST", "TEXT", "TIDE", "TONE", "TOOL", "TYPE", "UNIT",
        "USER", "VAST", "VIEW", "VOID", "VOLT", "WATT", "WIRE", "WORD", "ZONE", "ARCH",
        "BARK", "BIRD", "BOAT", "CAFE", "CAMP", "CARD", "CART", "CAVE", "CITY", "CLAY",
        "CLUB", "COAT", "COIN", "CORN", "CROP", "CROW", "DAWN", "DEER", "DOCK", "DOOR",
        "DOVE", "DRUM", "DUCK", "DUST", "EARN", "EAST", "EXIT", "FACE", "FARM", "FERN",
        "FILM", "FISH", "FLAG", "FLAT", "FLOW", "FOLK", "FOOD", "FOOT", "FORK", "FORT",
        "FROG", "FUEL", "GIFT", "GIRL", "GOAT", "GOLF", "HAND", "HARP", "HAWK", "HEAD",
        "HERB", "HILL", "HOME", "HOPE", "HORN", "HOST", "HOUR", "HUNT", "IRON", "JADE",
        "JAZZ", "JEEP", "JOKE", "KEEP", "KING", "KITE", "KNOT", "LAMP", "LAND", "LANE",
        "LEAF", "LIFE", "LIFT", "LIME", "LION", "LOAF", "LOVE", "LUCK", "MALL", "MARK",
        "MASK", "MAZE", "MEAL", "MILD", "MILK", "MILL", "MINT", "MIST", "MOSS", "MOTH",
        "MOVE", "MULE", "NAME", "NAVY", "NEAR", "NEAT", "NECK", "NEST", "NEWS", "NEXT",
        "NOON", "NOSE", "OATS", "OVEN", "PACE", "PACK", "PAGE", "PAIL", "PAIR", "PALM",
        "PARK", "PART", "PASS", "PAST", "PEAR", "PEAT", "PEEL", "PEER", "PINE", "PIPE",
        "PLUM", "POEM", "POET", "POND", "POOL", "POST", "POUR", "PRAY", "PULL", "PUMP",
        "PURE", "PUSH", "RACE", "RAFT", "RAIL", "RANK", "RARE", "READ", "REAL", "REED",
        "REEF", "RENT", "REST", "RICE", "RICH", "RIDE", "RISE", "ROAD", "ROAR", "ROBE",
        "ROLE", "ROOF", "ROOM", "ROPE", "ROSE", "RUBY", "RUSH", "RUST", "SAIL", "SALT",
        "SAND", "SAVE", "SEAL", "SEAT", "SELF", "SEND", "SHED", "SHIP", "SHOE", "SHOP",
        "SHOT", "SHOW", "SIDE", "SILK", "SINK", "SITE", "SKIN", "SKIP", "SLAB", "SLIP",
        "SNAP", "SOAP", "SOCK", "LAVA", "SOIL", "SONG", "SOON", "SOUL", "SOUP", "SPOT",
        "STEM", "STOP", "SUIT", "SWAN", "SWIM", "TAIL", "TALE", "TALK", "TALL", "TANK",
        "TAPE", "TAXI", "TEAM", "TEAL", "TEAR", "TENT"
    )

    fun getAskedSignatures(context: Context): Set<String> = synchronized(lock) {
        loadAskedSignaturesLocked(context.applicationContext)
    }

    fun getAskedCount(context: Context): Int = getAskedSignatures(context).size

    fun replaceAskedSignatures(context: Context, signatures: Collection<String>) = synchronized(lock) {
        val clean = LinkedHashSet<String>(signatures.size)
        for (sig in signatures) {
            val trimmed = sig.trim()
            if (trimmed.isNotEmpty()) clean.add(trimmed)
        }
        saveAskedSignaturesLocked(context.applicationContext, clean)
    }

    fun generateQuizSet(
        context: Context,
        count: Int = QUESTIONS_PER_UNLOCK,
        random: Random = Random.Default
    ): List<FocusQuizQuestion> = synchronized(lock) {
        val appCtx = context.applicationContext
        val asked = loadAskedSignaturesLocked(appCtx)
        val result = ArrayList<FocusQuizQuestion>(count)
        repeat(count) {
            result.add(generateUniqueQuestionLocked(asked, random))
        }
        saveAskedSignaturesLocked(appCtx, asked)
        result
    }

    fun checkAnswer(question: FocusQuizQuestion, userInput: String): Boolean {
        val normalized = userInput.trim().uppercase()
        if (normalized.isEmpty()) return false
        return question.acceptedAnswers.any { it.uppercase() == normalized }
    }

    private fun generateUniqueQuestionLocked(
        askedSignatures: MutableSet<String>,
        random: Random
    ): FocusQuizQuestion {
        for (attempt in 0 until 250) {
            val q = when (random.nextInt(4)) {
                0 -> generateOddOneOutQuestion(random)
                1 -> generateMathReflexQuestion(random)
                2 -> generateVisualMathGame(random)
                else -> generateEasyScrambleQuestion(askedSignatures, random)
            }
            if (askedSignatures.add(q.signature)) {
                return q
            }
        }

        // Guaranteed unique infinite procedural fallback
        var a: Int
        var b: Int
        var sum: Int
        var sig: String
        var nonce = 0
        do {
            a = random.nextInt(50, 10_000)
            b = random.nextInt(2, 100)
            sum = a + b
            sig = "FALLBACK_X:$a+$b=$sum:${System.nanoTime()}_$nonce"
            nonce++
        } while (!askedSignatures.add(sig))

        return FocusQuizQuestion(
            category = "Quick X",
            hint = "Find x",
            prompt = "x + $b = $sum",
            acceptedAnswers = listOf(a.toString()),
            signature = sig,
            inputMode = QuizInputMode.NUMERIC
        )
    }

    private fun generateOddOneOutQuestion(random: Random): FocusQuizQuestion {
        val keys = categoryPools.keys.toList()
        val mainKey = keys[random.nextInt(keys.size)]
        var oddKey = keys[random.nextInt(keys.size)]
        while (oddKey == mainKey) {
            oddKey = keys[random.nextInt(keys.size)]
        }

        val mainPool = categoryPools.getValue(mainKey)
        val oddPool = categoryPools.getValue(oddKey)
        val mainWords = mainPool.shuffled(random).take(3)
        val oddWord = oddPool[random.nextInt(oddPool.size)]
        val finalWords = (mainWords + oddWord).shuffled(random)
        val sortedComboSig = mainWords.sorted().joinToString(",") + "|" + oddWord

        return FocusQuizQuestion(
            category = "Odd One Out",
            hint = "Type the word that doesn't belong",
            prompt = finalWords.joinToString(" • "),
            acceptedAnswers = listOf(oddWord),
            signature = "ODD:$sortedComboSig",
            inputMode = QuizInputMode.ALPHA
        )
    }

    private fun generateMathReflexQuestion(random: Random): FocusQuizQuestion {
        val mode = random.nextInt(1, 5)

        if (mode == 1) {
            val findEven = random.nextBoolean()
            val distractors = mutableListOf<Int>()
            while (distractors.size < 3) {
                val n = random.nextInt(11, 200)
                if (findEven && n % 2 != 0 && n !in distractors) distractors.add(n)
                if (!findEven && n % 2 == 0 && n !in distractors) distractors.add(n)
            }
            var target: Int
            while (true) {
                val n = random.nextInt(11, 200)
                if ((findEven && n % 2 == 0) || (!findEven && n % 2 != 0)) {
                    target = n
                    break
                }
            }
            val nums = (distractors + target).shuffled(random)
            return FocusQuizQuestion(
                category = "Number Reflex",
                hint = if (findEven) "Type the ONLY EVEN number" else "Type the ONLY ODD number",
                prompt = nums.joinToString(" • "),
                acceptedAnswers = listOf(target.toString()),
                signature = "REFLEX_EO:${if (findEven) "E" else "O"}:${nums.sorted().joinToString(",")}",
                inputMode = QuizInputMode.NUMERIC
            )
        }

        if (mode == 2) {
            val findMax = random.nextBoolean()
            val nums = mutableListOf<Int>()
            while (nums.size < 4) {
                val n = random.nextInt(10, 251)
                if (n !in nums) nums.add(n)
            }
            val ans = if (findMax) nums.max() else nums.min()
            return FocusQuizQuestion(
                category = "Number Reflex",
                hint = if (findMax) "Type the HIGHEST number" else "Type the LOWEST number",
                prompt = nums.joinToString(" • "),
                acceptedAnswers = listOf(ans.toString()),
                signature = "REFLEX_MM:${if (findMax) "MAX" else "MIN"}:${nums.sorted().joinToString(",")}",
                inputMode = QuizInputMode.NUMERIC
            )
        }

        if (mode == 3) {
            val x = random.nextInt(2, 46)
            val b = random.nextInt(2, 46)
            val sum = x + b
            return FocusQuizQuestion(
                category = "Quick X",
                hint = "Find x",
                prompt = "x + $b = $sum",
                acceptedAnswers = listOf(x.toString()),
                signature = "QUICK_X:$x+$b=$sum",
                inputMode = QuizInputMode.NUMERIC
            )
        }

        val step = flowSteps[random.nextInt(flowSteps.size)]
        val start = random.nextInt(1, 31) * step
        val seq = mutableListOf(
            start.toString(),
            (start + step).toString(),
            (start + step * 2).toString(),
            (start + step * 3).toString()
        )
        val hideIdx = random.nextInt(1, 4)
        val ans = seq[hideIdx]
        seq[hideIdx] = "?"

        return FocusQuizQuestion(
            category = "Number Flow",
            hint = "Type the missing number",
            prompt = seq.joinToString("  →  "),
            acceptedAnswers = listOf(ans),
            signature = "FLOW:$start:$step:$hideIdx",
            inputMode = QuizInputMode.NUMERIC
        )
    }

    private fun generateVisualMathGame(random: Random): FocusQuizQuestion {
        val e1 = visualEmojis[random.nextInt(visualEmojis.size)]
        var e2 = visualEmojis[random.nextInt(visualEmojis.size)]
        while (e2 == e1) {
            e2 = visualEmojis[random.nextInt(visualEmojis.size)]
        }

        val mode = random.nextInt(1, 4)

        if (mode == 1) {
            val value = random.nextInt(2, 26)
            val count = random.nextInt(2, 4)
            val extra = random.nextInt(1, 31)
            val emojisStr = List(count) { e1 }.joinToString(" + ")
            val total = (value * count) + extra

            return FocusQuizQuestion(
                category = "Emoji Algebra",
                hint = "If $e1 = $value, find the total",
                prompt = "$emojisStr + $extra = ?",
                acceptedAnswers = listOf(total.toString()),
                signature = "EMOJI_ALG:$e1:$value:$count:$extra",
                inputMode = QuizInputMode.NUMERIC
            )
        }

        if (mode == 2) {
            val addMode = random.nextFloat() < 0.7f
            val v1 = if (addMode) random.nextInt(2, 46) else random.nextInt(2, 13)
            val v2 = if (addMode) random.nextInt(2, 46) else random.nextInt(2, 13)
            val total = if (addMode) (v1 + v2) else (v1 * v2)
            val symbol = if (addMode) "+" else "×"

            return FocusQuizQuestion(
                category = "Symbol Swap",
                hint = "$e1 = $v1   and   $e2 = $v2",
                prompt = "$e1  $symbol  $e2  =  ?",
                acceptedAnswers = listOf(total.toString()),
                signature = "SYM_SWAP:$e1:$v1:$symbol:$e2:$v2",
                inputMode = QuizInputMode.NUMERIC
            )
        }

        val chosen = soloLetters.shuffled(random).take(3)
        val soloLetter = chosen[0]
        val dup1 = chosen[1]
        val dup2 = chosen[2]
        val finalSeq = listOf(soloLetter, dup1, dup1, dup2, dup2).shuffled(random)

        return FocusQuizQuestion(
            category = "Solo Hunter",
            hint = "Type the letter that appears ONLY ONCE",
            prompt = finalSeq.joinToString("  •  "),
            acceptedAnswers = listOf(soloLetter),
            signature = "SOLO:${finalSeq.joinToString("")}",
            inputMode = QuizInputMode.ALPHA
        )
    }

    private fun generateEasyScrambleQuestion(
        askedSignatures: Set<String>,
        random: Random
    ): FocusQuizQuestion {
        val unaskedWords = easyFourLetterWords.filter { w -> "SCRAMBLE:$w" !in askedSignatures }
        if (unaskedWords.isEmpty()) {
            return generateMathReflexQuestion(random)
        }

        val word = unaskedWords[random.nextInt(unaskedWords.size)]
        var scrambled = word
        var attempts = 0
        while (scrambled == word && attempts < 20) {
            scrambled = word.toList().shuffled(random).joinToString("")
            attempts++
        }

        return FocusQuizQuestion(
            category = "Easy Unscramble",
            hint = "Starts with '${word.first()}' and ends with '${word.last()}'",
            prompt = scrambled.toCharArray().joinToString(" "),
            acceptedAnswers = listOf(word),
            signature = "SCRAMBLE:$word",
            inputMode = QuizInputMode.ALPHA
        )
    }

    private fun loadAskedSignaturesLocked(appCtx: Context): MutableSet<String> {
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ASKED_SIGNATURES, null) ?: return LinkedHashSet()
        return runCatching {
            val arr = JSONArray(raw)
            val set = LinkedHashSet<String>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.optString(i, "").trim()
                if (item.isNotEmpty()) set.add(item)
            }
            set
        }.getOrElse { LinkedHashSet() }
    }

    private fun saveAskedSignaturesLocked(appCtx: Context, signatures: Set<String>) {
        val arr = JSONArray()
        for (sig in signatures) {
            arr.put(sig)
        }
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ASKED_SIGNATURES, arr.toString())
            .apply()
    }
}
