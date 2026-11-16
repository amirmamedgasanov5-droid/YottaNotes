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

/** Чекбокс: тонкий квадрат; при выборе заливается и галочка рисуется. */
@Composable
fun NoodleCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    diameter: Dp = 22.dp,
) {
    val colors = NoodleTheme.colors
    val fill by animateColorAsState(if (checked) colors.label else colors.surface, tween(160), label = "cbFill")
    val edge by animateColorAsState(if (checked) colors.label else colors.labelQuaternary, tween(160), label = "cbEdge")
    val check by animateFloatAsState(if (checked) 1f else 0f, tween(240, delayMillis = if (checked) 40 else 0), label = "check")
    val measure = remember { PathMeasure() }
    Box(
        modifier
            .size(diameter)
            .inkPressable(SquircleShape(7.dp), fill, enabled, border = 0.dp, role = Role.Checkbox, haptic = true, onClick = { onCheckedChange(!checked) })
            .ink(SquircleShape(7.dp), Color.Transparent, border = 1.dp, outline = edge),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(diameter)) {
            if (check > 0f) {
                val s = size.minDimension
                val path = Path().apply {
                    moveTo(s * 0.27f, s * 0.52f); lineTo(s * 0.43f, s * 0.68f); lineTo(s * 0.74f, s * 0.32f)
                }
                measure.setPath(path, false)
                val dst = Path()
                measure.getSegment(0f, measure.length * check.coerceIn(0f, 1f), dst, true)
                drawPath(dst, colors.background, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** Радио: тонкое кольцо, внутри плавно вырастает точка. */
@Composable
fun NoodleRadio(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    diameter: Dp = 22.dp,
) {
    val colors = NoodleTheme.colors
    val edge by animateColorAsState(if (selected) colors.label else colors.labelQuaternary, tween(160), label = "rdEdge")
    val dot by animateFloatAsState(if (selected) 1f else 0f, spring(0.7f, 500f), label = "dot")
    Box(
        modifier
            .size(diameter)
            .inkPressable(CircleShape, colors.surface, enabled, border = 0.dp, role = Role.RadioButton, onClick = onClick)
            .ink(CircleShape, Color.Transparent, border = 1.dp, outline = edge),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(diameter * 0.46f)
                .graphicsLayer { scaleX = dot; scaleY = dot }
                .ink(CircleShape, colors.label, border = 0.dp),
        )
    }
}

/** Чип: серая плашка; выбранный — белый с тонкой акцентной рамкой. */
@Composable
fun NoodleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val colors = NoodleTheme.colors
    val fill by animateColorAsState(if (selected) colors.surface else colors.fillTertiary, tween(180), label = "chipBg")
    val edge by animateColorAsState(if (selected) colors.primary else Color.Transparent, tween(180), label = "chipEdge")
    Row(
        modifier
            .inkPressable(NoodleShapes.Capsule, fill, border = 0.dp, onClick = onClick)
            .ink(NoodleShapes.Capsule, Color.Transparent, border = 1.dp, outline = edge, strokeWidth = 1.2.dp)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) NoodleIcon(leadingIcon, Modifier.padding(end = 6.dp), tint = colors.label, iconSize = 16.dp)
        NoodleText(text, style = NoodleTheme.typography.subhead.copy(fontWeight = FontWeight.Medium), color = colors.label, maxLines = 1)
    }
}

/** Тег / статус: тонированный текст на лёгкой плашке. */
@Composable
fun NoodleTag(text: String, modifier: Modifier = Modifier, color: Color = NoodleTheme.colors.labelSecondary) {
    NoodleText(
        text,
        modifier
            .ink(NoodleShapes.Capsule, color.copy(alpha = 0.12f), border = 0.dp)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        style = NoodleTheme.typography.caption1.copy(fontWeight = FontWeight.Medium),
        color = color,
        maxLines = 1,
    )
}
