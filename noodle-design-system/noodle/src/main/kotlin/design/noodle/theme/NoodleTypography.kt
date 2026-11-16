package design.noodle.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import design.noodle.R

/** Дисплейный шрифт Noodle — Prata: выразительная антиква для заголовков и цифр. */
val NoodleDisplay = FontFamily(Font(R.font.prata_regular, FontWeight.Normal))

/** Текстовый шрифт Noodle — Inter: спокойный гротеск для интерфейса. */
val NoodleText = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_semibold, FontWeight.Bold),
)

@Immutable
class NoodleTypography(
    val largeTitle: TextStyle,
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val subhead: TextStyle,
    val footnote: TextStyle,
    val caption1: TextStyle,
    val caption2: TextStyle,
)

fun defaultNoodleTypography(): NoodleTypography {
    val d = NoodleDisplay
    val t = NoodleText
    return NoodleTypography(
        largeTitle = TextStyle(fontFamily = d, fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.6).sp),
        title1 = TextStyle(fontFamily = d, fontSize = 29.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.4).sp),
        title2 = TextStyle(fontFamily = d, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.2).sp),
        title3 = TextStyle(fontFamily = t, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.3).sp),
        headline = TextStyle(fontFamily = t, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp),
        body = TextStyle(fontFamily = t, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.15).sp),
        callout = TextStyle(fontFamily = t, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.1).sp),
        subhead = TextStyle(fontFamily = t, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
        footnote = TextStyle(fontFamily = t, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
        caption1 = TextStyle(fontFamily = t, fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.Normal),
        caption2 = TextStyle(fontFamily = t, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    )
}
