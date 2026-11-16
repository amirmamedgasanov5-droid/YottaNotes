package design.noodle.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Шкала Noodle: небольшие, лёгкие, плотно набранные размеры — тишина и воздух. */
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
        largeTitle = TextStyle(fontFamily = f, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Normal, letterSpacing = (-1.0).sp),
        title1 = TextStyle(fontFamily = f, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.8).sp),
        title2 = TextStyle(fontFamily = f, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.5).sp),
        title3 = TextStyle(fontFamily = f, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.3).sp),
        headline = TextStyle(fontFamily = f, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp),
        body = TextStyle(fontFamily = f, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.2).sp),
        callout = TextStyle(fontFamily = f, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.1).sp),
        subhead = TextStyle(fontFamily = f, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
        footnote = TextStyle(fontFamily = f, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
        caption1 = TextStyle(fontFamily = f, fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.Normal),
        caption2 = TextStyle(fontFamily = f, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    )
}
