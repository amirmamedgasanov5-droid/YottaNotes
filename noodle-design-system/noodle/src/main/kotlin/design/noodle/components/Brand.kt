package design.noodle.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.noodleClickable
import design.noodle.theme.NoodleTheme
import kotlin.math.PI
import kotlin.math.sin

private fun DrawScope.strand(
    yBase: Float,
    amplitude: Float,
    phase: Float,
    waves: Float,
    color: Color,
    stroke: Float,
    envelope: Boolean,
) {
    val w = size.width
    val steps = 72
    val path = Path()
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val env = if (envelope) sin(PI.toFloat() * t) else 1f
        val y = yBase + amplitude * env * sin(t * 2f * PI.toFloat() * waves + phase)
        if (i == 0) path.moveTo(0f, y) else path.lineTo(w * t, y)
    }
    drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Декоративные «нити лапши»: плавно бегущие волны. Для героев, пустых состояний и фонов. */
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
            val y = size.height * (k + 1f) / (strands + 1f)
            strand(
                yBase = y,
                amplitude = amplitude.toPx() * (0.6f + 0.4f * ((k % 2) + 1)),
                phase = phase * (if (k % 2 == 0) 1f else -1f) + k * 0.9f,
                waves = 1.4f + k * 0.18f,
                color = color.copy(alpha = alpha * (1f - k * 0.14f)),
                stroke = strokeWidth.toPx(),
                envelope = false,
            )
        }
    }
}

/** Фирменный лоадер Noodle: три нити лапши переливаются градиентом. */
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
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "loaderPhase",
    )
    Canvas(modifier.size(width, height)) {
        val brush = Brush.horizontalGradient(listOf(colors.primarySecondary, colors.primary))
        val sw = height.toPx() * 0.13f
        for (k in 0 until 3) {
            val path = Path()
            val steps = 60
            val y0 = size.height * (0.28f + 0.22f * k)
            for (i in 0..steps) {
                val tt = i / steps.toFloat()
                val env = sin(PI.toFloat() * tt)
                val y = y0 + size.height * 0.2f * env * sin(tt * 2f * PI.toFloat() * 1.25f - phase + k * 1.1f)
                if (i == 0) path.moveTo(0f, y) else path.lineTo(size.width * tt, y)
            }
            drawPath(path, brush, alpha = 1f - k * 0.22f, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Логотип Noodle: градиентный сквиркл с тремя нитями. Нажмите — он подпрыгнет. */
@Composable
fun NoodleLogo(
    modifier: Modifier = Modifier,
    diameter: Dp = 72.dp,
    onClick: (() -> Unit)? = null,
) {
    val colors = NoodleTheme.colors
    val shape = SquircleShape(diameter * 0.28f)
    val t = rememberInfiniteTransition(label = "logo")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
        label = "logoPhase",
    )
    Box(
        modifier
            .size(diameter)
            .then(if (onClick != null) Modifier.noodleClickable(pressedScale = 0.9f, onClick = onClick) else Modifier)
            .clip(shape)
            .background(colors.gradient),
    ) {
        Canvas(Modifier.size(diameter)) {
            val sw = size.height * 0.075f
            for (k in 0 until 3) {
                val y = size.height * (0.34f + 0.16f * k)
                strand(
                    yBase = y,
                    amplitude = size.height * 0.07f,
                    phase = phase * (if (k % 2 == 0) 1f else -1f) + k * 0.8f,
                    waves = 1.2f,
                    color = Color.White.copy(alpha = 0.55f + 0.2f * k),
                    stroke = sw,
                    envelope = true,
                )
            }
        }
    }
}
