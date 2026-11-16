package design.noodle.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Шкала типографики в духе Apple HIG (Large Title … Caption 2). */
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
    val f = FontFamily.Default
    return NoodleTypography(
        largeTitle = TextStyle(fontFamily = f, fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
        title1 = TextStyle(fontFamily = f, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
        title2 = TextStyle(fontFamily = f, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp),
        title3 = TextStyle(fontFamily = f, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        headline = TextStyle(fontFamily = f, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
        body = TextStyle(fontFamily = f, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.4).sp),
        callout = TextStyle(fontFamily = f, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.3).sp),
        subhead = TextStyle(fontFamily = f, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.2).sp),
        footnote = TextStyle(fontFamily = f, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.1).sp),
        caption1 = TextStyle(fontFamily = f, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
        caption2 = TextStyle(fontFamily = f, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.1.sp),
    )
}
