package design.noodle.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalNoodleColors = staticCompositionLocalOf { lightNoodleColors() }
val LocalNoodleTypography = staticCompositionLocalOf { defaultNoodleTypography() }
val LocalNoodleContentColor = compositionLocalOf { Color.Unspecified }
val LocalNoodleHapticsEnabled = compositionLocalOf { true }

object NoodleTheme {
    val colors: NoodleColors
        @Composable @ReadOnlyComposable get() = LocalNoodleColors.current
    val typography: NoodleTypography
        @Composable @ReadOnlyComposable get() = LocalNoodleTypography.current
}

/**
 * Корень Noodle Design System. Смена темы/акцента плавно перекрашивает весь интерфейс.
 */
@Composable
fun NoodleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: NoodleAccent = NoodleAccent.Chili,
    hapticsEnabled: Boolean = true,
    typography: NoodleTypography = remember { defaultNoodleTypography() },
    content: @Composable () -> Unit,
) {
    val target = remember(darkTheme, accent) {
        if (darkTheme) darkNoodleColors(accent) else lightNoodleColors(accent)
    }
    val colors = animateNoodleColors(target)
    CompositionLocalProvider(
        LocalNoodleColors provides colors,
        LocalNoodleTypography provides typography,
        LocalNoodleHapticsEnabled provides hapticsEnabled,
        content = content,
    )
}

@Composable
private fun c(color: Color): Color =
    animateColorAsState(color, tween(380), label = "noodleColor").value

@Composable
private fun animateNoodleColors(t: NoodleColors): NoodleColors = NoodleColors(
    isDark = t.isDark,
    background = c(t.background),
    surface = c(t.surface),
    surfaceElevated = c(t.surfaceElevated),
    label = c(t.label),
    labelSecondary = c(t.labelSecondary),
    labelTertiary = c(t.labelTertiary),
    labelQuaternary = c(t.labelQuaternary),
    separator = c(t.separator),
    fill = c(t.fill),
    fillSecondary = c(t.fillSecondary),
    fillTertiary = c(t.fillTertiary),
    controlThumb = c(t.controlThumb),
    switchOff = c(t.switchOff),
    primary = c(t.primary),
    primarySecondary = c(t.primarySecondary),
    onPrimary = c(t.onPrimary),
    success = c(t.success),
    warning = c(t.warning),
    danger = c(t.danger),
    info = c(t.info),
    glass = c(t.glass),
    glassBorder = c(t.glassBorder),
    scrim = c(t.scrim),
    shadow = c(t.shadow),
    outline = c(t.outline),
    hardShadow = c(t.hardShadow),
)
