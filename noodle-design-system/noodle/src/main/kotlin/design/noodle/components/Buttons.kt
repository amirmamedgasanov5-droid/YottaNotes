package design.noodle.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.pressScale
import design.noodle.foundation.rememberNoodleHaptics
import design.noodle.foundation.softShadow
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme

enum class NoodleButtonStyle { Filled, Tinted, Gray, Plain, Destructive }
enum class NoodleButtonSize { Large, Medium, Small }

/**
 * Основная кнопка. Пружинное нажатие, затемнение, тактильный отклик,
 * плавный переход в состояние загрузки.
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
    val haptics = rememberNoodleHaptics()

    val containerBrush: Brush
    val content: Color
    when (style) {
        NoodleButtonStyle.Filled -> {
            containerBrush = colors.gradient
            content = colors.onPrimary
        }
        NoodleButtonStyle.Tinted -> {
            containerBrush = Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.14f), colors.primary.copy(alpha = 0.14f)))
            content = colors.primary
        }
        NoodleButtonStyle.Gray -> {
            containerBrush = Brush.linearGradient(listOf(colors.fillTertiary, colors.fillTertiary))
            content = colors.primary
        }
        NoodleButtonStyle.Plain -> {
            containerBrush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            content = colors.primary
        }
        NoodleButtonStyle.Destructive -> {
            containerBrush = Brush.linearGradient(listOf(colors.danger.copy(alpha = 0.14f), colors.danger.copy(alpha = 0.14f)))
            content = colors.danger
        }
    }
    val height: Dp = when (size) {
        NoodleButtonSize.Large -> 52.dp
        NoodleButtonSize.Medium -> 44.dp
        NoodleButtonSize.Small -> 34.dp
    }
    val textStyle = when (size) {
        NoodleButtonSize.Large -> type.headline
        NoodleButtonSize.Medium -> type.callout.copy(fontWeight = FontWeight.SemiBold)
        NoodleButtonSize.Small -> type.subhead.copy(fontWeight = FontWeight.SemiBold)
    }
    val shape = if (size == NoodleButtonSize.Large) SquircleShape(16.dp) else NoodleShapes.Capsule
    val hPad = when (size) {
        NoodleButtonSize.Large -> 20.dp
        NoodleButtonSize.Medium -> 20.dp
        NoodleButtonSize.Small -> 14.dp
    }

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val overlay by animateFloatAsState(if (pressed) 1f else 0f, tween(110), label = "overlay")
    val glow = style == NoodleButtonStyle.Filled && size != NoodleButtonSize.Small

    Box(
        modifier
            .defaultMinSize(minWidth = 44.dp)
            .height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .pressScale(source, 0.965f, enabled && !loading)
            .then(if (glow) Modifier.softShadow(10.dp, shape, colors.primary) else Modifier)
            .clip(shape)
            .background(containerBrush)
            .drawWithContent {
                drawContent()
                if (overlay > 0f) drawRect(Color.Black.copy(alpha = 0.12f * overlay))
            }
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
            ) {
                haptics.tick()
                onClick()
            },
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
            NoodleText(text, style = textStyle, color = content, maxLines = 1)
        }
    }
}

/** Круглая кнопка-иконка (панели навигации, карточки). */
@Composable
fun NoodleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NoodleTheme.colors.primary,
    container: Color = NoodleTheme.colors.fillTertiary,
    diameter: Dp = 36.dp,
    iconSize: Dp = 20.dp,
    enabled: Boolean = true,
) {
    val haptics = rememberNoodleHaptics()
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(diameter)
            .pressScale(source, 0.88f, enabled)
            .clip(NoodleShapes.Capsule)
            .background(container)
            .clickable(source, null, enabled = enabled, role = Role.Button) {
                haptics.tick()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        NoodleIcon(icon, tint = tint, iconSize = iconSize)
    }
}

/** Текстовая кнопка для панелей (как «Готово» в iOS). */
@Composable
fun NoodleTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    color: Color = NoodleTheme.colors.primary,
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
        style = NoodleTheme.typography.body.copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal),
        color = color,
    )
}
