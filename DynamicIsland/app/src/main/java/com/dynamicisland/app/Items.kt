package com.dynamicisland.app

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.SystemClock

class Act(val title: CharSequence, val run: () -> Unit)

sealed class Item {
    abstract val id: String
    /** срочное: остаётся раскрытым, пока не обработано (входящий звонок, будильник) */
    open val urgent: Boolean get() = false
}

class NotifItem(
    override val id: String,
    val app: CharSequence,
    val appIcon: Drawable?,
    val avatar: Drawable?,
    val title: CharSequence,
    val text: CharSequence,
    val open: (() -> Unit)?,
) : Item()

/** kind: 0 — зарядка подключена, 1 — низкий заряд, 2 — заряжено полностью */
class ChargeItem(val level: Int, val kind: Int) : Item() {
    override val id = "charge"
}

/** Маленькая «пилюля» без раскрытия: режим звука, наушники, разблокировка. */
class MiniItem(
    override val id: String,
    val kind: Int, // 0 звук, 1 наушники, 2 разблокировка
    val text: String,
    val color: Int,
    val icon: Int,
    val slashed: Boolean = false,
) : Item()

class CallItem(
    override val id: String,
    val caller: CharSequence,
    val appIcon: Drawable?,
    val avatar: Drawable?,
    val ringing: Boolean,
    val startMs: Long,
    val answer: Act?,
    val decline: Act?,
    val open: (() -> Unit)?,
) : Item() {
    override val urgent get() = ringing
    fun chronoBase(): Long = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startMs)
}

class AlertItem(
    override val id: String,
    val title: CharSequence,
    val text: CharSequence,
    val appIcon: Drawable?,
    val actions: List<Act>,
    val open: (() -> Unit)?,
) : Item() {
    override val urgent get() = true
}

/** Таймер / секундомер и другие «живые» уведомления с хронометром. */
class LiveItem(
    override val id: String,
    val app: CharSequence,
    val title: CharSequence,
    val text: CharSequence,
    val appIcon: Drawable?,
    /** база в SystemClock.elapsedRealtime() */
    val base: Long,
    val countDown: Boolean,
    val actions: List<Act>,
    val open: (() -> Unit)?,
) : Item()

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
    var onSeek: ((Long) -> Unit)? = null
    var open: (() -> Unit)? = null

    fun position(): Long {
        val p = if (playing) posMs + ((SystemClock.elapsedRealtime() - stamp) * speed).toLong() else posMs
        return if (durMs > 0) p.coerceIn(0, durMs) else p.coerceAtLeast(0)
    }
}
