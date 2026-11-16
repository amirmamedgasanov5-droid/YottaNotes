package design.noodle.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Язык движения Noodle: всё построено на пружинах, как в iOS.
 * smooth — без отскока, snappy — быстрый и упругий, bouncy — игривый, press — реакция на касание.
 */
object NoodleMotion {
    fun <T> smooth(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 380f)
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.82f, stiffness = 520f)
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.58f, stiffness = 320f)
    fun <T> gentle(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 140f)
    fun <T> press(): SpringSpec<T> = spring(dampingRatio = 0.72f, stiffness = 900f)

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Decelerate = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    const val Quick = 160
    const val Medium = 320
    const val Slow = 560
}
