package com.dynamicisland.app

import android.graphics.Path

/** Прямоугольник с «непрерывными» углами в стиле iOS (суперэллипс), а у капсулы — идеальная капсула. */
fun Path.smoothRoundRect(l: Float, t: Float, r: Float, b: Float, radius: Float) {
    rewind()
    val w = r - l; val h = b - t
    val maxP = minOf(w, h) / 2f
    val rad = radius.coerceIn(0f, maxP)
    if (rad < 0.5f) { addRect(l, t, r, b, Path.Direction.CW); return }
    val p = minOf(rad * 1.24f, maxP)
    val k = 0.5523f + (0.63f - 0.5523f) * ((p / rad - 1f) / 0.24f).coerceIn(0f, 1f)
    val q = (1f - k) * p
    moveTo(l + p, t)
    lineTo(r - p, t)
    cubicTo(r - q, t, r, t + q, r, t + p)
    lineTo(r, b - p)
    cubicTo(r, b - q, r - q, b, r - p, b)
    lineTo(l + p, b)
    cubicTo(l + q, b, l, b - q, l, b - p)
    lineTo(l, t + p)
    cubicTo(l, t + q, l + q, t, l + p, t)
    close()
}
