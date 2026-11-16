package com.dynamicisland.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.math.sin

val WHITE = Color.WHITE
val SEC = 0xB3FFFFFF.toInt()
val GREEN = 0xFF30D158.toInt()
val RED = 0xFFFF453A.toInt()

fun Context.dp(v: Float): Float = v * resources.displayMetrics.density
fun Context.dpi(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun tv(ctx: Context, s: CharSequence, sp: Float, color: Int = WHITE, bold: Boolean = false, lines: Int = 1) =
    TextView(ctx).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(color)
        typeface = Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), if (bold) 600 else 400, false)
        maxLines = lines
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
    }

fun icon(ctx: Context, res: Int, tint: Int, sizeDp: Float) = ImageView(ctx).apply {
    setImageResource(res)
    setColorFilter(tint)
    layoutParams = LinearLayout.LayoutParams(ctx.dpi(sizeDp), ctx.dpi(sizeDp))
}

fun roundImg(ctx: Context, drawable: Drawable?, sizeDp: Float, radiusDp: Float, fallbackColor: Int = 0xFF3A3A3C.toInt(), fallbackIcon: Int = R.drawable.ic_bell) =
    ImageView(ctx).apply {
        val px = ctx.dpi(sizeDp)
        layoutParams = LinearLayout.LayoutParams(px, px)
        scaleType = ImageView.ScaleType.CENTER_CROP
        if (drawable != null) setImageDrawable(drawable) else {
            background = GradientDrawable().apply { setColor(fallbackColor); cornerRadius = ctx.dp(radiusDp) }
            setImageResource(fallbackIcon); setColorFilter(WHITE)
            val p = px / 4; setPadding(p, p, p, p)
        }
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, ctx.dp(radiusDp)) }
        }
    }

fun row(ctx: Context, vararg views: View) = LinearLayout(ctx).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    views.forEach { addView(it) }
}

fun lp(ctx: Context, w: Float, h: Float, gravity: Int = Gravity.TOP or Gravity.START) =
    FrameLayout.LayoutParams(if (w < 0) WRAP else ctx.dpi(w), if (h < 0) WRAP else ctx.dpi(h), gravity)

/** Круглая аватарка-буква для демо-данных. */
fun letterAvatar(ctx: Context, ch: String, color: Int): Drawable {
    val s = 128
    val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.shader = LinearGradient(0f, 0f, s.toFloat(), s.toFloat(), color, Color.argb(255, 40, 40, 90), Shader.TileMode.CLAMP)
    c.drawRect(0f, 0f, s.toFloat(), s.toFloat(), p)
    p.shader = null; p.color = WHITE; p.textSize = 64f; p.textAlign = Paint.Align.CENTER
    p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
    c.drawText(ch, s / 2f, s / 2f + 22f, p)
    return BitmapDrawable(ctx.resources, b)
}

fun coverArt(ctx: Context, c1: Int, c2: Int): Bitmap {
    val s = 256
    val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.shader = LinearGradient(0f, 0f, s.toFloat(), s.toFloat(), c1, c2, Shader.TileMode.CLAMP)
    c.drawRect(0f, 0f, s.toFloat(), s.toFloat(), p)
    p.shader = RadialGradient(s * .7f, s * .3f, s * .5f, 0x66FFFFFF, 0, Shader.TileMode.CLAMP)
    c.drawRect(0f, 0f, s.toFloat(), s.toFloat(), p)
    return b
}

fun vividColor(b: Bitmap?): Int {
    if (b == null) return 0xFFFF375F.toInt()
    val px = Bitmap.createScaledBitmap(b, 1, 1, true).getPixel(0, 0)
    val hsv = FloatArray(3)
    Color.colorToHSV(px, hsv)
    hsv[1] = max(hsv[1], 0.55f); hsv[2] = 1f
    return Color.HSVToColor(hsv)
}

