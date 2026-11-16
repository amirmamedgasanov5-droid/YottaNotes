package design.noodle.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import design.noodle.foundation.Hairline
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.rememberNoodleHaptics
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun NoodleDivider(modifier: Modifier = Modifier, color: Color = NoodleTheme.colors.separator) {
    Box(modifier.fillMaxWidth().height(Hairline).background(color))
}

class NoodleGroupScope {
    internal val rows = mutableListOf<Pair<Any?, @Composable () -> Unit>>()

    /** [key] нужен для динамических списков (удаление/перестановка), чтобы состояние строк не «съезжало». */
    fun row(key: Any? = null, content: @Composable () -> Unit) {
        rows.add(key to content)
    }
}

/**
 * Сгруппированный «inset» список (как Настройки в iOS): скруглённая карточка,
 * между строками — тонкие разделители, сверху заголовок, снизу пояснение.
 */
@Composable
fun NoodleGroup(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    dividerInset: Dp = 16.dp,
    content: NoodleGroupScope.() -> Unit,
) {
    val colors = NoodleTheme.colors
    val type = NoodleTheme.typography
    val scope = NoodleGroupScope().apply(content)
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (header != null) {
            NoodleText(
                header.uppercase(),
                Modifier.padding(start = 16.dp, bottom = 7.dp),
                style = type.footnote,
                color = colors.labelSecondary,
            )
        }
        Column(Modifier.fillMaxWidth().clip(SquircleShape(20.dp)).background(colors.surface)) {
            scope.rows.forEachIndexed { index, (rowKey, row) ->
                key(rowKey ?: index) { row() }
                if (index < scope.rows.lastIndex) NoodleDivider(Modifier.padding(start = dividerInset))
            }
        }
        if (footer != null) {
            NoodleText(
                footer,
                Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp),
                style = type.footnote,
                color = colors.labelSecondary,
            )
        }
    }
}

/** Строка списка: иконка-плитка, заголовок, подзаголовок, значение, шеврон, произвольный trailing. */
@Composable
fun NoodleRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = NoodleTheme.colors.primary,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = NoodleTheme.colors
    val type = NoodleTheme.typography
    val haptics = rememberNoodleHaptics()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) colors.fillTertiary else Color.Transparent, tween(if (pressed) 40 else 220), label = "rowBg")

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(bg)
            .then(
                if (onClick != null) Modifier.clickable(source, null, role = Role.Button) {
                    haptics.tick()
                    onClick()
                } else Modifier,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(Modifier.padding(end = 12.dp)) { leading() }
        } else if (icon != null) {
            Box(
                Modifier.padding(end = 12.dp).size(30.dp).clip(SquircleShape(8.dp)).background(iconTint),
                contentAlignment = Alignment.Center,
            ) { NoodleIcon(icon, tint = Color.White, iconSize = 18.dp) }
        }
        Column(Modifier.weight(1f)) {
            NoodleText(title, style = type.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                NoodleText(subtitle, style = type.footnote, color = colors.labelSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (value != null) {
            NoodleText(value, Modifier.padding(start = 8.dp), style = type.body, color = colors.labelSecondary, maxLines = 1)
        }
        if (trailing != null) Box(Modifier.padding(start = 10.dp)) { trailing() }
        if (showChevron) {
            NoodleIcon(Icons.Rounded.KeyboardArrowRight, Modifier.padding(start = 4.dp), tint = colors.labelTertiary, iconSize = 22.dp)
        }
    }
}

/**
 * Свайп влево: показывается красная кнопка «Удалить». Полный свайп удаляет сразу,
 * строка схлопывается анимацией.
 */
@Composable
fun NoodleSwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Удалить",
    content: @Composable () -> Unit,
) {
    val colors = NoodleTheme.colors
    val haptics = rememberNoodleHaptics()
    val actionPx = with(LocalDensity.current) { 88.dp.toPx() }
    var widthPx by remember { mutableIntStateOf(1) }
    var offset by remember { mutableFloatStateOf(0f) }
    var removed by remember { mutableStateOf(false) }

    LaunchedEffect(removed) {
        if (removed) {
            delay(320)
            onDelete()
        }
    }

    suspend fun settle(target: Float) {
        animate(offset, target, animationSpec = NoodleMotion.snappy()) { v, _ -> offset = v }
    }

    AnimatedVisibility(
        visible = !removed,
        exit = shrinkVertically(tween(300, easing = NoodleMotion.Emphasized)) + fadeOut(tween(220)),
    ) {
        Box(modifier.fillMaxWidth().onSizeChanged { widthPx = it.width }.clipToBounds()) {
            Box(
                Modifier.matchParentSize().background(colors.danger),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    Modifier
                        .width(88.dp)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, null) {
                            haptics.impact()
                            removed = true
                        }
                        .graphicsLayer { alpha = (-offset / actionPx).coerceIn(0f, 1f) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NoodleIcon(Icons.Rounded.Delete, tint = Color.White, iconSize = 22.dp)
                        NoodleText(label, style = NoodleTheme.typography.caption1.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                    }
                }
            }
            Box(
                Modifier
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .fillMaxWidth()
                    .background(colors.surface)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { d ->
                            offset = (offset + d).coerceIn(-widthPx.toFloat(), 0f)
                        },
                        onDragStopped = { v ->
                            when {
                                offset < -widthPx * 0.62f || v < -2800f -> {
                                    haptics.impact()
                                    settle(-widthPx.toFloat())
                                    removed = true
                                }
                                offset < -actionPx * 0.5f || v < -900f -> settle(-actionPx)
                                else -> settle(0f)
                            }
                        },
                    ),
            ) { content() }
        }
    }
}

@Composable
fun NoodleSectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.Bottom) {
        NoodleText(text, Modifier.weight(1f), style = NoodleTheme.typography.title3)
        if (action != null) {
            NoodleTextButton(action, onAction)
        }
    }
}
