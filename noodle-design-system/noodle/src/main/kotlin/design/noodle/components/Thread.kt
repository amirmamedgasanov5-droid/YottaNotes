package design.noodle.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import kotlin.math.PI

/*
 * Принцип «нити» Noodle: в покое линия прямая и тонкая, а когда элемент «живой»
 * (двигается, выбран, фокусирован) — она волнится и пружиной успокаивается обратно.
 */

/** Всплеск амплитуды (0..peak), который затухает пружиной при каждой смене [trigger]. */
@Composable
internal fun rememberPulse(trigger: Any?, peak: Float = 1f): State<Float> {
    val a = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        a.snapTo(peak)
        a.animateTo(0f, spring(dampingRatio = 0.38f, stiffness = 80f))
    }
    return a.asState()
}

/** Бегущая фаза волны 0..2π. */
@Composable
internal fun rememberThreadPhase(periodMillis: Int = 1800): State<Float> {
    val t = rememberInfiniteTransition(label = "threadPhase")
    return t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
        label = "phase",
    )
}
