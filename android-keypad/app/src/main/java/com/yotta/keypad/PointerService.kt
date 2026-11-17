package com.yotta.keypad

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/** Рисует курсор поверх всего и делает клики/перетаскивание/прокрутку жестами. */
class PointerService : AccessibilityService() {

    companion object {
        var instance: PointerService? = null
    }

    private lateinit var wm: WindowManager
    private var cursor: View? = null
    private var lp: WindowManager.LayoutParams? = null
    private var sw = 0
    private var sh = 0
    private var cx = 0f
    private var cy = 0f
    private val size by lazy { (28 * resources.displayMetrics.density).toInt() }

    private var dragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragStartTime = 0L
    private var scrollAcc = 0f
    private var lastScroll = 0L

    override fun onServiceConnected() {
        instance = this
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        refreshScreen()
        cx = sw / 2f; cy = sh / 2f
    }

    private fun refreshScreen() {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm)
        sw = dm.widthPixels; sh = dm.heightPixels
    }

    fun showCursor() {
        if (cursor != null) return
        refreshScreen()
        val v = object : View(this) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 3f }
            override fun onDraw(c: Canvas) {
                val p = Path().apply {
                    moveTo(2f, 2f); lineTo(2f, height * 0.85f); lineTo(width * 0.3f, height * 0.65f)
                    lineTo(width * 0.55f, height * 0.95f); lineTo(width * 0.7f, height * 0.85f)
                    lineTo(width * 0.45f, height * 0.58f); lineTo(width * 0.8f, height * 0.55f); close()
                }
                c.drawPath(p, fill); c.drawPath(p, line)
            }
        }
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = cx.toInt(); y = cy.toInt() }
        wm.addView(v, params)
        cursor = v; lp = params
    }

    fun hideCursor() {
        cursor?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        cursor = null; lp = null
    }

    fun move(dx: Float, dy: Float) {
        cx = (cx + dx).coerceIn(0f, sw - 1f)
        cy = (cy + dy).coerceIn(0f, sh - 1f)
        lp?.let { it.x = cx.toInt(); it.y = cy.toInt(); cursor?.let { v -> wm.updateViewLayout(v, it) } }
    }

    private fun gesture(path: Path, duration: Long) {
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration.coerceAtLeast(1))).build()
        dispatchGesture(g, null, null)
    }

    fun click() = gesture(Path().apply { moveTo(cx, cy); lineTo(cx, cy) }, 50)

    /** Правая кнопка: долгое нажатие (контекстное меню). */
    fun rightClick() = gesture(Path().apply { moveTo(cx, cy); lineTo(cx, cy) }, 700)

    fun dragStart() {
        dragging = true; dragStartX = cx; dragStartY = cy; dragStartTime = System.currentTimeMillis()
    }

    fun dragEnd() {
        if (!dragging) return
        dragging = false
        val dur = (System.currentTimeMillis() - dragStartTime).coerceIn(150, 3000)
        gesture(Path().apply { moveTo(dragStartX, dragStartY); lineTo(cx, cy) }, dur)
    }

    /** Прокрутка: dy>0 — вниз по содержимому, как колёсико мыши. */
    fun scroll(dy: Float) {
        scrollAcc += dy
        val now = System.currentTimeMillis()
        if (now - lastScroll < 180 || kotlin.math.abs(scrollAcc) < 8f) return
        lastScroll = now
        val d = scrollAcc.coerceIn(-sh * 0.6f, sh * 0.6f) * 2f
        scrollAcc = 0f
        val y0 = cy.coerceIn(sh * 0.2f, sh * 0.8f)
        val y1 = (y0 - d).coerceIn(1f, sh - 1f)
        gesture(Path().apply { moveTo(cx, y0); lineTo(cx, y1) }, 120)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        hideCursor(); instance = null; return super.onUnbind(intent)
    }
}
