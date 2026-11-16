package design.noodle.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import design.noodle.foundation.ink
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.noRippleClickable
import design.noodle.foundation.rememberNoodleHaptics
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme

/** Прогресс появления оверлея: 0 — скрыт, 1 — показан. Монтирует контент только пока он виден. */
@Stable
class NoodlePresence internal constructor(
    val progress: Animatable<Float, AnimationVector1D>,
    val mounted: Boolean,
)

@Composable
fun rememberPresence(visible: Boolean, enterSpec: AnimationSpec<Float> = NoodleMotion.smooth()): NoodlePresence {
    val progress = remember { Animatable(0f) }
    var mounted by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible) {
            mounted = true
            progress.animateTo(1f, enterSpec)
        } else {
            progress.animateTo(0f, tween(240, easing = NoodleMotion.Emphasized))
            mounted = false
        }
    }
    return NoodlePresence(progress, mounted)
}

@Composable
private fun Scrim(presence: NoodlePresence, onClick: () -> Unit) {
    val colors = NoodleTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = presence.progress.value.coerceIn(0f, 1f) }
            .background(colors.scrim)
            .noRippleClickable(onClick = onClick),
    )
}

/**
 * Модальная шторка: тянется вниз пальцем (со скоростью флинга), закрывается
 * по тапу на затемнение или кнопке «назад».
 */
@Composable
fun NoodleBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val presence = rememberPresence(visible)
    if (!presence.mounted) return
    val colors = NoodleTheme.colors
    var dragPx by remember { mutableFloatStateOf(0f) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    LaunchedEffect(visible) { if (visible) dragPx = 0f }
    BackHandler(enabled = visible, onBack = onDismissRequest)

    Box(Modifier.fillMaxSize()) {
        Scrim(presence, onDismissRequest)
        Column(
            modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { sheetHeight = it.height }
                .graphicsLayer { translationY = (1f - presence.progress.value) * sheetHeight + dragPx }
                .ink(NoodleShapes.Sheet, colors.surface, elevation = 0.dp, border = 2.5.dp)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { d -> dragPx = (dragPx + d).coerceAtLeast(0f) },
                    onDragStopped = { v ->
                        if (dragPx > sheetHeight * 0.3f || v > 1600f) {
                            onDismissRequest()
                        } else {
                            animate(dragPx, 0f, animationSpec = NoodleMotion.snappy()) { value, _ -> dragPx = value }
                        }
                    },
                )
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.padding(top = 10.dp, bottom = 8.dp).width(46.dp).height(8.dp).ink(NoodleShapes.Capsule, colors.primarySecondary, elevation = 0.dp, border = 2.dp),
            )
            content()
        }
    }
}

enum class NoodleAlertActionStyle { Default, Cancel, Destructive }

@Stable
class NoodleAlertAction(
    val label: String,
    val style: NoodleAlertActionStyle = NoodleAlertActionStyle.Default,
    val onClick: () -> Unit = {},
)

/** Алерт как в iOS: «выпрыгивает» из лёгкого увеличения, кнопки разделены линиями. */
@Composable
fun NoodleAlert(
    visible: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    actions: List<NoodleAlertAction>,
) {
    val presence = rememberPresence(visible, NoodleMotion.bouncy())
    if (!presence.mounted) return
    val colors = NoodleTheme.colors
    val type = NoodleTheme.typography
    val shape = SquircleShape(24.dp)
    BackHandler(enabled = visible, onBack = onDismissRequest)

    Box(Modifier.fillMaxSize()) {
        Scrim(presence, onDismissRequest)
        Column(
            modifier
                .align(Alignment.Center)
                .width(284.dp)
                .graphicsLayer {
                    val p = presence.progress.value
                    val s = 1.14f - 0.14f * p
                    scaleX = s
                    scaleY = s
                    alpha = (p * 2.2f).coerceIn(0f, 1f)
                }
                .ink(shape, colors.surface, elevation = 8.dp, border = 2.5.dp)
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                NoodleText(title, style = type.title3, textAlign = TextAlign.Center)
                if (message != null) {
                    NoodleText(message, Modifier.padding(top = 4.dp), style = type.footnote, color = colors.labelSecondary, textAlign = TextAlign.Center)
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(colors.outline))
            if (actions.size == 2) {
                Row(Modifier.fillMaxWidth().height(48.dp)) {
                    AlertButton(actions[0], Modifier.weight(1f), onDismissRequest)
                    Box(Modifier.width(2.dp).height(48.dp).background(colors.outline))
                    AlertButton(actions[1], Modifier.weight(1f), onDismissRequest)
                }
            } else {
                actions.forEachIndexed { i, a ->
                    AlertButton(a, Modifier.fillMaxWidth(), onDismissRequest)
                    if (i < actions.lastIndex) Box(Modifier.fillMaxWidth().height(2.dp).background(colors.outline))
                }
            }
        }
    }
}

