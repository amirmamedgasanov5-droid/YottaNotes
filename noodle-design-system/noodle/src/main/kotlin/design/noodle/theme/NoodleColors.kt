package design.noodle.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Noodle: тихая монохромная база + один тёплый акцент. */
object NoodlePalette {
    val Ink = Color(0xFF1A1713)
    val Paper = Color(0xFFFAF8F4)
    val Amber = Color(0xFFE48E2C)
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
    background = Color(0xFFFAF8F4),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    label = Color(0xFF1A1713),
    labelSecondary = Color(0xFF77716A),
    labelTertiary = Color(0xFFADA79F),
    labelQuaternary = Color(0xFFD9D3C9),
    separator = Color(0xFFE9E4DB),
    fill = Color(0xFFE6E0D5),
    fillSecondary = Color(0xFFEFEBE3),
    fillTertiary = Color(0xFFF3F0E9),
    controlThumb = Color(0xFFFFFFFF),
    switchOff = Color(0xFFE2DCD0),
    primary = accent.primary,
    primarySecondary = accent.secondary,
    onPrimary = Color(0xFFFFFFFF),
    success = Color(0xFF3B9B6B),
    warning = Color(0xFFE9A23B),
    danger = Color(0xFFD64545),
    info = Color(0xFF5E9BD6),
    glass = Color(0xF7FFFFFF),
    glassBorder = Color(0xFFE7E2D9),
    scrim = Color(0x66000000),
    shadow = Color(0xFF000000),
    outline = Color(0xFFE7E2D9),
    hardShadow = Color(0x00000000),
)

fun darkNoodleColors(accent: NoodleAccent = NoodleAccent.Amber) = NoodleColors(
    isDark = true,
    background = Color(0xFF0F0E0C),
    surface = Color(0xFF171512),
    surfaceElevated = Color(0xFF1D1A16),
    label = Color(0xFFF3EFE8),
    labelSecondary = Color(0xFF8F8981),
    labelTertiary = Color(0xFF5E5951),
    labelQuaternary = Color(0xFF3A362F),
    separator = Color(0xFF24211C),
    fill = Color(0xFF2D2924),
    fillSecondary = Color(0xFF211E1A),
    fillTertiary = Color(0xFF191714),
    controlThumb = Color(0xFF0F0E0C),
    switchOff = Color(0xFF2D2924),
    primary = accent.primary,
    primarySecondary = accent.secondary,
    onPrimary = Color(0xFF0F0E0C),
    success = Color(0xFF4FB985),
    warning = Color(0xFFE9A23B),
    danger = Color(0xFFE5645E),
    info = Color(0xFF6BA8E0),
    glass = Color(0xF7171512),
    glassBorder = Color(0xFF2A2621),
    scrim = Color(0x99000000),
    shadow = Color(0xFF000000),
    outline = Color(0xFF2A2621),
    hardShadow = Color(0x00000000),
)
