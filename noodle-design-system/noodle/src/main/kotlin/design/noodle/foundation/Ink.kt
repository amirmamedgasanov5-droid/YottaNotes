package design.noodle.foundation

import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.theme.NoodleTheme

/**
 * Фирменная «чернильная» поверхность Noodle:
 * плоская заливка + контур + жёсткая смещённая тень (без размытия).
 * [pressed] (0..1) вдавливает поверхность в тень — как физическая кнопка.
 */
@Composable
fun Modifier.ink(
    shape: Shape,
    fill: Color,
    pressed: () -> Float = { 0f },
    elevation: Dp = 4.dp,
    border: Dp = 2.dp,
    outline: Color = NoodleTheme.colors.outline,
    shadowColor: Color = NoodleTheme.colors.hardShadow,
): Modifier {
    val d = with(LocalDensity.current) { elevation.toPx() }
    return this
        .graphicsLayer {
            val p = pressed()
            translationX = d * p
            translationY = d * p
        }
        .drawBehind {
            if (d > 0f) {
                val o = shape.createOutline(size, layoutDirection, this)
                val off = d * (1f - pressed())
                translate(off, off) { drawOutline(o, shadowColor) }
            }
        }
        .background(fill, shape)
        .then(if (border > 0.dp) Modifier.border(border, outline, shape) else Modifier)
}

/** Состояние нажатия 0..1 с упругой пружиной. */
@Composable
fun rememberPress(source: MutableInteractionSource, enabled: Boolean = true): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "inkPress",
    )
}

/** «Чернильная» кликабельная поверхность: вдавливается в тень, вибрирует. */
@Composable
fun Modifier.inkPressable(
    shape: Shape,
    fill: Color,
    enabled: Boolean = true,
    elevation: Dp = 4.dp,
    border: Dp = 2.dp,
    haptic: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val press = rememberPress(source, enabled)
    val haptics = rememberNoodleHaptics()
    return this
        .ink(shape, fill, { press.value }, elevation, border)
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role) {
            if (haptic) haptics.tick()
            onClick()
        }
}
