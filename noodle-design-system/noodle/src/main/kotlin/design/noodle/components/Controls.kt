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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.shadow
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
    val trackW = 50.dp
    val trackH = 30.dp
    val pad = 3.dp
    val thumb = 24.dp
    val targetW = if (pressed) 29.dp else thumb
    val thumbW by animateDpAsState(targetW, NoodleMotion.snappy(), label = "thumbW")
    val thumbX by animateDpAsState(
        if (checked) trackW - pad - targetW else pad,
        spring(dampingRatio = 0.6f, stiffness = 480f), label = "thumbX",
    )
    val track by animateColorAsState(if (checked) colors.label else colors.switchOff, tween(200), label = "track")
    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)

    Box(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .size(trackW, trackH)
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
        Box(Modifier.size(trackW, trackH).ink(NoodleShapes.Capsule, track, border = 0.dp))
        Box(
            Modifier
                .offset { IntOffset(thumbX.roundToPx(), ((trackH - thumb) / 2).roundToPx()) }
                .size(thumbW, thumb)
                .ink(NoodleShapes.Capsule, colors.controlThumb, border = 0.dp),
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
            .height(40.dp)
            .ink(NoodleShapes.Capsule, colors.fillTertiary, border = 0.dp)
            .padding(3.dp),
    ) {
        val segW = maxWidth / options.size
        val x by animateDpAsState(segW * selectedIndex, spring(dampingRatio = 0.62f, stiffness = 520f), label = "segX")
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(segW)
                .fillMaxHeight()
                .ink(NoodleShapes.Capsule, colors.surface, border = 1.dp),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                val c by animateColorAsState(
                    if (selected) colors.label else colors.labelSecondary, tween(160), label = "segText",
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
                        style = NoodleTheme.typography.subhead.copy(fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal),
                        color = c,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Слайдер-нить: в покое — тонкая прямая линия с бусиной, при перетаскивании
 * пройденная часть оживает волной, а отпустили — пружиной выпрямляется.
 */
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
    val thumbPx = with(LocalDensity.current) { 20.dp.toPx() }
    var width by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    val span = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val amp = remember { Animatable(0f) }
    LaunchedEffect(dragging) { amp.animateTo(if (dragging) 1f else 0f, spring(dampingRatio = 0.4f, stiffness = 120f)) }
    val phase by rememberThreadPhase(1300)
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

    Canvas(
        modifier
            .fillMaxWidth()
            .height(44.dp)
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
        val cy = size.height / 2f
        val r = thumbPx / 2f
        val x = r + fraction * (size.width - thumbPx)
        val sw = 2.dp.toPx()
        val wl = 16.dp.toPx()
        val a = amp.value * 5.dp.toPx()
        val x0 = sw
        drawLine(colors.fill, Offset(x, cy), Offset(size.width - sw, cy), sw, StrokeCap.Round)
        squiggle(colors.label, sw, a, wl, -phase, y = cy, x0 = x0, x1 = x)
        val ty = cy + a * kotlin.math.sin(((x - x0) / wl) * 2f * Math.PI.toFloat() - phase)
        val grow = 1f + 0.22f * amp.value.coerceIn(0f, 1f)
        drawCircle(colors.label, r * grow, Offset(x, ty))
        drawCircle(colors.background, r * 0.34f * grow, Offset(x, ty))
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
    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        StepperButton(plus = false, enabled = value > range.first) { onValueChange(value - 1) }
        StepperButton(plus = true, enabled = value < range.last) { onValueChange(value + 1) }
    }
}

@Composable
private fun StepperButton(plus: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = NoodleTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .inkPressable(NoodleShapes.Capsule, colors.fillTertiary, enabled, border = 0.dp, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(14.dp)) {
            val w = 1.6.dp.toPx()
            drawLine(colors.label, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), w, StrokeCap.Round)
            if (plus) drawLine(colors.label, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), w, StrokeCap.Round)
        }
    }
}