fun fmtTime(ms: Long): String {
    val s = (ms / 1000).toInt().coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// ---------------------------------------------------------------- виджеты

class EqualizerView(ctx: Context, var color: Int, private val bars: Int = 4) : View(ctx) {
    var active = true
        set(v) { field = v; last = 0; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    private var last = 0L

    override fun onDraw(c: Canvas) {
        val now = SystemClock.uptimeMillis()
        if (last != 0L && active) t += (now - last) / 1000f
        last = now
        p.color = color
        val w = width.toFloat(); val h = height.toFloat()
        val bw = w / (bars * 2 - 1)
        for (i in 0 until bars) {
            val amp = if (active) 0.25f + 0.75f * (0.5f + 0.5f * sin(t * (5.5f + i * 1.7f) + i * 1.3f)) else 0.2f
            val bh = max(bw, h * amp)
            val x = i * bw * 2
            c.drawRoundRect(x, (h - bh) / 2, x + bw, (h + bh) / 2, bw / 2, bw / 2, p)
        }
        if (active) postInvalidateOnAnimation()
    }
}

class ProgressBarView(ctx: Context) : View(ctx) {
    var frac = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val h = height.toFloat(); val w = width.toFloat()
        p.color = 0x40FFFFFF; c.drawRoundRect(0f, 0f, w, h, h / 2, h / 2, p)
        p.color = 0xE6FFFFFF.toInt(); c.drawRoundRect(0f, 0f, max(h, w * frac), h, h / 2, h / 2, p)
    }
}

class BatteryView(ctx: Context, private val level: Int, private val color: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shown = 0f
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1100; startDelay = 250
        addUpdateListener { shown = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cap = h * 0.14f
        val bw = w - cap - 2f
        val sw = h * 0.07f
        p.style = Paint.Style.STROKE; p.strokeWidth = sw; p.color = 0x80FFFFFF.toInt()
        c.drawRoundRect(sw / 2, sw / 2, bw - sw / 2, h - sw / 2, h * 0.3f, h * 0.3f, p)
        p.style = Paint.Style.FILL; p.color = 0x80FFFFFF.toInt()
        c.drawRoundRect(bw + 2f, h * 0.34f, bw + 2f + cap, h * 0.66f, cap / 2, cap / 2, p)
        val inset = sw * 2
        val full = bw - inset * 2
        val fw = max(h * 0.3f, full * level / 100f * shown)
        p.color = color
        c.drawRoundRect(inset, inset, inset + fw, h - inset, h * 0.2f, h * 0.2f, p)
    }
}

interface Bindable { fun bind(item: Item) }

// ---------------------------------------------------------------- раскладки

fun Exp.populate(f: FrameLayout, lead: View?, trail: View?, body: View?) {
    val ctx = f.context
    val rowH = 26f
    lead?.let {
        f.addView(it, lp(ctx, -1f, rowH, Gravity.TOP or Gravity.START).apply {
            leftMargin = leadStart.toInt(); topMargin = (rowCy - ctx.dp(rowH) / 2).toInt()
        })
    }
    trail?.let {
        f.addView(it, lp(ctx, -1f, rowH, Gravity.TOP or Gravity.END).apply {
            rightMargin = trailEnd.toInt(); topMargin = (rowCy - ctx.dp(rowH) / 2).toInt()
        })
    }
    body?.let {
        f.addView(it, FrameLayout.LayoutParams((rect.width() - ctx.dp(32f)).toInt(), bodyH.toInt(), Gravity.TOP or Gravity.START).apply {
            leftMargin = ctx.dpi(16f); topMargin = bodyTop.toInt()
        })
    }
}

fun Compact.populate(f: FrameLayout, lead: View?, trail: View?) {
    val ctx = f.context
    val h = ctx.dp(24f)
    lead?.let {
        f.addView(it, FrameLayout.LayoutParams(slotL.toInt(), h.toInt(), Gravity.TOP or Gravity.START).apply {
            leftMargin = leadX.toInt(); topMargin = (rowCy - h / 2).toInt()
        })
    }
    trail?.let {
        f.addView(it, FrameLayout.LayoutParams(slotT.toInt(), h.toInt(), Gravity.TOP or Gravity.START).apply {
            leftMargin = trailX.toInt(); topMargin = (rowCy - h / 2).toInt()
        })
    }
}

private fun pillButton(ctx: Context, label: String, bg: Int, ic: Int, onClick: () -> Unit) =
    row(ctx, icon(ctx, ic, WHITE, 20f), tv(ctx, label, 14f, WHITE, true).apply { setPadding(ctx.dpi(6f), 0, 0, 0) }).apply {
        gravity = Gravity.CENTER
        background = GradientDrawable().apply { setColor(bg); cornerRadius = ctx.dp(22f) }
        isClickable = true
        setOnClickListener { onClick() }
    }

// ---------------------------------------------------------------- содержимое

class NotifView(ctx: Context, e: Exp, n: NotifItem) : FrameLayout(ctx), Bindable {
    init {
        val lead = row(ctx, roundImg(ctx, n.appIcon, 18f, 5f), tv(ctx, n.app, 12f, SEC, true).apply {
            setPadding(ctx.dpi(6f), 0, 0, 0); maxWidth = e.leadMax.toInt()
        })
        val trail = row(ctx, tv(ctx, "сейчас", 12f, SEC))
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dpi(12f), 0, 0, 0)
            addView(tv(ctx, n.title, 15f, WHITE, true))
            if (n.text.isNotEmpty()) addView(tv(ctx, n.text, 14f, SEC, false, 2).apply { setPadding(0, ctx.dpi(2f), 0, 0) })
        }
        val body = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(roundImg(ctx, n.avatar ?: n.appIcon, 44f, 22f))
            addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        e.populate(this, lead, trail, body)
    }
    override fun bind(item: Item) {}
}

