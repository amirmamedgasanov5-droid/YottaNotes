package design.noodle.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.noRippleClickable
import design.noodle.foundation.pressScale
import design.noodle.foundation.rememberNoodleHaptics
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme
import kotlin.math.roundToInt

/** Переключатель в стиле iOS: «резиновый» бегунок растягивается при нажатии. */
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
    val trackW = 52.dp
    val trackH = 32.dp
    val pad = 2.dp
    val targetW = if (pressed) 36.dp else 28.dp
    val thumbW by animateDpAsState(targetW, NoodleMotion.snappy(), label = "thumbW")
    val thumbX by animateDpAsState(if (checked) trackW - pad - targetW else pad, NoodleMotion.snappy(), label = "thumbX")
    val track by animateColorAsState(if (checked) colors.primary else colors.switchOff, tween(240), label = "track")
    val currentChecked by rememberUpdatedState(checked)
    val currentOnChange by rememberUpdatedState(onCheckedChange)

    Box(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .size(trackW, trackH)
            .clip(NoodleShapes.Capsule)
            .background(track)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = {
                        haptics.tick()
                        currentOnChange(!currentChecked)
                    },
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total > 6f && !currentChecked) {
                            haptics.tick(); currentOnChange(true)
                        } else if (total < -6f && currentChecked) {
                            haptics.tick(); currentOnChange(false)
                        }
                    },
                ) { change, delta ->
                    change.consume()
                    total += delta
                }
            },
    ) {
        Box(
            Modifier
                .offset { IntOffset(thumbX.roundToPx(), pad.roundToPx()) }
                .size(thumbW, 28.dp)
                .shadow(3.dp, NoodleShapes.Capsule, ambientColor = Color.Black.copy(0.3f), spotColor = Color.Black.copy(0.3f))
                .background(Color.White, NoodleShapes.Capsule),
        )
    }
}

/** Сегментированный контрол со скользящим бегунком. */
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
            .height(36.dp)
            .clip(SquircleShape(10.dp))
            .background(colors.fillTertiary)
            .padding(2.dp),
    ) {
        val segW = maxWidth / options.size
        val x by animateDpAsState(segW * selectedIndex, NoodleMotion.snappy(), label = "segX")
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(segW)
                .fillMaxHeight()
                .shadow(2.dp, SquircleShape(8.dp), ambientColor = Color.Black.copy(0.25f), spotColor = Color.Black.copy(0.25f))
                .background(colors.controlThumb, SquircleShape(8.dp)),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                val c by animateColorAsState(if (selected) colors.label else colors.labelSecondary, tween(180), label = "segText")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .noRippleClickable {
                            if (!selected) {
                                haptics.tick()
                                onSelect(i)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    NoodleText(
                        label,
                        style = NoodleTheme.typography.subhead.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                        color = c,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Слайдер с «живым» бегунком. */
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
    val thumbPx = with(LocalDensity.current) { 28.dp.toPx() }
    var width by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    val span = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val scale by animateFloatAsState(if (dragging) 1.14f else 1f, NoodleMotion.bouncy(), label = "thumbScale")
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
            if (idx != lastStep.intValue) {
                lastStep.intValue = idx
                haptics.tick()
            }
        }
        currentOnChange(v)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .onSizeChanged { width = it.width }
            .pointerInput(enabled, steps) {
                if (!enabled) return@pointerInput
                detectTapGestures { emit(it.x) }
            }
            .pointerInput(enabled, steps) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { dragging = true; emit(it.x) },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    change.consume()
                    emit(change.position.x)
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val h = 6.dp.toPx()
                    val y = this.size.height / 2f - h / 2f
                    val r = CornerRadius(h / 2f)
                    drawRoundRect(colors.fillSecondary, Offset(0f, y), Size(this.size.width, h), r)
                    val activeW = thumbPx / 2f + fraction * (this.size.width - thumbPx)
                    drawRoundRect(colors.gradient, Offset(0f, y), Size(activeW, h), r)
                },
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((fraction * (width - thumbPx)).roundToInt(), 0) }
                .size(28.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(5.dp, CircleShape, ambientColor = Color.Black.copy(0.35f), spotColor = Color.Black.copy(0.35f))
                .background(Color.White, CircleShape),
        )
    }
}

/** Степпер «− / +». */
@Composable
fun NoodleStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..99,
) {
    val colors = NoodleTheme.colors
    val haptics = rememberNoodleHaptics()
    Row(
        modifier
            .height(32.dp)
            .clip(SquircleShape(9.dp))
            .background(colors.fillTertiary),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperHalf(plus = false, enabled = value > range.first) {
            haptics.tick(); onValueChange(value - 1)
        }
        Box(Modifier.width(0.5.dp).height(16.dp).background(colors.separator))
        StepperHalf(plus = true, enabled = value < range.last) {
            haptics.tick(); onValueChange(value + 1)
        }
    }
}

@Composable
private fun StepperHalf(plus: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = NoodleTheme.colors
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier
            .width(47.dp)
            .fillMaxHeight()
            .pressScale(source, 0.86f, enabled)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val tint = if (enabled) colors.label else colors.labelTertiary
        Canvas(Modifier.size(14.dp)) {
            val w = 2.dp.toPx()
            drawLine(tint, Offset(0f, this.size.height / 2f), Offset(this.size.width, this.size.height / 2f), w, StrokeCap.Round)
            if (plus) drawLine(tint, Offset(this.size.width / 2f, 0f), Offset(this.size.width / 2f, this.size.height), w, StrokeCap.Round)
        }
    }
}
