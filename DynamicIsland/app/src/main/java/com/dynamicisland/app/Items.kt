package com.dynamicisland.app

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.SystemClock

class Act(val title: CharSequence, val run: () -> Unit)

sealed class Item { abstract val id: String }

class NotifItem(
    override val id: String,
    val app: CharSequence,
    val appIcon: Drawable?,
    val avatar: Drawable?,
    val title: CharSequence,
    val text: CharSequence,
    val open: (() -> Unit)?,
) : Item()

/** kind: 0 — зарядка подключена, 1 — низкий заряд */
class ChargeItem(val level: Int, val kind: Int) : Item() {
    override val id = "charge"
}

class CallItem(
    override val id: String,
    val caller: CharSequence,
    val appIcon: Drawable?,
    val avatar: Drawable?,
    val ringing: Boolean,
    /** wall-clock время начала разговора */
    val startMs: Long,
    val answer: Act?,
    val decline: Act?,
    val open: (() -> Unit)?,
) : Item() {
    fun chronoBase(): Long = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startMs)
}

class MediaItem : Item() {
    override val id = "media"
    var app: CharSequence = ""
    var title: CharSequence = ""
    var artist: CharSequence = ""
    var art: Bitmap? = null
    var color: Int = 0xFFFF375F.toInt()
    var playing = false
    var posMs = 0L
    var durMs = 0L
    var stamp = SystemClock.elapsedRealtime()
    var speed = 1f
    var onPrev: (() -> Unit)? = null
    var onToggle: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var open: (() -> Unit)? = null

    fun position(): Long {
        val p = if (playing) posMs + ((SystemClock.elapsedRealtime() - stamp) * speed).toLong() else posMs
        return if (durMs > 0) p.coerceIn(0, durMs) else p.coerceAtLeast(0)
    }
}