class ChargeView(ctx: Context, e: Exp, c: ChargeItem) : FrameLayout(ctx), Bindable {
    init {
        val color = if (c.kind == 1) RED else GREEN
        val lead = row(ctx, icon(ctx, R.drawable.ic_bolt, color, 18f),
            tv(ctx, if (c.kind == 1) "Низкий заряд" else "Зарядка", 12f, SEC, true).apply { setPadding(ctx.dpi(4f), 0, 0, 0) })
        val trail = row(ctx, tv(ctx, "${c.level}%", 13f, color, true))
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dpi(14f), 0, 0, 0)
            addView(tv(ctx, "${c.level}%", 24f, WHITE, true))
            addView(tv(ctx, if (c.kind == 1) "Подключите зарядку" else "Заряжается", 13f, SEC))
        }
        val body = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(BatteryView(ctx, c.level, color), LinearLayout.LayoutParams(ctx.dpi(64f), ctx.dpi(30f)))
            addView(col)
        }
        e.populate(this, lead, trail, body)
    }
    override fun bind(item: Item) {}
}

class MediaCompactView(ctx: Context, c: Compact, private var m: MediaItem) : FrameLayout(ctx), Bindable {
    private val art = roundImg(ctx, null, 24f, 7f)
    private val eq = EqualizerView(ctx, m.color)
    private val holder = FrameLayout(ctx)
    init {
        c.populate(this, holder.also { it.addView(art) }, eq)
        bind(m)
    }
    override fun bind(item: Item) {
        m = item as MediaItem
        holder.removeAllViews()
        holder.addView(roundImg(context, m.art?.let { BitmapDrawable(resources, it) }, 24f, 7f, m.color, R.drawable.ic_music))
        eq.color = m.color; eq.active = m.playing
    }
}

class MediaExpandedView(ctx: Context, e: Exp, private var m: MediaItem) : FrameLayout(ctx), Bindable {
    private val artHolder = FrameLayout(ctx)
    private val title = tv(ctx, "", 16f, WHITE, true)
    private val artist = tv(ctx, "", 14f, SEC)
    private val eq = EqualizerView(ctx, m.color)
    private val prog = ProgressBarView(ctx)
    private val tCur = tv(ctx, "0:00", 11f, SEC)
    private val tEnd = tv(ctx, "", 11f, SEC)
    private val play = ImageView(ctx)
    private val tick = object : Runnable {
        override fun run() {
            updateProgress()
            if (isAttachedToWindow) postDelayed(this, 400)
        }
    }

    init {
        val lead = row(ctx, tv(ctx, m.app, 12f, SEC, true).apply { maxWidth = e.leadMax.toInt() })
        val trail = row(ctx, eq.apply { layoutParams = LinearLayout.LayoutParams(ctx.dpi(22f), ctx.dpi(18f)) })

        val info = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dpi(12f), 0, 0, 0)
            addView(title); addView(artist.apply { setPadding(0, ctx.dpi(2f), 0, 0) })
        }
        val top = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(artHolder, LinearLayout.LayoutParams(ctx.dpi(50f), ctx.dpi(50f)))
            addView(info, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        val times = LinearLayout(ctx).apply {
            addView(tCur, LinearLayout.LayoutParams(0, WRAP, 1f)); addView(tEnd)
        }
        fun btn(res: Int, sz: Float, onClick: () -> Unit) = ImageView(ctx).apply {
            setImageResource(res); setColorFilter(WHITE); scaleType = ImageView.ScaleType.FIT_CENTER
            val pd = ctx.dpi(6f); setPadding(pd, pd, pd, pd)
            isClickable = true; setOnClickListener { onClick() }
        }
        val prev = btn(R.drawable.ic_prev, 30f) { m.onPrev?.invoke() }
        val next = btn(R.drawable.ic_next, 30f) { m.onNext?.invoke() }
        play.apply {
            setColorFilter(WHITE); scaleType = ImageView.ScaleType.FIT_CENTER
            val pd = ctx.dpi(4f); setPadding(pd, pd, pd, pd)
            isClickable = true
            setOnClickListener { m.onToggle?.invoke(); m.playing = !m.playing; m.posMs = m.position(); m.stamp = SystemClock.elapsedRealtime(); bind(m) }
        }
        val controls = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER
            addView(prev, LinearLayout.LayoutParams(ctx.dpi(44f), ctx.dpi(44f)))
            addView(play, LinearLayout.LayoutParams(ctx.dpi(52f), ctx.dpi(44f)).apply { setMargins(ctx.dpi(22f), 0, ctx.dpi(22f), 0) })
            addView(next, LinearLayout.LayoutParams(ctx.dpi(44f), ctx.dpi(44f)))
        }
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(top, LinearLayout.LayoutParams(-1, ctx.dpi(50f)))
            addView(prog, LinearLayout.LayoutParams(-1, ctx.dpi(4f)).apply { topMargin = ctx.dpi(12f) })
            addView(times, LinearLayout.LayoutParams(-1, WRAP).apply { topMargin = ctx.dpi(5f) })
            addView(controls, LinearLayout.LayoutParams(-1, ctx.dpi(44f)).apply { topMargin = ctx.dpi(2f) })
        }
        e.populate(this, lead, trail, body)
        bind(m)
    }

    private fun updateProgress() {
        val d = m.durMs
        tCur.text = fmtTime(m.position())
        tEnd.text = if (d > 0) "-" + fmtTime(d - m.position()) else ""
        prog.frac = if (d > 0) m.position().toFloat() / d else 0f
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); post(tick) }

    override fun bind(item: Item) {
        m = item as MediaItem
        title.text = m.title.ifEmpty { "Без названия" }; artist.text = m.artist
        artHolder.removeAllViews()
        artHolder.addView(roundImg(context, m.art?.let { BitmapDrawable(resources, it) }, 50f, 11f, m.color, R.drawable.ic_music))
        eq.color = m.color; eq.active = m.playing
        play.setImageResource(if (m.playing) R.drawable.ic_pause else R.drawable.ic_play)
        updateProgress()
    }
}

