package com.dynamicisland.app

import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.max

/** Строит [Spec] (положение формы + содержимое) для каждого состояния острова. Не зависит от окон. */
class SpecBuilder(private val ctx: Context, private val d: Float) {
    var geoVer = 0
    private val expCache = HashMap<String, Pair<Int, Exp>>()

    fun hidden(g: Geometry) = Spec("hidden", g.camRect(), g.camRadius(), false, g.camRect(), false)

    fun idle(g: Geometry): Spec {
        val r = g.idle()
        return Spec("idle", r, g.pillRadius(r), true, g.camRect(), false)
    }

    private fun textW(s: String, sp: Float): Float {
        val t = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            typeface = Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), 600, false)
            text = s
        }
        t.measure(0, 0)
        return t.measuredWidth.toFloat()
    }

    private fun glyph(res: Int, color: Int): Drawable = ctx.getDrawable(res)!!.mutate().apply { setTint(color) }

    private fun bubbleOf(g: Geometry, c: Compact, sec: Item?): Triple<RectF, Drawable?, Boolean>? {
        sec ?: return null
        val dia = if (g.attached) 34 * d else c.rect.height()
        val gap = 8 * d
        val rightOk = g.sw - g.margin - c.rect.right >= dia + gap
        val leftOk = c.rect.left - g.margin >= dia + gap
        if (!rightOk && !leftOk) return null
        val toLeft = (g.side == Side.RIGHT && leftOk) || (!rightOk && leftOk)
        val x = if (toLeft) c.rect.left - gap - dia else c.rect.right + gap
        val r = RectF(x, g.cy - dia / 2, x + dia, g.cy + dia / 2)
        return when (sec) {
            is MediaItem -> Triple(r, sec.art?.let { BitmapDrawable(ctx.resources, it) } ?: glyph(R.drawable.ic_music, sec.color), sec.art != null)
            is CallItem -> Triple(r, glyph(R.drawable.ic_call, GREEN), false)
            is LiveItem -> Triple(r, sec.appIcon?.constantState?.newDrawable()?.mutate() ?: glyph(R.drawable.ic_timer, 0xFFFF9F0A.toInt()), sec.appIcon != null)
            is AlertItem -> Triple(r, glyph(R.drawable.ic_bell, 0xFFFF9F0A.toInt()), false)
            else -> null
        }
    }

    fun compact(g: Geometry, p: Item, sec: Item?): Spec {
        val c = g.compact(24 * d, if (p is CallItem || p is LiveItem) 46 * d else 24 * d)
        val b = bubbleOf(g, c, sec)
        val bk = sec?.id
        return when (p) {
            is MediaItem -> Spec("media:c", c.rect, c.radius, true, g.camRect(), true,
                { MediaCompactView(ctx, c, p) }, { (it as Bindable).bind(p) }, b?.first, b?.second, b?.third ?: false, bk)
            is CallItem -> Spec("call:c:${p.id}", c.rect, c.radius, true, g.camRect(), true,
                { CallCompactView(ctx, c, p) }, { (it as Bindable).bind(p) }, b?.first, b?.second, b?.third ?: false, bk)
            is LiveItem -> Spec("live:c:${p.id}", c.rect, c.radius, true, g.camRect(), true,
                { LiveCompactView(ctx, c, p) }, null, b?.first, b?.second, b?.third ?: false, bk)
            else -> idle(g)
        }
    }

    fun transient(g: Geometry, p: Item): Spec = when (p) {
        is NotifItem -> expanded(g, p)
        is ChargeItem -> {
            val c = g.compact(34 * d, textW("${p.level}%", 14f) + 6 * d)
            Spec("charge:${p.kind}:${p.level}", c.rect, c.radius, true, g.camRect(), true, { MiniView(ctx, c, p) })
        }
        is MiniItem -> {
            val c = g.compact(26 * d, if (p.text.isEmpty()) 0f else textW(p.text, 14f) + 6 * d)
            Spec("mini:${p.id}:${p.text}", c.rect, c.radius, true, g.camRect(), true, { MiniView(ctx, c, p) })
        }
        else -> expanded(g, p)
    }

    /** Раскрытый вид; высота считается по реальному содержимому, поэтому ничего не обрезается. */
    fun expanded(g: Geometry, p: Item): Spec {
        val maxW = 400 * d
        val make: (Exp) -> FrameLayout
        val key: String
        when (p) {
            is NotifItem -> { key = "n:${p.id}:${(p.title.toString() + p.text).hashCode()}"; make = { NotifView(ctx, it, p) } }
            is MediaItem -> { key = "media:e"; make = { MediaExpandedView(ctx, it, p) } }
            is CallItem -> { key = "call:e:${p.id}:${p.ringing}"; make = { CallExpandedView(ctx, it, p) } }
            is LiveItem -> { key = "live:e:${p.id}"; make = { LiveExpandedView(ctx, it, p) } }
            is AlertItem -> { key = "alert:${p.id}"; make = { AlertView(ctx, it, p) } }
            else -> return idle(g)
        }
        val cached = expCache[key]
        val e: Exp = if (cached != null && cached.first == geoVer) cached.second else {
            val e0 = g.expanded(0f, maxW)
            val bh = max(make(e0).bodyHeight((e0.rect.width() - 2 * SIDE * d).toInt()).toFloat(), 40 * d)
            g.expanded(bh, maxW).also { if (expCache.size > 20) expCache.clear(); expCache[key] = geoVer to it }
        }
        val upd: ((View) -> Unit)? = if (p is MediaItem) { v -> (v as Bindable).bind(p) } else null
        return Spec(key, e.rect, e.radius, true, g.camRect(), true, { make(e) }, upd)
    }
}
