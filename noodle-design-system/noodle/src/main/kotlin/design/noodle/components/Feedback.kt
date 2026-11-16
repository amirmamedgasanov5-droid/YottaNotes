package design.noodle.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.ink
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/** iOS-подобный спиннер из 12 штрихов. */
@Composable
fun NoodleSpinner(
    modifier: Modifier = Modifier,
    diameter: Dp = 20.dp,
    color: Color = NoodleTheme.colors.label,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "spinnerPhase",
    )
    Canvas(modifier.size(diameter)) {
        val ticks = 12
        val c = center
        val outer = this.size.minDimension / 2f
        val inner = outer * 0.52f
        val head = phase.toInt() % ticks
        for (i in 0 until ticks) {
            val behind = ((head - i) % ticks + ticks) % ticks
            val alpha = 1f - behind / ticks.toFloat() * 0.88f
            val a = Math.toRadians((i * 30.0) - 90.0)
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            drawLine(
                color = color.copy(alpha = color.alpha * alpha),
                start = Offset(c.x + dx * inner, c.y + dy * inner),
                end = Offset(c.x + dx * outer, c.y + dy * outer),
                strokeWidth = outer * 0.2f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Линейный прогресс: чернильная трубка, внутри — цветная лапша. */
@Composable
fun NoodleProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
    brush: Brush = NoodleTheme.colors.gradient,
) {
    val colors = NoodleTheme.colors
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), spring(0.6f, 220f), label = "bar")
    Box(modifier.fillMaxWidth().height(height).ink(NoodleShapes.Capsule, colors.surface, elevation = 3.dp)) {
        Box(
            Modifier
                .padding(2.dp)
                .fillMaxHeight()
                .fillMaxWidth(p.coerceAtLeast(0.001f))
                .clip(NoodleShapes.Capsule)
                .background(brush),
        )
    }
}

/** Волнистый прогресс: активная часть — живая «нить», бегущая по дорожке. */
@Composable
fun NoodleWavyProgress(progress: Float, modifier: Modifier = Modifier) {
    val colors = NoodleTheme.colors
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), spring(0.6f, 220f), label = "wavy")
    val t = rememberInfiniteTransition(label = "wavyT")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "phase",
    )
    Canvas(modifier.fillMaxWidth().height(24.dp)) {
        val sw = 6.dp.toPx()
        val pad = sw / 2f + 1.dp.toPx()
        val end = pad + (size.width - pad * 2f) * p
        drawLine(colors.fill, Offset(end, size.height / 2f), Offset(size.width - pad, size.height / 2f), sw, StrokeCap.Round)
        if (p > 0.001f) {
            squiggle(colors.outline, sw + 4.dp.toPx(), 5.dp.toPx(), 22.dp.toPx(), -phase, x0 = pad, x1 = end)
            squiggle(colors.primary, sw, 5.dp.toPx(), 22.dp.toPx(), -phase, x0 = pad, x1 = end)
        }
    }
}

/** Бесконечный линейный прогресс. */
@Composable
fun NoodleIndeterminateBar(modifier: Modifier = Modifier, height: Dp = 18.dp) {
    val colors = NoodleTheme.colors
    val t = rememberInfiniteTransition(label = "indeterminate")
    val x by t.animateFloat(
        initialValue = -0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = NoodleMotion.Emphasized)),
        label = "x",
    )
    Box(modifier.fillMaxWidth().height(height).ink(NoodleShapes.Capsule, colors.surface, elevation = 3.dp)) {
        Canvas(Modifier.fillMaxSize().padding(2.dp)) {
            val r = CornerRadius(size.height / 2f)
            val w = size.width * 0.35f
            val start = (x * size.width).coerceAtLeast(0f)
            val end = (x * size.width + w).coerceAtMost(size.width)
            if (end > start) drawRoundRect(colors.primary, Offset(start, 0f), Size(end - start, size.height), r)
        }
    }
}

