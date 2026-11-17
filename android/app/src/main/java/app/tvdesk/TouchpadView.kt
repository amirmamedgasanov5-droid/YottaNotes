package app.tvdesk

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.hypot

/**
 * Тачпад как на ноутбуке:
 *  • один палец — двигает курсор; тап — клик;
 *  • тап и сразу прижать палец + двигать — перетаскивание (двойной тап = двойной клик);
 *  • долгое удержание на месте — долгое нажатие (контекстное меню);
 *  • два пальца — прокрутка; тап двумя пальцами — правый клик (долгое нажатие).
 */
class TouchpadView(ctx: Context, private val l: Listener) : View(ctx) {

    interface Listener {
        fun onMove(dx: Float, dy: Float)
        fun onTap()
        fun onTwoTap()
        fun onScroll(dx: Float, dy: Float)
        fun onDragStart()
        fun onDragEnd()
        fun onLongPress()
    }

    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop.toFloat()
    private val d = ctx.resources.displayMetrics.density

    private var mode = 0 // 0 нет, 1 один палец, 2 два пальца, 3 игнор до отпускания
    private var downT = 0L
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var moved = false
    private var dragging = false
    private var longFired = false
    private var lastTapT = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var twoT = 0L
    private var twoMoved = false
    private var twoX = 0f
    private var twoY = 0f
    private var twoStartX = 0f
    private var twoStartY = 0f

    private val longRunnable = Runnable {
        if (mode == 1 && !moved && !dragging) { longFired = true; l.onLongPress() }
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E252E.toInt() }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3A4553.toInt(); style = Paint.Style.STROKE; strokeWidth = 2 * d
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 255, 255, 255); textAlign = Paint.Align.CENTER; textSize = 13 * d
    }

    override fun onDraw(c: Canvas) {
        val r = RectF(2 * d, 2 * d, width - 2 * d, height - 2 * d)
        c.drawRoundRect(r, 12 * d, 12 * d, bgPaint)
        c.drawRoundRect(r, 12 * d, 12 * d, edgePaint)
        c.drawText("Тачпад", width / 2f, height / 2f - 8 * d, textPaint)
        c.drawText("1 палец — курсор, 2 — прокрутка", width / 2f, height / 2f + 10 * d, textPaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mode = 1; downT = e.eventTime; downX = e.x; downY = e.y
                lastX = e.x; lastY = e.y; moved = false; dragging = false; longFired = false
                if (e.eventTime - lastTapT < 300 && hypot(e.x - lastTapX, e.y - lastTapY) < 60 * d) {
                    dragging = true; moved = true
                    l.onDragStart()
                } else postDelayed(longRunnable, 600)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                removeCallbacks(longRunnable)
                if (e.pointerCount == 2 && mode == 1) {
                    if (dragging) { l.onDragEnd(); dragging = false }
                    mode = 2; twoT = e.eventTime; twoMoved = false
                    twoX = (e.getX(0) + e.getX(1)) / 2; twoY = (e.getY(0) + e.getY(1)) / 2
                    twoStartX = twoX; twoStartY = twoY
                } else mode = 3
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == 1 && e.pointerCount == 1) {
                    if (!moved) {
                        if (hypot(e.x - downX, e.y - downY) > slop) {
                            moved = true; removeCallbacks(longRunnable)
                            lastX = e.x; lastY = e.y
                        }
                    } else {
                        l.onMove(e.x - lastX, e.y - lastY)
                        lastX = e.x; lastY = e.y
                    }
                } else if (mode == 2 && e.pointerCount >= 2) {
                    val cx = (e.getX(0) + e.getX(1)) / 2
                    val cy = (e.getY(0) + e.getY(1)) / 2
                    if (!twoMoved && hypot(cx - twoStartX, cy - twoStartY) > slop * 1.5f) {
                        twoMoved = true; twoX = cx; twoY = cy
                    } else if (twoMoved) {
                        l.onScroll(cx - twoX, cy - twoY)
                        twoX = cx; twoY = cy
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (mode == 2) {
                    if (!twoMoved && e.eventTime - twoT < 350) l.onTwoTap()
                    mode = 3
                }
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(longRunnable)
                if (mode == 1) {
                    if (dragging) { l.onDragEnd(); lastTapT = 0 }
                    else if (!moved && !longFired && e.eventTime - downT < 300) {
                        l.onTap(); lastTapT = e.eventTime; lastTapX = e.x; lastTapY = e.y
                    }
                }
                mode = 0; dragging = false
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longRunnable)
                if (dragging) l.onDragEnd()
                mode = 0; dragging = false
            }
        }
        return true
    }
}
