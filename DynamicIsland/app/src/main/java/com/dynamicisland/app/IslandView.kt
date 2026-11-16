package com.dynamicisland.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Описание того, что должен показывать остров. Координаты — экранные. */
class Spec(
    val key: String,
    val rect: RectF,
    val radius: Float,
    val visible: Boolean,
    /** откуда «вырастает» форма, если она была скрыта (прямоугольник камеры) */
    val origin: RectF,
    val touchable: Boolean,
    val build: (() -> View)? = null,
    val update: ((View) -> Unit)? = null,
    /** второй пузырёк рядом с основной капсулой (вторая «живая активность») */
    val bubble: RectF? = null,
    val bubbleIcon: Drawable? = null,
    /** true — иконка заполняет круг (обложка), false — глиф с отступом */
    val bubbleFill: Boolean = false,
    val bubbleKey: String? = null,
)

class IslandView(ctx: Context, private val ctl: IslandController) : FrameLayout(ctx) {
    private val d = ctx.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = d * 1.1f }
    private fun rim(c: Canvas, p: Path, a: Float) {
        if (!ctl.prefs.outline) return
        rimPaint.color = Color.argb((46 * a).toInt(), 255, 255, 255)
        c.drawPath(p, rimPaint)
    }

    // Четыре края + радиус — каждый на своей пружине: форма «желейно» деформируется как в iOS.
    private val sl = Spring(); private val st = Spring(); private val sr = Spring(); private val sb = Spring()
    private val rad = Spring()
    // пузырь второй активности
    private val bl = Spring(); private val bt = Spring(); private val br = Spring(); private val bb = Spring()
    private var bAlpha = 0f
    private var bTarget = 0f
    private var bIcon: Drawable? = null
    private var bFill = false
    private var bKey: String? = null
    // эффект нажатия
    private val press = Spring(1f)

    private var zeta = 0.7f
    private var alphaV = 0f
    private var alphaT = 0f

    private val loc = IntArray(2)
    private val path = Path()
    private val bpath = Path()
    private val rf = RectF()
    private val brf = RectF()
    private var last = 0L
    private var running = false

    private class Layer(val view: View, val rect: RectF)
    private val layers = ArrayList<Layer>()
    private var layer: Layer? = null
    private var layerKey: String? = null

    private var swiped = false
    private var bubbleDown = false
    private val gd = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapUp(e: MotionEvent): Boolean { ctl.onTap(); return true }
        override fun onLongPress(e: MotionEvent) { performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); ctl.onLongPress() }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (e1 == null || swiped) return true
            val mx = e2.x - e1.x; val my = e2.y - e1.y
            if (my < -28 * d && abs(my) > abs(mx)) { swiped = true; ctl.onSwipeUp() }
            else if (abs(mx) > 56 * d && abs(mx) > abs(my) * 1.4f) { swiped = true; ctl.onSwipeSide() }
            return true
        }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (swiped) return true
            if (vy < -900 && abs(vy) > abs(vx)) { swiped = true; ctl.onSwipeUp() }
            else if (abs(vx) > 1100 && abs(vx) > abs(vy) * 1.5f) { swiped = true; ctl.onSwipeSide() }
            return true
        }
    })

    init { setWillNotDraw(false) }

    fun bounds() = RectF(sl.v, st.v, sr.v, sb.v)
    fun bubbleBounds() = if (bAlpha > 0.02f) RectF(bl.v, bt.v, br.v, bb.v) else null

    fun present(s: Spec) {
        if (s.visible && alphaV < 0.03f) snap(s.origin)
        val cur = bounds()
        val grow = s.rect.width() * s.rect.height() > cur.width() * cur.height()
        zeta = if (grow) 0.74f else 0.90f
        sl.target = s.rect.left; st.target = s.rect.top
        sr.target = s.rect.right; sb.target = s.rect.bottom
        rad.target = s.radius
        alphaT = if (s.visible) 1f else 0f
        presentBubble(s)
        updateContent(s)
        start()
    }

    private fun presentBubble(s: Spec) {
        val b = s.bubble
        if (b != null && s.visible) {
            if (bAlpha < 0.03f) {
                // вылетает из края основной капсулы, как «отделяющаяся» капля
                val ex = if (b.centerX() > s.rect.centerX()) s.rect.right - 4 * d else s.rect.left + 4 * d
                bl.snap(ex - 3 * d); br.snap(ex + 3 * d)
                bt.snap(b.centerY() - 3 * d); bb.snap(b.centerY() + 3 * d)
            }
            bl.target = b.left; bt.target = b.top; br.target = b.right; bb.target = b.bottom
            bTarget = 1f
            bIcon = s.bubbleIcon; bFill = s.bubbleFill; bKey = s.bubbleKey
        } else {
            if (bAlpha > 0.02f) {
                val cx = (bl.v + br.v) / 2; val cy = (bt.v + bb.v) / 2
                // втягивается обратно в капсулу
                val tx = if (cx > s.rect.centerX()) s.rect.right - 4 * d else s.rect.left + 4 * d
                bl.target = tx - 3 * d; br.target = tx + 3 * d; bt.target = cy - 3 * d; bb.target = cy + 3 * d
            }
            bTarget = 0f
        }
    }

    private fun snap(r: RectF) {
        sl.snap(r.left); st.snap(r.top); sr.snap(r.right); sb.snap(r.bottom)
        rad.snap(min(r.width(), r.height()) / 2)
    }

    private fun updateContent(s: Spec) {
        val build = s.build
        if (!s.visible || build == null) {
            fadeOut(layer); layer = null; layerKey = null
            return
        }
        val cur = layer
        if (cur != null && layerKey == s.key && cur.rect == s.rect) {
            s.update?.invoke(cur.view)
            return
        }
        fadeOut(cur)
        val v = build()
        val w = ceil(s.rect.width()).toInt()
        val h = ceil(s.rect.height()).toInt()
        addView(v, LayoutParams(w, h))
        v.pivotX = w / 2f; v.pivotY = min(h / 2f, 70 * d)
        v.alpha = 0f; v.scaleX = 0.92f; v.scaleY = 0.92f
        v.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setStartDelay(130).setDuration(240)
            .setInterpolator(DecelerateInterpolator(1.6f)).start()
        val l = Layer(v, RectF(s.rect))
        layers.add(l); layer = l; layerKey = s.key
    }

    private fun fadeOut(l: Layer?) {
        l ?: return
        l.view.animate().cancel()
        l.view.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setStartDelay(0).setDuration(110)
            .withEndAction { removeView(l.view); layers.remove(l) }.start()
    }

    private fun start() {
        if (!running) { running = true; last = 0; Choreographer.getInstance().postFrameCallback(frame) }
        invalidate()
    }

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(ns: Long) {
            val dt = if (last == 0L) 1f / 60f else ((ns - last) / 1e9f).coerceIn(0.001f, 0.05f)
            last = ns
            sl.step(dt, 0.42f, zeta); sr.step(dt, 0.42f, zeta)
            st.step(dt, 0.42f, zeta); sb.step(dt, 0.42f, zeta)
            rad.snap(rad.target)
            bl.step(dt, 0.40f, 0.70f); br.step(dt, 0.40f, 0.70f)
            bt.step(dt, 0.40f, 0.70f); bb.step(dt, 0.40f, 0.70f)
            press.step(dt, 0.26f, 0.62f)
            alphaV += (alphaT - alphaV) * min(1f, dt * 16f)
            if (abs(alphaT - alphaV) < 0.01f) alphaV = alphaT
            bAlpha += (bTarget - bAlpha) * min(1f, dt * 14f)
            if (abs(bTarget - bAlpha) < 0.01f) bAlpha = bTarget
            invalidate()
            val done = sl.settled() && st.settled() && sr.settled() && sb.settled() && rad.settled() &&
                bl.settled() && bt.settled() && br.settled() && bb.settled() && press.settled() &&
                alphaV == alphaT && bAlpha == bTarget
            if (done) { running = false; ctl.onSettled() } else Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun dispatchDraw(c: Canvas) {
        if (alphaV < 0.01f && bAlpha < 0.01f) return
        getLocationOnScreen(loc)
        c.save()
        val ps = press.v
        if (abs(ps - 1f) > 0.001f) {
            c.scale(ps, ps, (sl.v + sr.v) / 2 - loc[0], (st.v + sb.v) / 2 - loc[1])
        }
        if (alphaV >= 0.01f) {
            val l = sl.v - loc[0]; val t = st.v - loc[1]
            val r = max(sr.v - loc[0], l + 2f); val b = max(sb.v - loc[1], t + 2f)
            rf.set(l, t, r, b)
            val rr = min(max(rad.target, 0f), min(rf.width(), rf.height()) / 2)
            path.smoothRoundRect(rf.left, rf.top, rf.right, rf.bottom, rr)
            paint.alpha = (255 * alphaV).toInt()
            c.drawPath(path, paint)
            rim(c, path, alphaV)
            for (ly in layers) {
                ly.view.translationX = ly.rect.left - loc[0]
                ly.view.translationY = ly.rect.top - loc[1]
            }
            c.save(); c.clipPath(path)
            super.dispatchDraw(c)
            c.restore()
        }
        if (bAlpha >= 0.01f) {
            val l = bl.v - loc[0]; val t = bt.v - loc[1]
            val r = max(br.v - loc[0], l + 2f); val b = max(bb.v - loc[1], t + 2f)
            brf.set(l, t, r, b)
            val rr = min(brf.width(), brf.height()) / 2
            bpath.smoothRoundRect(brf.left, brf.top, brf.right, brf.bottom, rr)
            paint.alpha = (255 * bAlpha).toInt()
            c.drawPath(bpath, paint)
            rim(c, bpath, bAlpha)
            val ic = bIcon
            if (ic != null) {
                c.save(); c.clipPath(bpath)
                val inset = if (bFill) 0f else brf.width() * 0.26f
                ic.setBounds((brf.left + inset).toInt(), (brf.top + inset).toInt(), (brf.right - inset).toInt(), (brf.bottom - inset).toInt())
                ic.alpha = (255 * bAlpha).toInt()
                ic.draw(c)
                c.restore()
            }
        }
        c.restore()
    }

    private fun inside(e: MotionEvent) = alphaV > 0.3f && e.rawX in sl.v..sr.v && e.rawY in st.v..sb.v
    private fun insideBubble(e: MotionEvent) = bAlpha > 0.5f && e.rawX in bl.v..br.v && e.rawY in bt.v..bb.v

    private fun pressTo(v: Float) { press.target = v; start() }

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_OUTSIDE -> { ctl.onOutside(); return false }
            MotionEvent.ACTION_DOWN -> {
                bubbleDown = false
                val inMain = inside(e)
                if (!inMain && insideBubble(e)) { bubbleDown = true; pressTo(0.94f); return true }
                if (!inMain) return false
                swiped = false; ctl.onTouch(true); pressTo(0.965f)
            }
        }
        if (bubbleDown) {
            if (e.actionMasked == MotionEvent.ACTION_UP) {
                bubbleDown = false; pressTo(1f)
                if (insideBubble(e)) ctl.onBubbleTap()
            } else if (e.actionMasked == MotionEvent.ACTION_CANCEL) { bubbleDown = false; pressTo(1f) }
            return true
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { ctl.onTouch(false); pressTo(1f) }
        }
        return super.dispatchTouchEvent(e)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean { gd.onTouchEvent(e); return true }
}
