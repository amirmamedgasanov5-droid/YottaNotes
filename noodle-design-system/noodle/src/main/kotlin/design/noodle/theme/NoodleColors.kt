package design.noodle.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Noodle: тихая монохромная база + один тёплый акцент. */
object NoodlePalette {
    val Ink = Color(0xFF111111)
    val Paper = Color(0xFFFFFFFF)
    val Amber = Color(0xFFE9A23B)
    val Clay = Color(0xFFD9694B)
    val Sage = Color(0xFF6FA287)
    val Sky = Color(0xFF5E9BD6)
    val Rose = Color(0xFFD9708F)
}

/** Акцент появляется только в выделении, фокусе и «нити». */
@Immutable
class NoodleAccent(val name: String, val primary: Color, val secondary: Color) {
    companion object {
        val Amber = NoodleAccent("Amber", NoodlePalette.Amber, NoodlePalette.Amber)
        val Clay = NoodleAccent("Clay", NoodlePalette.Clay, NoodlePalette.Clay)
        val Sage = NoodleAccent("Sage", NoodlePalette.Sage, NoodlePalette.Sage)
        val Sky = NoodleAccent("Sky", NoodlePalette.Sky, NoodlePalette.Sky)
        val Rose = NoodleAccent("Rose", NoodlePalette.Rose, NoodlePalette.Rose)
        val Chili = Amber
        val all = listOf(Amber, Clay, Sage, Sky, Rose)
    }
}

@Immutable
class NoodleColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val label: Color,
    val labelSecondary: Color,
    val labelTertiary: Color,
    val labelQuaternary: Color,
    val separator: Color,
    val fill: Color,
    val fillSecondary: Color,
    val fillTertiary: Color,
    val controlThumb: Color,
    val switchOff: Color,
    val primary: Color,
    val primarySecondary: Color,
    val onPrimary: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val glass: Color,
    val glassBorder: Color,
    val scrim: Color,
    val shadow: Color,
    val outline: Color,
    val hardShadow: Color,
) {
    val gradient: Brush = Brush.linearGradient(listOf(label, label))
    val primaryContainer: Color get() = fillTertiary
    /** Фон главной (тёмной в светлой теме) кнопки — инверсия текста. */
    val inverse: Color get() = background
}

fun lightNoodleColors(accent: NoodleAccent = NoodleAccent.Amber) = NoodleColors(
    isDark = false,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    label = Color(0xFF111111),
    labelSecondary = Color(0xFF737373),
    labelTertiary = Color(0xFFA8A8A8),
    labelQuaternary = Color(0xFFD4D4D4),
    separator = Color(0xFFEDEDED),
    fill = Color(0xFFE9E9E9),
    fillSecondary = Color(0xFFF0F0F0),
    fillTertiary = Color(0xFFF6F6F6),
    controlThumb = Color(0xFFFFFFFF),
    switchOff = Color(0xFFE4E4E4),
    primary = accent.primary,
    primarySecondary = accent.secondary,
    onPrimary = Color(0xFFFFFFFF),
    success = Color(0xFF3B9B6B),
    warning = Color(0xFFE9A23B),
    danger = Color(0xFFD64545),
    info = Color(0xFF5E9BD6),
    glass = Color(0xF5FFFFFF),
    glassBorder = Color(0xFFE8E8E8),
    scrim = Color(0x66000000),
    shadow = Color(0xFF000000),
    outline = Color(0xFFE6E6E6),
    hardShadow = Color(0x00000000),
)

fun darkNoodleColors(accent: NoodleAccent = NoodleAccent.Amber) = NoodleColors(
    isDark = true,
    background = Color(0xFF0B0B0B),
    surface = Color(0xFF0B0B0B),
    surfaceElevated = Color(0xFF141414),
    label = Color(0xFFF2F2F2),
    labelSecondary = Color(0xFF8C8C8C),
    labelTertiary = Color(0xFF5C5C5C),
    labelQuaternary = Color(0xFF383838),
    separator = Color(0xFF1F1F1F),
    fill = Color(0xFF2A2A2A),
    fillSecondary = Color(0xFF1E1E1E),
    fillTertiary = Color(0xFF161616),
    controlThumb = Color(0xFF0B0B0B),
    switchOff = Color(0xFF2A2A2A),
    primary = accent.primary,
    primarySecondary = accent.secondary,
    onPrimary = Color(0xFF0B0B0B),
    success = Color(0xFF4FB985),
    warning = Color(0xFFE9A23B),
    danger = Color(0xFFE5645E),
    info = Color(0xFF6BA8E0),
    glass = Color(0xF50B0B0B),
    glassBorder = Color(0xFF242424),
    scrim = Color(0x99000000),
    shadow = Color(0xFF000000),
    outline = Color(0xFF262626),
    hardShadow = Color(0x00000000),
)
