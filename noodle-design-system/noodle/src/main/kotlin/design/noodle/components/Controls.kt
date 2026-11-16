package design.noodle.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.ink
import design.noodle.foundation.inkPressable
import design.noodle.foundation.noRippleClickable
import design.noodle.foundation.rememberNoodleHaptics
import design.noodle.foundation.rememberPress
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme
import kotlin.math.roundToInt

/** Переключатель: чернильная «пилюля», круглый бегунок щёлкает с пружинным перелётом. */
@Composable
fun NoodleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = NoodleTheme.colors
    val haptics = rememberNoodleHaptics()
    var pressed by remember { mutableStateOf(false) }
    val trackW = 58.dp
    val trackH = 34.dp
    val pad = 3.dp
    val thumb = 24.dp
    val targetW = if (pressed) 30.dp else thumb
    val thumbW by animateDpAsState(targetW, NoodleMotion.snappy(), label = "thumbW")
    val thumbX by animateDpAsState(
        if (checked) trackW - pad * 2 - 2.dp - targetW + 1.dp else pad - 1.dp,
        spring(dampingRatio = 0.45f, stiffness = 520f), label = "thumbX",
    )
    val track by animateColorAsState(if (checked) colors.primary else colors.switchOff, tween(180), label = "track")
    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)

    Box(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .size(trackW + 3.dp, trackH + 3.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try { tryAwaitRelease() } finally { pressed = false }
                    },
                    onTap = { haptics.tick(); currentOnChange(!currentChecked) },
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total > 6f && !currentChecked) { haptics.tick(); currentOnChange(true) }
                        else if (total < -6f && currentChecked) { haptics.tick(); currentOnChange(false) }
                    },
                ) { change, delta -> change.consume(); total += delta }
            },
    ) {
        Box(Modifier.size(trackW, trackH).ink(NoodleShapes.Capsule, track, elevation = 3.dp, border = 2.dp))
        Box(
            Modifier
                .offset { IntOffset(thumbX.roundToPx(), ((trackH - thumb) / 2).roundToPx()) }
                .size(thumbW, thumb)
                .ink(NoodleShapes.Capsule, colors.controlThumb, elevation = 0.dp, border = 2.dp),
        )
    }
}

/** Сегментированный контрол: выбранный сегмент — жёлтый «жетон» с контуром, скользит пружиной. */
@Composable
fun NoodleSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NoodleTheme.colors
    val haptics = rememberNoodleHaptics()
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .ink(SquircleShape(15.dp), colors.surface, elevation = 3.dp)
            .padding(4.dp),
    ) {
        val segW = maxWidth / options.size
        val x by animateDpAsState(segW * selectedIndex, spring(dampingRatio = 0.62f, stiffness = 520f), label = "segX")
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(segW)
                .fillMaxHeight()
                .ink(SquircleShape(11.dp), colors.primarySecondary, elevation = 0.dp, border = 2.dp),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                val c by animateColorAsState(
                    if (selected) colors.onPrimary else colors.labelSecondary, tween(160), label = "segText",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .noRippleClickable { if (!selected) { haptics.tick(); onSelect(i) } },
                    contentAlignment = Alignment.Center,
                ) {
                    NoodleText(
                        label,
                        style = NoodleTheme.typography.subhead.copy(fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold),
                        color = c,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Слайдер: толстая чернильная дорожка, заливка цветом, бегунок с жёсткой тенью. */
@Composable
fun NoodleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    val colors = NoodleTheme.colors
    val haptics = rememberNoodleHaptics()
    val thumbPx = with(LocalDensity.current) { 30.dp.toPx() }
    var width by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    val span = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val lift by animateFloatAsState(if (dragging) 1f else 0f, spring(0.5f, 700f), label = "lift")
    val currentOnChange by rememberUpdatedState(onValueChange)
    val lastStep = remember { mutableIntStateOf(-1) }

    fun valueAt(x: Float): Float {
        val usable = (width - thumbPx).coerceAtLeast(1f)
        var f = ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f)
        if (steps > 0) f = (f * (steps + 1)).roundToInt().toFloat() / (steps + 1)
        return valueRange.start + f * span
    }

    fun emit(x: Float) {
        val v = valueAt(x)
        if (steps > 0) {
            val idx = (((v - valueRange.start) / span) * (steps + 1)).roundToInt()
            if (idx != lastStep.intValue) { lastStep.intValue = idx; haptics.tick() }
        }
        currentOnChange(v)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .onSizeChanged { width = it.width }
            .pointerInput(enabled, steps) { if (enabled) detectTapGestures { emit(it.x) } }
            .pointerInput(enabled, steps) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { dragging = true; emit(it.x) },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ -> change.consume(); emit(change.position.x) }
            },
    ) {
        // дорожка + заливка
        val density = LocalDensity.current
        val fillDp = with(density) { (thumbPx / 2f + fraction * (width - thumbPx)).toDp() }
        Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(18.dp).ink(NoodleShapes.Capsule, colors.surface, elevation = 3.dp)) {
            Box(
                Modifier
                    .padding(2.dp)
                    .fillMaxHeight()
                    .width((fillDp - 4.dp).coerceAtLeast(14.dp))
                    .clip(NoodleShapes.Capsule)
                    .background(colors.primary),
            )
        }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((fraction * (width - thumbPx)).roundToInt(), -(lift * 3.dp.toPx()).roundToInt()) }
                .size(30.dp)
                .ink(CircleShape, colors.primarySecondary, elevation = 3.dp),
        )
    }
}

/** Степпер «− / +»: две чернильные кнопки. */
@Composable
fun NoodleStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..99,
) {
    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        StepperButton(plus = false, enabled = value > range.first) { onValueChange(value - 1) }
        StepperButton(plus = true, enabled = value < range.last) { onValueChange(value + 1) }
    }
}

@Composable
private fun StepperButton(plus: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = NoodleTheme.colors
    Box(
        Modifier
            .size(38.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .inkPressable(SquircleShape(12.dp), if (plus) colors.primary else colors.surface, enabled, elevation = 3.dp, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(14.dp)) {
            val w = 2.8.dp.toPx()
            drawLine(colors.outline, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), w, StrokeCap.Round)
            if (plus) drawLine(colors.outline, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), w, StrokeCap.Round)
        }
    }
}
