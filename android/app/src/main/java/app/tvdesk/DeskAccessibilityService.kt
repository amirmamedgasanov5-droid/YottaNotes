package app.tvdesk

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Сердце управления: рисует курсор и панель клавиатуры/тачпада поверх всех приложений
 * и превращает движения тачпада в настоящие касания (клик, долгое нажатие, прокрутка, перетаскивание).
 * Содержимое экрана НЕ читается.
 */
class DeskAccessibilityService : AccessibilityService(),
    TouchpadView.Listener, PanelView.Host {

    private lateinit var wm: WindowManager
    private lateinit var prefs: Prefs

    private var panel: PanelView? = null
    private var handle: View? = null
    private var cursor: CursorView? = null
    private var cursorLp: WindowManager.LayoutParams? = null

    private var cx = 0f
    private var cy = 0f

    private val layoutChange = View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ -> publishCovered(v) }

    // ---------- жизненный цикл ----------

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = Prefs(this)
        AppState.service = this
        AppState.changed()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        tearDown()
        AppState.service = null
        AppState.changed()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        tearDown()
        if (AppState.service === this) AppState.service = null
        super.onDestroy()
    }

    private fun tearDown() {
        removeView(panel); panel = null
        removeView(handle); handle = null
        removeView(cursor); cursor = null
        AppState.covered.setEmpty()
        AppState.changed()
    }

    private fun removeView(v: View?) {
        if (v == null) return
        try { v.removeOnLayoutChangeListener(layoutChange) } catch (_: Exception) {}
        try { wm.removeView(v) } catch (_: Exception) {}
    }

    // ---------- экран ----------

    private fun screen(): Rect = wm.maximumWindowMetrics.bounds

    private fun visible(): Rect {
        val s = screen()
        return AppState.visibleRect(s.width(), s.height())
    }

    fun isPanelShown() = panel != null
    fun isActive() = panel != null || handle != null

    // ---------- панель ----------

    fun showPanel() {
        if (!::wm.isInitialized) return
        removeView(handle); handle = null
        if (panel != null) return
        ensureCursor()

        val s = screen()
        AppState.panelDockTop = prefs.dockTop
        val p = PanelView(this, this, this, prefs, s.width())
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                // панель не попадает в снимок экрана / системное зеркалирование
                WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT,
        )
        lp.gravity = (if (prefs.dockTop) Gravity.TOP else Gravity.BOTTOM) or Gravity.START
        p.addOnLayoutChangeListener(layoutChange)
        wm.addView(p, lp)
        panel = p
        // курсор поверх панели в z-порядке
        cursor?.let { c -> cursorLp?.let { l -> try { wm.removeView(c); wm.addView(c, l) } catch (_: Exception) {} } }
        AppState.changed()
    }

    private fun publishCovered(v: View) {
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        val r = Rect(loc[0], loc[1], loc[0] + v.width, loc[1] + v.height)
        if (v.height > 0 && r != AppState.covered) {
            AppState.covered.set(r)
            clampCursor(); applyCursor()
            AppState.changed()
        }
    }

    override fun minimize() {
        val p = panel ?: return
        removeView(p); panel = null
        AppState.covered.setEmpty()

        val h = object : View(this) {}
        val dm = resources.displayMetrics
        h.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0x66000000); cornerRadius = 12 * dm.density
            setStroke((2 * dm.density).toInt(), 0x88FFFFFF.toInt())
        }
        val tv = android.widget.TextView(this).apply {
            text = "⌨"; setTextColor(0xCCFFFFFF.toInt()); textSize = 22f; gravity = Gravity.CENTER
            background = h.background
            setOnClickListener { showPanel() }
        }
        val size = (52 * dm.density).toInt()
        val lp = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT,
        )
        lp.gravity = Gravity.BOTTOM or Gravity.END
        lp.x = (12 * dm.density).toInt(); lp.y = (12 * dm.density).toInt()
        wm.addView(tv, lp)
        handle = tv
        cursor?.let { c -> cursorLp?.let { l -> try { wm.removeView(c); wm.addView(c, l) } catch (_: Exception) {} } }
        AppState.changed()
    }

    override fun close() {
        endDragNow()
        tearDown()
    }

    override fun toggleDock() {
        prefs.dockTop = !prefs.dockTop
        removeView(panel); panel = null
        AppState.covered.setEmpty()
        showPanel()
    }

    override fun openSettings() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun pickKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    override fun back() { performGlobalAction(GLOBAL_ACTION_BACK) }
    override fun home() { performGlobalAction(GLOBAL_ACTION_HOME) }
    override fun recents() { performGlobalAction(GLOBAL_ACTION_RECENTS) }
    override fun screenshot() { performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) }

    // ---------- курсор ----------

    private fun ensureCursor() {
        if (cursor != null) return
        val c = CursorView(this)
        val lp = WindowManager.LayoutParams(
            c.size, c.size, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        lp.gravity = Gravity.TOP or Gravity.START
        val s = screen()
        cx = s.width() / 2f; cy = s.height() / 3f
        lp.x = cx.toInt(); lp.y = cy.toInt()
        wm.addView(c, lp)
        cursor = c; cursorLp = lp
    }

    private fun clampCursor() {
        val v = visible()
        cx = cx.coerceIn(v.left.toFloat(), (v.right - 1).toFloat())
        cy = cy.coerceIn(v.top.toFloat(), (v.bottom - 1).toFloat())
    }

    private fun applyCursor() {
        val c = cursor ?: return
        val lp = cursorLp ?: return
        lp.x = cx.toInt(); lp.y = cy.toInt()
        try { wm.updateViewLayout(c, lp) } catch (_: Exception) {}
    }

    // ---------- события тачпада ----------

    override fun onMove(dx: Float, dy: Float) {
        val len = hypot(dx, dy)
        val gain = prefs.pointerSpeed * (1f + min(len, 40f) / 20f)
        cx += dx * gain; cy += dy * gain
        clampCursor(); applyCursor()
        if (dragStroke != null) pump()
    }

    override fun onTap() { tap(50) }
    override fun onLongPress() { tap(700) }
    override fun onTwoTap() { tap(700) } // правый клик на Android = долгое нажатие

    private var scrollAccX = 0f
    private var scrollAccY = 0f

    override fun onScroll(dx: Float, dy: Float) {
        val k = prefs.scrollSpeed * if (prefs.naturalScroll) 2.2f else -2.2f
        scrollAccX += dx * k; scrollAccY += dy * k
        if (gBusy || dragStroke != null || gq.isNotEmpty()) return
        if (abs(scrollAccX) < 40f && abs(scrollAccY) < 40f) return
        val s = screen()
        val sx = scrollAccX.coerceIn(-700f, 700f)
        val sy = scrollAccY.coerceIn(-700f, 700f)
        scrollAccX = 0f; scrollAccY = 0f
        val x0 = cx.coerceIn(24f, s.width() - 24f)
        val y0 = cy.coerceIn(24f, s.height() - 24f)
        val x1 = (x0 + sx).coerceIn(2f, s.width() - 2f)
        val y1 = (y0 + sy).coerceIn(2f, s.height() - 2f)
        val path = Path().apply { moveTo(x0, y0); lineTo(x1, y1) }
        enqueue(GestureDescription.Builder().addStroke(StrokeDescription(path, 0, 110)).build(), false)
    }

    override fun onDragStart() { beginDrag() }
    override fun onDragEnd() { dragEndReq = true; pump() }

    // ---------- отправка жестов ----------

    private val gq = ArrayDeque<GestureDescription>()
    private var gBusy = false
    private var dragStroke: StrokeDescription? = null
    private var dragX = 0f
    private var dragY = 0f
    private var dragEndReq = false
    private var currentIsDrag = false

    private fun tap(ms: Long) {
        if (dragStroke != null) return
        val path = Path().apply { moveTo(cx, cy) }
        enqueue(GestureDescription.Builder().addStroke(StrokeDescription(path, 0, ms)).build(), false)
    }

    private fun enqueue(g: GestureDescription, isDrag: Boolean) {
        if (isDrag) { dispatchNow(g, true); return }
        gq.addLast(g)
        pump()
    }

    private fun pump() {
        if (gBusy) return
        val next = gq.removeFirstOrNull()
        if (next != null) { dispatchNow(next, false); return }
        pumpDrag()
    }

    private fun dispatchNow(g: GestureDescription, isDrag: Boolean) {
        gBusy = true
        currentIsDrag = isDrag
        val ok = dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { gBusy = false; pump() }
            override fun onCancelled(d: GestureDescription?) {
                gBusy = false
                if (currentIsDrag) { dragStroke = null; dragEndReq = false }
                pump()
            }
        }, null)
        if (!ok) {
            gBusy = false
            if (isDrag) { dragStroke = null; dragEndReq = false }
        }
    }

    private fun beginDrag() {
        if (dragStroke != null) return
        dragX = cx; dragY = cy; dragEndReq = false
        val path = Path().apply { moveTo(dragX, dragY) }
        val s = StrokeDescription(path, 0, 40, true)
        dragStroke = s
        // сначала дожидаемся уже идущих жестов, потом стартуем касание
        if (gBusy) {
            // нет смысла ждать: отменим очередь и начнём сразу после завершения текущего
            gq.clear()
            pendingDragStart = GestureDescription.Builder().addStroke(s).build()
        } else dispatchNow(GestureDescription.Builder().addStroke(s).build(), true)
    }

    private var pendingDragStart: GestureDescription? = null

    private fun pumpDrag() {
        pendingDragStart?.let {
            pendingDragStart = null
            dispatchNow(it, true); return
        }
        val s = dragStroke ?: return
        val tx = cx; val ty = cy
        val path = Path().apply {
            moveTo(dragX, dragY)
            if (tx != dragX || ty != dragY) lineTo(tx, ty)
        }
        if (dragEndReq) {
            val ns = s.continueStroke(path, 0, 30, false)
            dragStroke = null; dragEndReq = false
            dragX = tx; dragY = ty
            dispatchNow(GestureDescription.Builder().addStroke(ns).build(), false)
        } else if (tx != dragX || ty != dragY) {
            val dist = hypot(tx - dragX, ty - dragY)
            val dur = (16 + dist / 8f).toLong().coerceAtMost(100)
            val ns = s.continueStroke(path, 0, dur, true)
            dragStroke = ns
            dragX = tx; dragY = ty
            dispatchNow(GestureDescription.Builder().addStroke(ns).build(), true)
        }
    }

    private fun endDragNow() {
        if (dragStroke != null) { dragEndReq = true; pump() }
    }
}
