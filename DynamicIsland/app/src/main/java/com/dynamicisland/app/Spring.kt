package com.dynamicisland.app

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Пружина как в SwiftUI: response (период, с) и dampingFraction (zeta). */
class Spring(var v: Float = 0f) {
    var target = v
    var vel = 0f

    fun snap(x: Float) { v = x; target = x; vel = 0f }

    fun step(dt: Float, response: Float, zeta: Float) {
        val k = (2.0 * PI / response).pow(2).toFloat()
        val c = 2f * zeta * sqrt(k)
        var left = dt
        while (left > 0f) {
            val h = min(left, 1f / 240f)
            val a = -k * (v - target) - c * vel
            vel += a * h
            v += vel * h
            left -= h
        }
    }

    fun settled() = abs(v - target) < 0.3f && abs(vel) < 8f
}
