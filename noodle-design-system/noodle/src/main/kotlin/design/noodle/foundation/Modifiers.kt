package design.noodle.foundation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.theme.LocalNoodleHapticsEnabled
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme

/** Тонкая линия 0.5dp — как разделители в iOS. */
val Hairline: Dp = 0.5.dp

/** Клик без ripple. */
fun Modifier.noRippleClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}

/** Пружинное «вдавливание» при касании. */
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.96f, enabled: Boolean = true): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = NoodleMotion.press(),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Клик: без ripple, с пружинным нажатием и (опционально) тактильным откликом. */
fun Modifier.noodleClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    haptic: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val haptics = rememberNoodleHaptics()
    this
        .pressScale(source, pressedScale, enabled)
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
        ) {
            if (haptic) haptics.tick()
            onClick()
        }
}

/** Мягкая тень, окрашенная в тёплый тон темы. */
@Composable
fun Modifier.softShadow(elevation: Dp, shape: Shape, tint: Color = NoodleTheme.colors.shadow): Modifier =
    this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = tint.copy(alpha = 0.55f),
        spotColor = tint.copy(alpha = 0.65f),
    )

fun Modifier.hairlineBottom(color: Color, startInset: Dp = 0.dp): Modifier = drawBehind {
    val h = Hairline.toPx()
    drawRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(startInset.toPx(), size.height - h),
        size = androidx.compose.ui.geometry.Size(size.width - startInset.toPx(), h),
    )
}

/** Тактильный отклик Noodle. */
@Stable
class NoodleHaptics internal constructor(
    private val feedback: HapticFeedback,
    private val enabled: Boolean,
) {
    fun tick() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun impact() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun rememberNoodleHaptics(): NoodleHaptics {
    val feedback = LocalHapticFeedback.current
    val enabled = LocalNoodleHapticsEnabled.current
    return remember(feedback, enabled) { NoodleHaptics(feedback, enabled) }
}