@Composable
private fun AlertButton(action: NoodleAlertAction, modifier: Modifier, dismiss: () -> Unit) {
    val colors = NoodleTheme.colors
    val haptics = rememberNoodleHaptics()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) colors.primarySecondary else Color.Transparent, tween(60), label = "alertBtn")
    val color = if (action.style == NoodleAlertActionStyle.Destructive) colors.danger else colors.label
    Box(
        modifier
            .height(48.dp)
            .background(bg)
            .clickable(source, null) {
                if (action.style == NoodleAlertActionStyle.Destructive) haptics.impact() else haptics.tick()
                action.onClick()
                dismiss()
            },
        contentAlignment = Alignment.Center,
    ) {
        NoodleText(
            action.label,
            style = NoodleTheme.typography.body.copy(fontWeight = if (action.style == NoodleAlertActionStyle.Cancel) FontWeight.Black else FontWeight.ExtraBold),
            color = color,
            maxLines = 1,
        )
    }
}

/** Action Sheet: группа действий и отдельная кнопка «Отмена», выезжает снизу. */
@Composable
fun NoodleActionSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    actions: List<NoodleAlertAction>,
    modifier: Modifier = Modifier,
    title: String? = null,
    cancelLabel: String = "Отмена",
) {
    val presence = rememberPresence(visible, NoodleMotion.snappy())
    if (!presence.mounted) return
    val colors = NoodleTheme.colors
    var h by remember { mutableIntStateOf(0) }
    val shape = SquircleShape(22.dp)
    BackHandler(enabled = visible, onBack = onDismissRequest)

    Box(Modifier.fillMaxSize()) {
        Scrim(presence, onDismissRequest)
        Column(
            modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { h = it.height }
                .graphicsLayer { translationY = (1f - presence.progress.value) * h }
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(end = 5.dp, bottom = 5.dp).ink(shape, colors.surface, elevation = 5.dp, border = 2.5.dp).clip(shape)) {
                if (title != null) {
                    NoodleText(
                        title,
                        Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 16.dp),
                        style = NoodleTheme.typography.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.labelSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Box(Modifier.fillMaxWidth().height(2.dp).background(colors.outline))
                }
                actions.forEachIndexed { i, a ->
                    AlertButton(a, Modifier.fillMaxWidth().height(56.dp), onDismissRequest)
                    if (i < actions.lastIndex) Box(Modifier.fillMaxWidth().height(2.dp).background(colors.outline))
                }
            }
            Box(Modifier.fillMaxWidth().padding(end = 5.dp, bottom = 5.dp).ink(shape, colors.primarySecondary, elevation = 5.dp, border = 2.5.dp).clip(shape)) {
                AlertButton(
                    NoodleAlertAction(cancelLabel, NoodleAlertActionStyle.Cancel),
                    Modifier.fillMaxWidth().height(56.dp),
                    onDismissRequest,
                )
            }
        }
    }
}

@Stable
class NoodleMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Контекстное меню: разворачивается из угла кнопки-якоря с пружиной.
 * Разместите внутри Box вместе с якорем.
 */
@Composable
fun NoodleMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<NoodleMenuItem>,
    modifier: Modifier = Modifier,
) {
    if (!expanded) return
    val colors = NoodleTheme.colors
    val haptics = rememberNoodleHaptics()
    val density = LocalDensity.current
    val pad = with(density) { 20.dp.roundToPx() }
    val gap = with(density) { 2.dp.roundToPx() }
    val provider = remember(pad, gap) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = (anchorBounds.right - popupContentSize.width + pad).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                val y = anchorBounds.bottom - pad + gap
                return IntOffset(x, y)
            }
        }
    }
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, NoodleMotion.bouncy()) }
    val shape = SquircleShape(18.dp)

    Popup(popupPositionProvider = provider, onDismissRequest = onDismissRequest, properties = PopupProperties(focusable = true)) {
        Column(
            modifier
                .padding(20.dp)
                .graphicsLayer {
                    val s = 0.55f + 0.45f * p.value
                    scaleX = s
                    scaleY = s
                    alpha = (p.value * 2f).coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(1f, 0f)
                }
                .width(224.dp)
                .ink(shape, colors.surface, elevation = 5.dp, border = 2.5.dp)
                .clip(shape),
        ) {
            items.forEachIndexed { i, item ->
                val source = remember { MutableInteractionSource() }
                val pressed by source.collectIsPressedAsState()
                val bg by animateColorAsState(if (pressed) colors.primarySecondary else Color.Transparent, tween(60), label = "menuBg")
                val tint = if (item.destructive) colors.danger else colors.label
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(bg)
                        .clickable(source, null) {
                            haptics.tick()
                            item.onClick()
                            onDismissRequest()
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NoodleText(item.label, Modifier.weight(1f), style = NoodleTheme.typography.body.copy(fontWeight = FontWeight.ExtraBold), color = tint, maxLines = 1)
                    if (item.icon != null) NoodleIcon(item.icon, tint = tint, iconSize = 20.dp)
                }
                if (i < items.lastIndex) Box(Modifier.fillMaxWidth().height(2.dp).background(colors.outline))
            }
        }
    }
}
