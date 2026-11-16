package com.dynamicisland.app

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

enum class Side { LEFT, CENTER, RIGHT }

class Compact(
    val rect: RectF, val radius: Float,
    val leadX: Float, val trailX: Float, val rowCy: Float,
    val slotL: Float, val slotT: Float,
)

class Exp(
    val rect: RectF, val radius: Float,
    val leadStart: Float, val trailEnd: Float, val rowCy: Float,
    val bodyTop: Float, val bodyH: Float, val leadMax: Float,
)

/**
 * Вся математика расположения. [cam] — прямоугольник камеры в координатах экрана.
 * [attached] — вырез касается верхнего края (классическая «чёлка»): тогда остров «приклеен» к краю.
 */
class Geometry(
    val sw: Float,
    val cam: RectF,
    val d: Float,
    val attached: Boolean,
    val pad: Float,
    val real: Boolean,
) {
    val margin = 10 * d
    val cx = cam.centerX()
    val cy = cam.centerY()
    val side = when {
        cx < sw * 0.36f -> Side.LEFT
        cx > sw * 0.64f -> Side.RIGHT
        else -> Side.CENTER
    }

    private val camL = cam.left - pad
    private val camR = cam.right + pad
    private val pillH = max(cam.height() + 2 * pad, 34 * d)
    val pillTop = if (attached) -44 * d else max(2 * d, cy - pillH / 2)
    val pillBottom = if (attached) max(cam.bottom + pad, 34 * d) else pillTop + pillH

    fun camRect() = RectF(cam)
    fun camRadius() = min(cam.width(), cam.height()) / 2

    fun idle() = RectF(camL, if (attached) -44 * d else cam.top - pad, camR, cam.bottom + pad)

    fun pillRadius(r: RectF) =
        if (attached) min((r.bottom - r.top) / 2, 18 * d) else (r.bottom - r.top) / 2

    fun compact(slotL: Float, slotT: Float): Compact {
        val gap = 5 * d
        val inn = 5 * d
        val left: Float
        val right: Float
        val leadX: Float
        val trailX: Float
        when (side) {
            Side.CENTER -> {
                left = camL - gap - slotL - inn
                right = camR + gap + slotT + inn
                leadX = inn
                trailX = (right - left) - inn - slotT
            }
            Side.LEFT -> {
                left = camL
                leadX = (camR - camL) + gap
                trailX = leadX + slotL + inn
                right = left + trailX + slotT + inn
            }
            Side.RIGHT -> {
                right = camR
                leadX = inn
                trailX = leadX + slotL + inn
                left = right - (trailX + slotT + gap + (camR - camL))
            }
        }
        val r = RectF(left, pillTop, right, pillBottom)
        return Compact(r, pillRadius(r), leadX, trailX, cy - pillTop, slotL, slotT)
    }

    fun expanded(bodyH: Float, maxW: Float): Exp {
        val w = min(sw - 2 * margin, maxW)
        val left = (cx - w / 2).coerceIn(margin, sw - margin - w)
        val bottom = pillBottom + 4 * d + bodyH + 16 * d
        val r = RectF(left, pillTop, left + w, bottom)
        val gap = 8 * d
        val side18 = 18 * d
        val leadStart: Float
        val trailEnd: Float
        val leadMax: Float
        when (side) {
            Side.CENTER -> {
                leadStart = side18; trailEnd = side18
                leadMax = (camL - gap) - (left + leadStart)
            }
            Side.LEFT -> {
                leadStart = (camR + gap) - left; trailEnd = side18
                leadMax = w - leadStart - trailEnd - 64 * d
            }
            Side.RIGHT -> {
                leadStart = side18; trailEnd = r.right - (camL - gap)
                leadMax = (camL - gap) - (left + leadStart) - 56 * d
            }
        }
        return Exp(
            r, min((bottom - pillTop) / 2, 42 * d), leadStart, trailEnd,
            cy - pillTop, pillBottom - pillTop + 4 * d, bodyH, max(leadMax, 60 * d),
        )
    }
}