class CallCompactView(ctx: Context, c: Compact, private var call: CallItem) : FrameLayout(ctx), Bindable {
    init {
        val lead = icon(ctx, R.drawable.ic_call, GREEN, 18f).apply { layoutParams = null }
        val chrono = Chronometer(ctx).apply {
            setTextColor(GREEN); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), 600, false)
            includeFontPadding = false; gravity = Gravity.CENTER_VERTICAL or Gravity.END
            base = call.chronoBase(); start()
        }
        val wrap = FrameLayout(ctx).apply { addView(lead, FrameLayout.LayoutParams(ctx.dpi(18f), ctx.dpi(18f), Gravity.CENTER)) }
        c.populate(this, wrap, chrono)
    }
    override fun bind(item: Item) { call = item as CallItem }
}

class CallExpandedView(ctx: Context, e: Exp, private var call: CallItem) : FrameLayout(ctx), Bindable {
    init {
        val lead = row(ctx, icon(ctx, R.drawable.ic_call, GREEN, 16f),
            tv(ctx, if (call.ringing) "Входящий звонок" else "Разговор", 12f, SEC, true).apply {
                setPadding(ctx.dpi(5f), 0, 0, 0); maxWidth = e.leadMax.toInt()
            })
        val trail = row(ctx, EqualizerView(ctx, GREEN).apply { layoutParams = LinearLayout.LayoutParams(ctx.dpi(22f), ctx.dpi(18f)) })
        val sub: View = if (call.ringing) tv(ctx, "Мобильный", 14f, SEC) else Chronometer(ctx).apply {
            setTextColor(SEC); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); includeFontPadding = false
            base = call.chronoBase(); start()
        }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dpi(12f), 0, 0, 0)
            addView(tv(ctx, call.caller, 19f, WHITE, true)); addView(sub.apply { setPadding(0, ctx.dpi(2f), 0, 0) })
        }
        val head = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(roundImg(ctx, call.avatar ?: call.appIcon, 48f, 24f, 0xFF4A4A52.toInt(), R.drawable.ic_call))
            addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(head, LinearLayout.LayoutParams(-1, ctx.dpi(48f)))
            val btns = LinearLayout(ctx)
            call.decline?.let { d ->
                btns.addView(pillButton(ctx, if (call.ringing) "Сбросить" else "Завершить", RED, R.drawable.ic_call_end) { d.run() },
                    LinearLayout.LayoutParams(0, ctx.dpi(44f), 1f))
            }
            if (call.ringing) call.answer?.let { a ->
                btns.addView(pillButton(ctx, "Принять", GREEN, R.drawable.ic_call) { a.run() },
                    LinearLayout.LayoutParams(0, ctx.dpi(44f), 1f).apply { leftMargin = ctx.dpi(10f) })
            }
            addView(btns, LinearLayout.LayoutParams(-1, ctx.dpi(44f)).apply { topMargin = ctx.dpi(12f) })
        }
        e.populate(this, lead, trail, body)
    }
    override fun bind(item: Item) { call = item as CallItem }
}
