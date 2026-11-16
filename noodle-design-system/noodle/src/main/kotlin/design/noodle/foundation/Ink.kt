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
 * Поверхность Noodle: плоская заливка и тончайший контур, без теней.
 * [pressed] (0..1) мягко уменьшает и приглушает элемент при касании.
 */
@Composable
fun Modifier.ink(
    shape: Shape,
    fill: Color,
    pressed: () -> Float = { 0f },
    elevation: Dp = 0.dp,
    border: Dp = 1.dp,
    outline: Color = NoodleTheme.colors.outline,
    shadowColor: Color = Color.Transparent,
    strokeWidth: Dp? = null,
): Modifier {
    val w = strokeWidth ?: if (border > 0.dp) 1.dp else 0.dp
    return this
        .graphicsLayer {
            val p = pressed()
            val sc = 1f - 0.03f * p
            scaleX = sc
            scaleY = sc
            alpha = 1f - 0.1f * p
        }
        .background(fill, shape)
        .then(if (w > 0.dp) Modifier.border(w, outline, shape) else Modifier)
}

/** Состояние нажатия 0..1 с упругой пружиной. */
@Composable
fun rememberPress(source: MutableInteractionSource, enabled: Boolean = true): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "inkPress",
    )
}

/** «Чернильная» кликабельная поверхность: вдавливается в тень, вибрирует. */
@Composable
fun Modifier.inkPressable(
    shape: Shape,
    fill: Color,
    enabled: Boolean = true,
    elevation: Dp = 0.dp,
    border: Dp = 1.dp,
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
