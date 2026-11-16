package design.noodle.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.inkPressable
import design.noodle.foundation.rememberNoodleHaptics
import design.noodle.foundation.rememberPress
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme

enum class NoodleButtonStyle { Filled, Tinted, Gray, Plain, Destructive }
enum class NoodleButtonSize { Large, Medium, Small }

/**
 * Кнопка Noodle: плоская заливка, чернильный контур и жёсткая тень.
 * При нажатии физически «вдавливается» в тень.
 */
@Composable
fun NoodleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NoodleButtonStyle = NoodleButtonStyle.Filled,
    size: NoodleButtonSize = NoodleButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val colors = NoodleTheme.colors
    val type = NoodleTheme.typography
    val fill = when (style) {
        NoodleButtonStyle.Filled -> colors.primary
        NoodleButtonStyle.Tinted -> colors.primarySecondary
        NoodleButtonStyle.Gray -> colors.surface
        NoodleButtonStyle.Plain -> Color.Transparent
        NoodleButtonStyle.Destructive -> colors.danger
    }
    val content = when (style) {
        NoodleButtonStyle.Plain -> colors.label
        NoodleButtonStyle.Gray -> colors.label
        else -> colors.onPrimary
    }
    val height: Dp = when (size) {
        NoodleButtonSize.Large -> 54.dp
        NoodleButtonSize.Medium -> 46.dp
        NoodleButtonSize.Small -> 36.dp
    }
    val textStyle = when (size) {
        NoodleButtonSize.Large -> type.headline.copy(fontSize = 17.sp)
        NoodleButtonSize.Medium -> type.headline
        NoodleButtonSize.Small -> type.subhead.copy(fontWeight = FontWeight.ExtraBold)
    }
    val shape = if (size == NoodleButtonSize.Large) SquircleShape(16.dp) else NoodleShapes.Capsule
    val elevation = when (size) {
        NoodleButtonSize.Large -> 5.dp
        NoodleButtonSize.Medium -> 4.dp
        NoodleButtonSize.Small -> 3.dp
    }
    val hPad = if (size == NoodleButtonSize.Small) 14.dp else 22.dp
    val plain = style == NoodleButtonStyle.Plain

    Box(
        modifier
            .defaultMinSize(minWidth = 44.dp)
            .height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .then(
                if (plain) {
                    val source = remember { MutableInteractionSource() }
                    val haptics = rememberNoodleHaptics()
                    val pressed by source.collectIsPressedAsState()
                    val a by animateFloatAsState(if (pressed) 0.45f else 1f, tween(100), label = "plainAlpha")
                    Modifier
                        .graphicsLayer { alpha = a }
                        .clickable(source, null, enabled = enabled && !loading, role = Role.Button) {
                            haptics.tick(); onClick()
                        }
                } else {
                    Modifier.inkPressable(
                        shape = shape,
                        fill = fill,
                        enabled = enabled && !loading,
                        elevation = elevation,
                        onClick = onClick,
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.padding(horizontal = hPad).animateContentSize(NoodleMotion.snappy()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = loading,
                enter = fadeIn() + expandHorizontally(NoodleMotion.snappy()),
                exit = fadeOut() + shrinkHorizontally(NoodleMotion.snappy()),
            ) {
                NoodleSpinner(Modifier.padding(end = 10.dp), diameter = 18.dp, color = content)
            }
            if (icon != null && !loading) {
                NoodleIcon(icon, Modifier.padding(end = 8.dp), tint = content, iconSize = 20.dp)
            }
            NoodleText(
                text,
                style = textStyle.copy(textDecoration = if (plain) TextDecoration.Underline else TextDecoration.None),
                color = content,
                maxLines = 1,
            )
        }
    }
}


/** Круглая кнопка-иконка с чернильным контуром. */
@Composable
fun NoodleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NoodleTheme.colors.label,
    container: Color = NoodleTheme.colors.surface,
    diameter: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    enabled: Boolean = true,
) {
    val transparent = container == Color.Transparent
    Box(
        modifier
            .size(diameter)
            .then(
                if (transparent) {
                    val haptics = rememberNoodleHaptics()
                    val source = remember { MutableInteractionSource() }
                    val press = rememberPress(source, enabled)
                    Modifier
                        .graphicsLayer { scaleX = 1f - 0.12f * press.value; scaleY = 1f - 0.12f * press.value }
                        .clickable(source, null, enabled = enabled, role = Role.Button) { haptics.tick(); onClick() }
                } else {
                    Modifier.inkPressable(NoodleShapes.Capsule, container, enabled, elevation = 3.dp, onClick = onClick)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        NoodleIcon(icon, tint = tint, iconSize = iconSize)
    }
}

/** Текстовая кнопка для панелей. */
@Composable
fun NoodleTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bold: Boolean = true,
    color: Color = NoodleTheme.colors.label,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val alpha by animateFloatAsState(if (pressed) 0.4f else if (enabled) 1f else 0.35f, tween(120), label = "alpha")
    NoodleText(
        text,
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        style = NoodleTheme.typography.body.copy(
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
        ),
        color = color,
    )
}
