package design.noodle.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.ink
import design.noodle.foundation.noodleClickable
import design.noodle.theme.NoodleTheme
import kotlin.math.PI
import kotlin.math.sin

/** Рисует волнистую «нить» от [x0] до [x1]. */
internal fun DrawScope.squiggle(
    color: Color,
    stroke: Float,
    amplitude: Float,
    wavelength: Float,
    phase: Float,
    y: Float = size.height / 2f,
    x0: Float = 0f,
    x1: Float = size.width,
) {
    val path = Path()
    var x = x0
    val step = 3f
    path.moveTo(x0, y + amplitude * sin(phase))
    while (x < x1) {
        x = (x + step).coerceAtMost(x1)
        path.lineTo(x, y + amplitude * sin(((x - x0) / wavelength) * 2f * PI.toFloat() + phase))
        if (x >= x1) break
    }
    drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Фирменная нить лапши: волнистая линия. Подчёркивания, разделители, украшения. */
@Composable
fun NoodleSquiggle(
    modifier: Modifier = Modifier,
    color: Color = NoodleTheme.colors.primary,
    width: Dp = 72.dp,
    thickness: Dp = 4.dp,
    amplitude: Dp = 4.dp,
    wavelength: Dp = 18.dp,
    animated: Boolean = true,
) {
    val t = rememberInfiniteTransition(label = "squiggle")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "phase",
    )
    val h = amplitude * 2 + thickness
    Canvas(modifier.width(width).height(h)) {
        val pad = thickness.toPx() / 2f
        squiggle(
            color = color,
            stroke = thickness.toPx(),
            amplitude = amplitude.toPx(),
            wavelength = wavelength.toPx(),
            phase = if (animated) -phase else 0f,
            x0 = pad,
            x1 = size.width - pad,
        )
    }
}

/** Декоративные нити: несколько бегущих волн. */
@Composable
fun NoodleWaves(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strands: Int = 4,
    amplitude: Dp = 12.dp,
    strokeWidth: Dp = 3.dp,
    periodMillis: Int = 7000,
    alpha: Float = 0.38f,
) {
    val t = rememberInfiniteTransition(label = "waves")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
        label = "wavePhase",
    )
    Canvas(modifier) {
        for (k in 0 until strands) {
            squiggle(
                color = color.copy(alpha = alpha),
                stroke = strokeWidth.toPx(),
                amplitude = amplitude.toPx() * (0.7f + 0.3f * (k % 2)),
                wavelength = size.width / (1.6f + k * 0.25f),
                phase = phase * (if (k % 2 == 0) 1f else -1f) + k,
                y = size.height * (k + 1f) / (strands + 1f),
            )
        }
    }
}

/** Фирменный лоадер: три нити лапши «плывут». */
@Composable
fun NoodleLoader(
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
    height: Dp = 40.dp,
) {
    val colors = NoodleTheme.colors
    val t = rememberInfiniteTransition(label = "loader")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "loaderPhase",
    )
    Canvas(modifier.size(width, height)) {
        val cols = listOf(colors.primary, colors.primarySecondary, colors.outline)
        for (k in 0 until 3) {
            squiggle(
                color = cols[k],
                stroke = size.height * 0.15f,
                amplitude = size.height * 0.16f,
                wavelength = size.width / 1.5f,
                phase = -phase + k * 0.9f,
                y = size.height * (0.24f + 0.26f * k),
                x0 = size.height * 0.1f,
                x1 = size.width - size.height * 0.1f,
            )
        }
    }
}

/** Логотип Noodle: яркий блок с чернильным контуром и тремя нитями. */
@Composable
fun NoodleLogo(
    modifier: Modifier = Modifier,
    diameter: Dp = 72.dp,
    onClick: (() -> Unit)? = null,
) {
    val colors = NoodleTheme.colors
    val t = rememberInfiniteTransition(label = "logo")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "logoPhase",
    )
    Box(
        modifier
            .size(diameter)
            .then(if (onClick != null) Modifier.noodleClickable(pressedScale = 0.92f, onClick = onClick) else Modifier)
            .ink(SquircleShape(diameter * 0.3f), colors.primarySecondary, elevation = diameter * 0.07f, border = 2.5.dp),
    ) {
        Canvas(Modifier.size(diameter)) {
            val cols = listOf(colors.primary, colors.outline, colors.primary)
            for (k in 0 until 3) {
                squiggle(
                    color = cols[k],
                    stroke = size.height * 0.085f,
                    amplitude = size.height * 0.055f,
                    wavelength = size.width * 0.42f,
                    phase = phase * (if (k % 2 == 0) 1f else -1f) + k,
                    y = size.height * (0.3f + 0.2f * k),
                    x0 = size.width * 0.2f,
                    x1 = size.width * 0.8f,
                )
            }
        }
    }
}
