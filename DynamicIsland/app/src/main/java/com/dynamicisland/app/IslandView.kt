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
)

class IslandView(ctx: Context, private val ctl: IslandController) : FrameLayout(ctx) {
    private val d = ctx.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }

    // Четыре края + радиус — каждый на своей пружине: форма «желейно» деформируется как в iOS.
    private val sl = Spring(); private val st = Spring(); private val sr = Spring(); private val sb = Spring()
    private val rad = Spring()
    private var zeta = 0.7f
    private var alphaV = 0f
    private var alphaT = 0f

    private val loc = IntArray(2)
    private val path = Path()
    private val rf = RectF()
    private var last = 0L
    private var running = false

    private class Layer(val view: View, val rect: RectF)
    private val layers = ArrayList<Layer>()
    private var layer: Layer? = null
    private var layerKey: String? = null

    private var swiped = false
    private val gd = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapUp(e: MotionEvent): Boolean { ctl.onTap(); return true }
        override fun onLongPress(e: MotionEvent) { ctl.onLongPress() }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (e1 != null && !swiped && e2.y - e1.y < -28 * d) { swiped = true; ctl.onSwipeUp() }
            return true
        }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (!swiped && vy < -900) { swiped = true; ctl.onSwipeUp() }
            return true
        }
    })

    init { setWillNotDraw(false) }

    fun bounds() = RectF(sl.v, st.v, sr.v, sb.v)

    fun present(s: Spec) {
        if (s.visible && alphaV < 0.03f) snap(s.origin)
        val cur = bounds()
        val grow = s.rect.width() * s.rect.height() > cur.width() * cur.height()
        zeta = if (grow) 0.60f else 0.80f
        sl.target = s.rect.left; st.target = s.rect.top
        sr.target = s.rect.right; sb.target = s.rect.bottom
        rad.target = s.radius
        alphaT = if (s.visible) 1f else 0f
        updateContent(s)
        start()
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
        v.alpha = 0f; v.scaleX = 0.85f; v.scaleY = 0.85f
        v.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setStartDelay(100).setDuration(280)
            .setInterpolator(DecelerateInterpolator(1.6f)).start()
        if (Build.VERSION.SDK_INT >= 31) {
            ValueAnimator.ofFloat(18f, 0f).apply {
                startDelay = 100; duration = 300
                addUpdateListener {
                    val b = it.animatedValue as Float
                    v.setRenderEffect(if (b < 0.6f) null else RenderEffect.createBlurEffect(b, b, Shader.TileMode.CLAMP))
                }
                start()
            }
        }
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
            sl.step(dt, 0.40f, zeta); sr.step(dt, 0.40f, zeta)
            st.step(dt, 0.44f, zeta); sb.step(dt, 0.50f, zeta)
            rad.step(dt, 0.38f, 0.85f)
            alphaV += (alphaT - alphaV) * min(1f, dt * 16f)
            if (abs(alphaT - alphaV) < 0.01f) alphaV = alphaT
            invalidate()
            val done = sl.settled() && st.settled() && sr.settled() && sb.settled() && rad.settled() && alphaV == alphaT
            if (done) { running = false; ctl.onSettled() } else Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun dispatchDraw(c: Canvas) {
        if (alphaV < 0.01f) return
        getLocationOnScreen(loc)
        val l = sl.v - loc[0]; val t = st.v - loc[1]
        val r = max(sr.v - loc[0], l + 2f); val b = max(sb.v - loc[1], t + 2f)
        rf.set(l, t, r, b)
        val rr = min(max(rad.v, 0f), min(rf.width(), rf.height()) / 2)
        path.rewind(); path.addRoundRect(rf, rr, rr, Path.Direction.CW)
        paint.alpha = (255 * alphaV).toInt()
        c.drawPath(path, paint)
        for (ly in layers) {
            ly.view.translationX = ly.rect.left - loc[0]
            ly.view.translationY = ly.rect.top - loc[1]
        }
        c.save(); c.clipPath(path)
        super.dispatchDraw(c)
        c.restore()
    }

    private fun inside(e: MotionEvent) = e.rawX in sl.v..sr.v && e.rawY in st.v..sb.v

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_OUTSIDE -> { ctl.onOutside(); return false }
            MotionEvent.ACTION_DOWN -> { if (!inside(e)) return false; swiped = false; ctl.onTouch(true) }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> ctl.onTouch(false)
        }
        return super.dispatchTouchEvent(e)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean { gd.onTouchEvent(e); return true }
}