/** Кольцевой прогресс с градиентом. */
@Composable
fun NoodleProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 96.dp,
    strokeWidth: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = NoodleTheme.colors
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = NoodleMotion.Emphasized), label = "ring")
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val sw = strokeWidth.toPx()
            val d = this.size.minDimension - sw - 5.dp.toPx()
            val tl = Offset((this.size.minDimension - d) / 2f, (this.size.minDimension - d) / 2f)
            val arcSize = Size(d, d)
            val ink = Stroke(sw + 5.dp.toPx(), cap = StrokeCap.Round)
            drawArc(colors.outline, 0f, 360f, false, tl, arcSize, style = ink)
            drawArc(colors.surface, 0f, 360f, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            if (p > 0.004f) {
                rotate(-90f) {
                    if (p < 0.999f) drawArc(colors.outline, 0f, 360f * p, false, tl, arcSize, style = ink)
                    drawArc(colors.primary, 0f, 360f * p, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
        }
        content()
    }
}

/** Скелетон с бегущим бликом. */
@Composable
fun Modifier.skeleton(shape: Shape = NoodleShapes.Small): Modifier {
    val colors = NoodleTheme.colors
    val t = rememberInfiniteTransition(label = "skeleton")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "phase",
    )
    val highlight = if (colors.isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.65f)
    return this
        .clip(shape)
        .background(colors.fillTertiary)
        .drawWithContent {
            drawContent()
            val w = this.size.width
            val x = -w * 0.6f + phase * w * 2.2f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, highlight, Color.Transparent),
                    startX = x,
                    endX = x + w * 0.6f,
                ),
            )
        }
}

// ───────────────────────── Toast ─────────────────────────

enum class NoodleToastTone { Neutral, Success, Warning, Danger }

@Stable
class NoodleToastMessage internal constructor(
    val id: Long,
    val text: String,
    val icon: ImageVector?,
    val tone: NoodleToastTone,
)

@Stable
class NoodleToastState {
    private var counter = 0L
    var message: NoodleToastMessage? by mutableStateOf(null)
        internal set
    var last: NoodleToastMessage? by mutableStateOf(null)
        private set

    fun show(text: String, icon: ImageVector? = null, tone: NoodleToastTone = NoodleToastTone.Neutral) {
        val m = NoodleToastMessage(++counter, text, icon, tone)
        last = m
        message = m
    }
}

@Composable
fun rememberNoodleToastState(): NoodleToastState = remember { NoodleToastState() }

/** Тост в стиле Dynamic Island: «капля» выпрыгивает сверху. Поставьте поверх всего интерфейса. */
@Composable
fun NoodleToastHost(state: NoodleToastState, modifier: Modifier = Modifier) {
    val current = state.message
    LaunchedEffect(current?.id) {
        if (current != null) {
            delay(2600)
            if (state.message?.id == current.id) state.message = null
        }
    }
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = current != null,
            enter = slideInVertically(NoodleMotion.bouncy()) { -it * 2 } +
                scaleIn(NoodleMotion.bouncy(), initialScale = 0.5f, transformOrigin = TransformOrigin(0.5f, 0f)) +
                fadeIn(tween(160)),
            exit = slideOutVertically(tween(260, easing = NoodleMotion.Emphasized)) { -it * 2 } +
                scaleOut(tween(260), targetScale = 0.7f, transformOrigin = TransformOrigin(0.5f, 0f)) +
                fadeOut(tween(200)),
        ) {
            val m = state.last
            if (m != null) ToastPill(m)
        }
    }
}

@Composable
private fun ToastPill(m: NoodleToastMessage) {
    val colors = NoodleTheme.colors
    val tint = when (m.tone) {
        NoodleToastTone.Neutral -> colors.surface
        NoodleToastTone.Success -> colors.success
        NoodleToastTone.Warning -> colors.warning
        NoodleToastTone.Danger -> colors.danger
    }
    Row(
        Modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .ink(NoodleShapes.Capsule, colors.primarySecondary, elevation = 4.dp, border = 2.5.dp)
            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(32.dp).ink(CircleShape, tint, elevation = 0.dp, border = 2.dp), contentAlignment = Alignment.Center) {
            if (m.icon != null) NoodleIcon(m.icon, tint = colors.outline, iconSize = 18.dp)
        }
        NoodleText(
            m.text,
            style = NoodleTheme.typography.subhead.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black),
            color = colors.onPrimary,
            maxLines = 2,
        )
    }
}

/** Небольшой хелпер для «поп»-анимации: значение прыгает от from к 1 при каждом триггере. */
@Composable
fun rememberPop(trigger: Any?, from: Float = 0.8f): Float {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        pop.snapTo(from)
        pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.35f, stiffness = 520f))
    }
    return pop.value
}
