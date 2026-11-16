package design.noodle.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.noodleClickable
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme

/** Круглый чекбокс как в iOS: заливка «выстреливает», галочка прорисовывается. */
@Composable
fun NoodleCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    diameter: Dp = 26.dp,
) {
    val colors = NoodleTheme.colors
    val fill by animateFloatAsState(if (checked) 1f else 0f, NoodleMotion.bouncy(), label = "fill")
    val check by animateFloatAsState(if (checked) 1f else 0f, tween(280, delayMillis = if (checked) 80 else 0), label = "check")
    val measure = remember { PathMeasure() }
    Canvas(
        modifier
            .size(diameter)
            .noodleClickable(enabled = enabled, pressedScale = 0.88f, role = Role.Checkbox) { onCheckedChange(!checked) },
    ) {
        val s = this.size.minDimension
        val stroke = 1.6.dp.toPx()
        drawCircle(colors.labelTertiary, radius = s / 2f - stroke / 2f, style = Stroke(stroke))
        if (fill > 0f) drawCircle(colors.primary, radius = s / 2f * fill.coerceAtLeast(0f))
        if (check > 0f) {
            val path = Path().apply {
                moveTo(s * 0.28f, s * 0.53f)
                lineTo(s * 0.43f, s * 0.67f)
                lineTo(s * 0.73f, s * 0.35f)
            }
            measure.setPath(path, false)
            val dst = Path()
            measure.getSegment(0f, measure.length * check.coerceIn(0f, 1f), dst, true)
            drawPath(dst, Color.White, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Радио-кнопка. */
@Composable
fun NoodleRadio(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    diameter: Dp = 26.dp,
) {
    val colors = NoodleTheme.colors
    val dot by animateFloatAsState(if (selected) 1f else 0f, NoodleMotion.bouncy(), label = "dot")
    val ring by animateColorAsState(if (selected) colors.primary else colors.labelTertiary, tween(200), label = "ring")
    Canvas(
        modifier
            .size(diameter)
            .noodleClickable(enabled = enabled, pressedScale = 0.88f, role = Role.RadioButton, onClick = onClick),
    ) {
        val s = this.size.minDimension
        val stroke = 1.8.dp.toPx()
        drawCircle(ring, radius = s / 2f - stroke / 2f, style = Stroke(stroke))
        if (dot > 0f) drawCircle(colors.primary, radius = (s / 2f - stroke * 2.2f).coerceAtLeast(0f) * dot)
    }
}

/** Чип-фильтр: при выборе появляется галочка, цвет перетекает. */
@Composable
fun NoodleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val colors = NoodleTheme.colors
    val bg by animateColorAsState(if (selected) colors.primary else colors.fillTertiary, tween(220), label = "chipBg")
    val fg by animateColorAsState(if (selected) colors.onPrimary else colors.label, tween(220), label = "chipFg")
    Row(
        modifier
            .noodleClickable(pressedScale = 0.94f, role = Role.Button, onClick = onClick)
            .clip(NoodleShapes.Capsule)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) NoodleIcon(leadingIcon, Modifier.padding(end = 6.dp), tint = fg, iconSize = 16.dp)
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(NoodleMotion.snappy()),
            exit = fadeOut() + shrinkHorizontally(NoodleMotion.snappy()),
        ) {
            Canvas(Modifier.padding(end = 6.dp).size(12.dp)) {
                val s = this.size.minDimension
                val p = Path().apply {
                    moveTo(s * 0.1f, s * 0.55f); lineTo(s * 0.4f, s * 0.85f); lineTo(s * 0.92f, s * 0.2f)
                }
                drawPath(p, fg, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        NoodleText(text, style = NoodleTheme.typography.subhead.copy(fontWeight = FontWeight.Medium), color = fg, maxLines = 1)
    }
}

/** Контурный тег / статус. */
@Composable
fun NoodleTag(text: String, modifier: Modifier = Modifier, color: Color = NoodleTheme.colors.primary) {
    NoodleText(
        text,
        modifier
            .clip(NoodleShapes.Capsule)
            .background(color.copy(alpha = 0.14f))
            .border(0.5.dp, color.copy(alpha = 0.35f), NoodleShapes.Capsule)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = NoodleTheme.typography.caption1.copy(fontWeight = FontWeight.SemiBold),
        color = color,
        maxLines = 1,
    )
}
