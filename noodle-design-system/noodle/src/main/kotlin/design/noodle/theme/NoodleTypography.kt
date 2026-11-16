package design.noodle.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Шкала Noodle: плотные, жирные заголовки и «упитанный» основной текст. */
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
    val f = FontFamily.SansSerif
    return NoodleTypography(
        largeTitle = TextStyle(fontFamily = f, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.2).sp),
        title1 = TextStyle(fontFamily = f, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp),
        title2 = TextStyle(fontFamily = f, fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        title3 = TextStyle(fontFamily = f, fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
        headline = TextStyle(fontFamily = f, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.2).sp),
        body = TextStyle(fontFamily = f, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.1).sp),
        callout = TextStyle(fontFamily = f, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
        subhead = TextStyle(fontFamily = f, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
        footnote = TextStyle(fontFamily = f, fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
        caption1 = TextStyle(fontFamily = f, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
        caption2 = TextStyle(fontFamily = f, fontSize = 10.5.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp),
    )
}
