package com.macdeck.keyboard

// CGEventFlags masks used by macOS
const val SH = 0x20000
const val CT = 0x40000
const val OP = 0x80000
const val CM = 0x100000
const val FNF = 0x800000
const val CAPS = -1

class K(
    val label: String,
    val code: Int,
    val w: Float = 1f,
    val shift: String? = null,
    val ar: String? = null,
    val mod: Int = 0,
    val repeat: Boolean = false,
    val small: Boolean = false
)

object Keys {
    private fun L(c: String, code: Int, ar: String) = K(c.uppercase(), code, 1f, null, ar)

    val LEFT = K("◀", 123, repeat = true)
    val RIGHT = K("▶", 124, repeat = true)
    val DOWN = K("▼", 125, repeat = true)
    val UP = K("▲", 126, repeat = true)

    private val fkeys = listOf(122, 120, 99, 118, 96, 97, 98, 100, 101, 109, 103, 111)

    val rows: List<List<K>> = listOf(
        listOf(K("esc", 53, 1.5f, small = true)) +
            fkeys.mapIndexed { i, c -> K("F${i + 1}", c, 1.125f, small = true) },

        listOf(
            K("`", 50, 1f, "~", "ذ"), K("1", 18, 1f, "!"), K("2", 19, 1f, "@"),
            K("3", 20, 1f, "#"), K("4", 21, 1f, "\$"), K("5", 23, 1f, "%"),
            K("6", 22, 1f, "^"), K("7", 26, 1f, "&"), K("8", 28, 1f, "*"),
            K("9", 25, 1f, "("), K("0", 29, 1f, ")"), K("-", 27, 1f, "_"),
            K("=", 24, 1f, "+"), K("delete", 51, 2f, repeat = true, small = true)
        ),

        listOf(
            K("tab", 48, 1.5f, small = true),
            L("q", 12, "ض"), L("w", 13, "ص"), L("e", 14, "ث"), L("r", 15, "ق"),
            L("t", 17, "ف"), L("y", 16, "غ"), L("u", 32, "ع"), L("i", 34, "ه"),
            L("o", 31, "خ"), L("p", 35, "ح"),
            K("[", 33, 1f, "{", "ج"), K("]", 30, 1f, "}", "د"), K("\\", 42, 1.5f, "|")
        ),

        listOf(
            K("caps lock", 57, 1.75f, mod = CAPS, small = true),
            L("a", 0, "ش"), L("s", 1, "س"), L("d", 2, "ي"), L("f", 3, "ب"),
            L("g", 5, "ل"), L("h", 4, "ا"), L("j", 38, "ت"), L("k", 40, "ن"),
            L("l", 37, "م"),
            K(";", 41, 1f, ":", "ك"), K("'", 39, 1f, "\"", "ط"),
            K("return", 36, 2.25f, small = true)
        ),

        listOf(
            K("⇧ shift", 56, 2.25f, mod = SH, small = true),
            L("z", 6, "ئ"), L("x", 7, "ء"), L("c", 8, "ؤ"), L("v", 9, "ر"),
            L("b", 11, "لا"), L("n", 45, "ى"), L("m", 46, "ة"),
            K(",", 43, 1f, "<", "و"), K(".", 47, 1f, ">", "ز"), K("/", 44, 1f, "?", "ظ"),
            K("⇧ shift", 60, 2.75f, mod = SH, small = true)
        ),

        listOf(
            K("fn", 63, 1f, mod = FNF, small = true),
            K("control", 59, 1f, mod = CT, small = true),
            K("⌥ option", 58, 1f, mod = OP, small = true),
            K("⌘ command", 55, 1.25f, mod = CM, small = true),
            K("", 49, 5.5f),
            K("⌘ command", 54, 1.25f, mod = CM, small = true),
            K("⌥ option", 61, 1f, mod = OP, small = true),
            K("ARROWS", 0, 3f)
        )
    )
}
