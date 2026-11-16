package design.noodle.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Палитра Noodle: плоские, насыщенные «уличные» цвета + чернила. */
object NoodlePalette {
    val Ink = Color(0xFF1C1410)
    val Paper = Color(0xFFFFF4DA)
    val Yolk = Color(0xFFFFC62E)
    val Chili = Color(0xFFFF5A36)
    val Mint = Color(0xFF3DDBA4)
    val Sky = Color(0xFF55B8FF)
    val Bubble = Color(0xFFFF8AC4)
    val Grape = Color(0xFFA78BFA)
    val Soy = Color(0xFF1C1410)
}

/** Акцент: основной цвет + второй (подсветка выбранного). */
@Immutable
class NoodleAccent(val name: String, val primary: Color, val secondary: Color) {
    companion object {
        val Chili = NoodleAccent("Chili", Color(0xFFFF5A36), Color(0xFFFFC62E))
        val Yolk = NoodleAccent("Yolk", Color(0xFFFFC62E), Color(0xFFFF8A5B))
        val Mint = NoodleAccent("Mint", Color(0xFF3DDBA4), Color(0xFFFFE066))
        val Sky = NoodleAccent("Sky", Color(0xFF55B8FF), Color(0xFFFFE066))
        val Bubble = NoodleAccent("Bubble", Color(0xFFFF8AC4), Color(0xFFFFE066))
        val all = listOf(Chili, Yolk, Mint, Sky, Bubble)
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
    /** Цвет чернильного контура. */
    val outline: Color,
    /** Цвет жёсткой смещённой тени. */
    val hardShadow: Color,
) {
    /** Совместимость: «градиент» теперь плоский — стиль Noodle без градиентов. */
    val gradient: Brush = Brush.linearGradient(listOf(primary, primary))
    val primaryContainer: Color get() = primarySecondary
}

fun lightNoodleColors(accent: NoodleAccent = NoodleAccent.Chili) = NoodleColors(
    isDark = false,
    background = Color(0xFFFFF4DA),
    surface = Color(0xFFFFFCF3),
    surfaceElevated = Color(0xFFFFFCF3),
    label = Color(0xFF1C1410),
    labelSecondary = Color(0xB31C1410),
    labelTertiary = Color(0x661C1410),
    labelQuaternary = Color(0x331C1410),
    separator = Color(0xFF1C1410),
    fill = Color(0xFFE6D5A8),
    fillSecondary = Color(0xFFF0E2BC),
    fillTertiary = Color(0xFFF7EBCB),
    controlThumb = Color(0xFFFFFFFF),
    switchOff = Color(0xFFE6D5A8),
    primary = accent.primary,
    primarySecondary = accent.secondary,
    onPrimary = Color(0xFF1C1410),
    success = Color(0xFF3DDBA4),
    warning = Color(0xFFFFC62E),
    danger = Color(0xFFFF4B4B),
    info = Color(0xFF55B8FF),
    glass = Color(0xFFFFFCF3),
    glassBorder = Color(0xFF1C1410),
    scrim = Color(0x801C1410),
    shadow = Color(0xFF1C1410),
    outline = Color(0xFF1C1410),
    hardShadow = Color(0xFF1C1410),
)

fun darkNoodleColors(accent: NoodleAccent = NoodleAccent.Chili) = NoodleColors(
    isDark = true,
    background = Color(0xFF17110D),
    surface = Color(0xFF261D17),
    surfaceElevated = Color(0xFF2F241C),
    label = Color(0xFFFFF1D6),
    labelSecondary = Color(0xB3FFF1D6),
    labelTertiary = Color(0x66FFF1D6),
    labelQuaternary = Color(0x33FFF1D6),
    separator = Color(0xFFFFF1D6),
    fill = Color(0xFF4C3D31),
    fillSecondary = Color(0xFF3F3228),
    fillTertiary = Color(0xFF33281F),
    controlThumb = Color(0xFFFFF1D6),
    switchOff = Color(0xFF4C3D31),
    primary = accent.primary,
    primarySecondary = accent.secondary,
    onPrimary = Color(0xFF1C1410),
    success = Color(0xFF3DDBA4),
    warning = Color(0xFFFFC62E),
    danger = Color(0xFFFF5C5C),
    info = Color(0xFF55B8FF),
    glass = Color(0xFF261D17),
    glassBorder = Color(0xFFFFF1D6),
    scrim = Color(0xB3000000),
    shadow = Color(0xFF000000),
    outline = Color(0xFFFFF1D6),
    hardShadow = Color(0xFFFFF1D6).copy(alpha = 0.92f),
)
