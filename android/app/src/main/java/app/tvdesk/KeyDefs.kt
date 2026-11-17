package app.tvdesk

import android.view.KeyEvent as K

enum class KType { CHAR, SPACE, FN, SHIFT, CTRL, ALT, WIN, CAPS, LANG, SHOT, UPDOWN }

/**
 * Одна клавиша. [code] — физический код (по нему работают Ctrl+C и т.п. даже в русской раскладке),
 * en/ru — символы без Shift, enS/ruS — с Shift.
 */
class KeyDef(
    val label: String,
    val code: Int,
    val weight: Float = 1f,
    val type: KType = KType.FN,
    val en: String = "", val enS: String = "",
    val ru: String = "", val ruS: String = "",
    val letter: Boolean = false,
) {
    val isMod get() = type == KType.SHIFT || type == KType.CTRL || type == KType.ALT || type == KType.WIN
    val repeatable get() = type == KType.CHAR || type == KType.SPACE || type == KType.FN
}

object KeyDefs {
    private fun fn(label: String, code: Int, w: Float = 1f) = KeyDef(label, code, w, KType.FN)
    private fun l(en: String, ru: String, code: Int, w: Float = 1f) =
        KeyDef(en, code, w, KType.CHAR, en, en.uppercase(), ru, ru.uppercase(), letter = true)
    private fun c(en: String, enS: String, ru: String, ruS: String, code: Int, w: Float = 1f) =
        KeyDef(en, code, w, KType.CHAR, en, enS, ru, ruS)

    /** Раскладка как у ноутбука. Сумма весов в каждой строке = 16. */
    val rows: List<List<KeyDef>> = listOf(
        buildList {
            add(fn("Esc", K.KEYCODE_ESCAPE))
            for (i in 0 until 12) add(fn("F${i + 1}", K.KEYCODE_F1 + i))
            add(fn("Ins", K.KEYCODE_INSERT))
            add(KeyDef("PrtSc", K.KEYCODE_SYSRQ, 1f, KType.SHOT))
            add(fn("Del", K.KEYCODE_FORWARD_DEL))
        },
        listOf(
            c("`", "~", "ё", "Ё", K.KEYCODE_GRAVE),
            c("1", "!", "1", "!", K.KEYCODE_1),
            c("2", "@", "2", "\"", K.KEYCODE_2),
            c("3", "#", "3", "№", K.KEYCODE_3),
            c("4", "$", "4", ";", K.KEYCODE_4),
            c("5", "%", "5", "%", K.KEYCODE_5),
            c("6", "^", "6", ":", K.KEYCODE_6),
            c("7", "&", "7", "?", K.KEYCODE_7),
            c("8", "*", "8", "*", K.KEYCODE_8),
            c("9", "(", "9", "(", K.KEYCODE_9),
            c("0", ")", "0", ")", K.KEYCODE_0),
            c("-", "_", "-", "_", K.KEYCODE_MINUS),
            c("=", "+", "=", "+", K.KEYCODE_EQUALS),
            fn("Backspace", K.KEYCODE_DEL, 2f),
            fn("PgUp", K.KEYCODE_PAGE_UP),
        ),
        listOf(
            fn("Tab", K.KEYCODE_TAB, 1.5f),
            l("q", "й", K.KEYCODE_Q), l("w", "ц", K.KEYCODE_W), l("e", "у", K.KEYCODE_E),
            l("r", "к", K.KEYCODE_R), l("t", "е", K.KEYCODE_T), l("y", "н", K.KEYCODE_Y),
            l("u", "г", K.KEYCODE_U), l("i", "ш", K.KEYCODE_I), l("o", "щ", K.KEYCODE_O),
            l("p", "з", K.KEYCODE_P),
            KeyDef("[", K.KEYCODE_LEFT_BRACKET, 1f, KType.CHAR, "[", "{", "х", "Х", letter = false),
            KeyDef("]", K.KEYCODE_RIGHT_BRACKET, 1f, KType.CHAR, "]", "}", "ъ", "Ъ", letter = false),
            c("\\", "|", "\\", "/", K.KEYCODE_BACKSLASH, 1.5f),
            fn("PgDn", K.KEYCODE_PAGE_DOWN),
        ),
        listOf(
            KeyDef("Caps", 0, 1.75f, KType.CAPS),
            l("a", "ф", K.KEYCODE_A), l("s", "ы", K.KEYCODE_S), l("d", "в", K.KEYCODE_D),
            l("f", "а", K.KEYCODE_F), l("g", "п", K.KEYCODE_G), l("h", "р", K.KEYCODE_H),
            l("j", "о", K.KEYCODE_J), l("k", "л", K.KEYCODE_K), l("l", "д", K.KEYCODE_L),
            KeyDef(";", K.KEYCODE_SEMICOLON, 1f, KType.CHAR, ";", ":", "ж", "Ж"),
            KeyDef("'", K.KEYCODE_APOSTROPHE, 1f, KType.CHAR, "'", "\"", "э", "Э"),
            fn("Enter", K.KEYCODE_ENTER, 2.25f),
            fn("Home", K.KEYCODE_MOVE_HOME),
        ),
        listOf(
            KeyDef("Shift", 0, 2.25f, KType.SHIFT),
            l("z", "я", K.KEYCODE_Z), l("x", "ч", K.KEYCODE_X), l("c", "с", K.KEYCODE_C),
            l("v", "м", K.KEYCODE_V), l("b", "и", K.KEYCODE_B), l("n", "т", K.KEYCODE_N),
            l("m", "ь", K.KEYCODE_M),
            KeyDef(",", K.KEYCODE_COMMA, 1f, KType.CHAR, ",", "<", "б", "Б"),
            KeyDef(".", K.KEYCODE_PERIOD, 1f, KType.CHAR, ".", ">", "ю", "Ю"),
            c("/", "?", ".", ",", K.KEYCODE_SLASH),
            KeyDef("Shift", 0, 2.75f, KType.SHIFT),
            fn("End", K.KEYCODE_MOVE_END),
        ),
        listOf(
            KeyDef("Ctrl", 0, 1.25f, KType.CTRL),
            KeyDef("Win", 0, 1.25f, KType.WIN),
            KeyDef("Alt", 0, 1.25f, KType.ALT),
            KeyDef("EN", 0, 1f, KType.LANG),
            KeyDef("", K.KEYCODE_SPACE, 5.75f, KType.SPACE),
            KeyDef("Alt", 0, 1.25f, KType.ALT),
            KeyDef("Ctrl", 0, 1.25f, KType.CTRL),
            fn("←", K.KEYCODE_DPAD_LEFT),
            KeyDef("", 0, 1f, KType.UPDOWN),
            fn("→", K.KEYCODE_DPAD_RIGHT),
        ),
    )
}
