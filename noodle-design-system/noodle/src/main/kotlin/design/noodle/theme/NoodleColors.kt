package design.noodle.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Фирменные цвета Noodle — тёплые, «бульонные». */
object NoodlePalette {
    val Yolk = Color(0xFFFFB627)
    val Chili = Color(0xFFFF5A2C)
    val Scallion = Color(0xFF2FBF8F)
    val Naruto = Color(0xFFFF4F8B)
    val Sky = Color(0xFF3D8BFF)
    val Plum = Color(0xFF8E5BFF)
    val Soy = Color(0xFF1B1411)
    val Cream = Color(0xFFF6F1E9)
}

/** Акцент: основной цвет + второй цвет градиента. */
@Immutable
class NoodleAccent(val name: String, val primary: Color, val secondary: Color) {
    companion object {
        val Chili = NoodleAccent("Chili", Color(0xFFFF5A2C), Color(0xFFFFB627))
        val Naruto = NoodleAccent("Naruto", Color(0xFFFF4F8B), Color(0xFFFFAA6B))
        val Matcha = NoodleAccent("Matcha", Color(0xFF14A97A), Color(0xFFA6E36B))
        val Ocean = NoodleAccent("Ocean", Color(0xFF2F7BFF), Color(0xFF5ED6FF))
        val Plum = NoodleAccent("Plum", Color(0xFF8E5BFF), Color(0xFFFF8AD8))
        val all = listOf(Chili, Naruto, Matcha, Ocean, Plum)
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
) {
    /** Фирменный градиент (вторичный → основной). */
    val gradient: Brush = Brush.linearGradient(listOf(primarySecondary, primary))
    val primaryContainer: Color get() = primary.copy(alpha = 0.14f)
}

fun lightNoodleColors(accent: NoodleAccent = NoodleAccent.Chili) = NoodleColors(
    isDark = false,
    background = Color(0xFFF6F1E9),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    label = Color(0xFF1E1713),
    labelSecondary = Color(0x9E1E1713),
    labelTertiary = Color(0x4D1E1713),
    labelQuaternary = Color(0x2E1E1713),
    separator = Color(0x261E1713),
    fill = Color(0x331E1713),
    fillSecondary = Color(0x291E1713),
    fillTertiary = Color(0x1A1E1713),
    controlThumb = Color(0xFFFFFFFF),
    switchOff = Color(0x4D78707A),
    primary = accent.primary,
    primarySecondary = accent.secondary,
    onPrimary = Color.White,
    success = Color(0xFF2FBF8F),
    warning = Color(0xFFFFB627),
    danger = Color(0xFFFF3B30),
    info = Color(0xFF3D8BFF),
    glass = Color(0xE8FFFFFF),
    glassBorder = Color(0x1A1E1713),
    scrim = Color(0x66120C08),
    shadow = Color(0xFF5A3A1E),
)

fun darkNoodleColors(accent: NoodleAccent = NoodleAccent.Chili) = NoodleColors(
    isDark = true,
    background = Color(0xFF0F0D0C),
    surface = Color(0xFF1C1816),
    surfaceElevated = Color(0xFF2A2523),
    label = Color(0xFFF7F1E8),
    labelSecondary = Color(0x99F7F1E8),
    labelTertiary = Color(0x4DF7F1E8),
    labelQuaternary = Color(0x2EF7F1E8),
    separator = Color(0x33F7F1E8),
    fill = Color(0x47F7F1E8),
    fillSecondary = Color(0x38F7F1E8),
    fillTertiary = Color(0x24F7F1E8),
    controlThumb = Color(0xFF6A6360),
    switchOff = Color(0x5C8A8290),
    primary = accent.primary,
    primarySecondary = accent.secondary,
    onPrimary = Color.White,
    success = Color(0xFF3ED6A3),
    warning = Color(0xFFFFC24A),
    danger = Color(0xFFFF5A4F),
    info = Color(0xFF5B9DFF),
    glass = Color(0xE62A2523),
    glassBorder = Color(0x1FFFFFFF),
    scrim = Color(0x99000000),
    shadow = Color(0xFF000000),
)
