package com.dynamicisland.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/** Рисует острова так же, как IslandView, на «обоях» — чтобы глазами проверить вёрстку. */
class IslandShots {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 700))

    private class Stage(ctx: Context, val spec: Spec, val dark: Boolean, val cam: RectF) : FrameLayout(ctx) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()
        init {
            setWillNotDraw(false)
            spec.build?.invoke()?.let { v ->
                val w = Math.ceil(spec.rect.width().toDouble()).toInt(); val h = Math.ceil(spec.rect.height().toDouble()).toInt()
                addView(v, LayoutParams(w, h))
                v.translationX = spec.rect.left; v.translationY = spec.rect.top
            }
        }
        override fun dispatchDraw(c: Canvas) {
            if (dark) c.drawColor(Color.BLACK) else {
                paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), 0xFF3A6DD8.toInt(), 0xFFE8A0C8.toInt(), Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint); paint.shader = null
            }
            val r = spec.rect
            path.smoothRoundRect(r.left, r.top, r.right, r.bottom, Math.min(spec.radius, Math.min(r.width(), r.height()) / 2))
            paint.color = Color.BLACK; c.drawPath(path, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f; paint.color = 0x2EFFFFFF; c.drawPath(path, paint); paint.style = Paint.Style.FILL
            spec.bubble?.let { b ->
                paint.color = Color.BLACK; c.drawRoundRect(b, b.width() / 2, b.height() / 2, paint)
                spec.bubbleIcon?.let { ic -> val i = if (spec.bubbleFill) 0f else b.width() * 0.26f
                    ic.setBounds((b.left + i).toInt(), (b.top + i).toInt(), (b.right - i).toInt(), (b.bottom - i).toInt()); ic.draw(c) }
            }
            c.save(); c.clipPath(path); super.dispatchDraw(c); c.restore()
            paint.color = 0xFF050505.toInt(); c.drawRoundRect(cam, cam.height() / 2, cam.height() / 2, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; paint.color = 0xFF30D158.toInt(); c.drawRoundRect(cam, cam.height() / 2, cam.height() / 2, paint); paint.style = Paint.Style.FILL
        }
    }

    private fun geo(ctx: Context, cx: Float): Geometry {
        val d = ctx.resources.displayMetrics.density
        val sw = ctx.resources.displayMetrics.widthPixels.toFloat()
        val cam = RectF(cx * sw - 11 * d, 14 * d, cx * sw + 11 * d, 36 * d)
        return Geometry(sw, cam, d, false, 2 * d, true)
    }

    private fun shot(name: String, cx: Float, dark: Boolean = false, f: (SpecBuilder, Geometry, Context) -> Spec) {
        val ctx = paparazzi.context
        val d = ctx.resources.displayMetrics.density
        val g = geo(ctx, cx)
        val sb = SpecBuilder(ctx, d)
        val spec = f(sb, g, ctx)
        val stage = Stage(ctx, spec, dark, g.cam)
        paparazzi.snapshot(stage, name)
    }

    private fun notif(ctx: Context, text: String) = NotifItem("n", "Сообщения", null,
        letterAvatar(ctx, "А", 0xFF0A84FF.toInt()), "Алекс", text, null)

    private fun media(ctx: Context) = MediaItem().apply {
        app = "Музыка"; title = "Blinding Lights"; artist = "The Weeknd"
        art = coverArt(ctx, 0xFFFF2D55.toInt(), 0xFF3A0CA3.toInt()); playing = false; durMs = 200_000; posMs = 82_000
    }

    @Test fun notifCenter() = shot("notif_center", 0.5f) { sb, g, c -> sb.expanded(g, notif(c, "Ты уже видел новый Dynamic Island? Выглядит огонь 🔥 Давай попробуем на твоём телефоне.")) }
    @Test fun notifShort() = shot("notif_short_center", 0.5f) { sb, g, c -> sb.expanded(g, notif(c, "Привет!")) }
    @Test fun notifLeft() = shot("notif_left", 0.12f) { sb, g, c -> sb.expanded(g, notif(c, "Ты уже видел новый Dynamic Island? Выглядит огонь 🔥")) }
    @Test fun mediaExp() = shot("media_expanded", 0.5f) { sb, g, c -> sb.expanded(g, media(c)) }
    @Test fun mediaCompact() = shot("media_compact", 0.5f) { sb, g, c -> sb.compact(g, media(c), null) }
    @Test fun mediaCompactLeft() = shot("media_compact_left", 0.12f) { sb, g, c -> sb.compact(g, media(c), null) }
    @Test fun mediaBubble() = shot("media_bubble", 0.5f) { sb, g, c ->
        val live = LiveItem("t", "Таймер", "Таймер", "Чай", null, android.os.SystemClock.elapsedRealtime() + 300000, true, emptyList(), null)
        sb.compact(g, media(c), live) }
    @Test fun callRing() = shot("call_ringing", 0.5f) { sb, g, c ->
        sb.expanded(g, CallItem("c", "Мама", null, letterAvatar(c, "М", 0xFFFF9F0A.toInt()), true, 0, Act("Принять") {}, Act("Сбросить") {}, null)) }
    @Test fun callCompact() = shot("call_compact", 0.5f) { sb, g, c ->
        sb.compact(g, CallItem("c", "Мама", null, null, false, System.currentTimeMillis(), null, Act("x") {}, null), null) }
    @Test fun charge() = shot("charge", 0.5f) { sb, g, _ -> sb.transient(g, ChargeItem(78, 0)) }
    @Test fun ringer() = shot("ringer", 0.5f) { sb, g, _ -> sb.transient(g, MiniItem("r", 0, "Без звука", 0xFFFF9F0A.toInt(), R.drawable.ic_bell, true)) }
    @Test fun timerExp() = shot("timer_expanded", 0.5f) { sb, g, c ->
        sb.expanded(g, LiveItem("t", "Таймер", "Таймер", "Чай 🍵", null, android.os.SystemClock.elapsedRealtime() + 300000, true, listOf(Act("Стоп") {}, Act("Пауза") {}), null)) }
    @Test fun alarm() = shot("alarm", 0.5f) { sb, g, _ ->
        sb.expanded(g, AlertItem("a", "07:30", "Подъём", null, listOf(Act("Отложить") {}, Act("Стоп") {}), null)) }
    @Test fun darkBg() = shot("notif_on_black", 0.5f, true) { sb, g, c -> sb.expanded(g, notif(c, "Остров на чёрном фоне")) }
}
