package design.noodle.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.ink
import design.noodle.foundation.inkPressable
import design.noodle.theme.NoodleTheme

/** Чекбокс: квадрат с контуром, при выборе заливается и галочка «рисуется» чернилами. */
@Composable
fun NoodleCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    diameter: Dp = 28.dp,
) {
    val colors = NoodleTheme.colors
    val fill by animateColorAsState(if (checked) colors.primarySecondary else colors.surface, tween(140), label = "cbFill")
    val check by animateFloatAsState(if (checked) 1f else 0f, tween(240, delayMillis = if (checked) 60 else 0), label = "check")
    val measure = remember { PathMeasure() }
    Box(
        modifier
            .size(diameter)
            .inkPressable(SquircleShape(9.dp), fill, enabled, elevation = 2.dp, role = Role.Checkbox) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(diameter)) {
            if (check > 0f) {
                val s = size.minDimension
                val path = Path().apply {
                    moveTo(s * 0.24f, s * 0.52f); lineTo(s * 0.42f, s * 0.70f); lineTo(s * 0.77f, s * 0.30f)
                }
                measure.setPath(path, false)
                val dst = Path()
                measure.getSegment(0f, measure.length * check.coerceIn(0f, 1f), dst, true)
                drawPath(dst, colors.outline, style = Stroke(3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** Радио: круг с контуром, внутри «прыгает» чернильная точка. */
@Composable
fun NoodleRadio(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    diameter: Dp = 28.dp,
) {
    val colors = NoodleTheme.colors
    val fill by animateColorAsState(if (selected) colors.primarySecondary else colors.surface, tween(140), label = "rdFill")
    val dot by animateFloatAsState(if (selected) 1f else 0f, spring(0.4f, 600f), label = "dot")
    Box(
        modifier
            .size(diameter)
            .inkPressable(CircleShape, fill, enabled, elevation = 2.dp, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(diameter * 0.4f)
                .graphicsLayer { scaleX = dot; scaleY = dot }
                .ink(CircleShape, colors.outline, elevation = 0.dp, border = 0.dp),
        )
    }
}

/** Чип-фильтр: выбранный — жёлтый, с галочкой и вдавлен. */
@Composable
fun NoodleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val colors = NoodleTheme.colors
    val fill by animateColorAsState(if (selected) colors.primarySecondary else colors.surface, tween(160), label = "chipBg")
    Row(
        modifier
            .inkPressable(NoodleShapes.Capsule, fill, elevation = if (selected) 1.dp else 3.dp, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) NoodleIcon(leadingIcon, Modifier.padding(end = 6.dp), tint = colors.outline, iconSize = 16.dp)
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            Canvas(Modifier.padding(end = 7.dp).size(12.dp)) {
                val s = size.minDimension
                val p = Path().apply { moveTo(s * 0.1f, s * 0.55f); lineTo(s * 0.4f, s * 0.85f); lineTo(s * 0.92f, s * 0.2f) }
                drawPath(p, colors.outline, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        NoodleText(text, style = NoodleTheme.typography.subhead.copy(fontWeight = FontWeight.ExtraBold), color = if (selected) colors.onPrimary else colors.label, maxLines = 1)
    }
}

/** Тег / статус: цветная плашка с чернильным контуром. */
@Composable
fun NoodleTag(text: String, modifier: Modifier = Modifier, color: Color = NoodleTheme.colors.primary) {
    val dark = color.luminanceLow()
    NoodleText(
        text,
        modifier
            .ink(NoodleShapes.Capsule, color, elevation = 0.dp, border = 1.8.dp)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        style = NoodleTheme.typography.caption1.copy(fontWeight = FontWeight.ExtraBold),
        color = if (dark) Color.White else NoodleTheme.colors.onPrimary,
        maxLines = 1,
    )
}

private fun Color.luminanceLow(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.45f
