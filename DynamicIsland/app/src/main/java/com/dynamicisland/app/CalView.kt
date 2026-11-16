package com.dynamicisland.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/** Оверлей ручной подгонки: рамку перетаскиваем на настоящую камеру, размер правим кнопками. */
class CalView(
    ctx: Context,
    init: RectF,
    private val sw: Float,
    private val onDone: (RectF?) -> Unit,
) : FrameLayout(ctx) {
    private val d = ctx.resources.displayMetrics.density
    private var cx = init.centerX()
    private var top = init.top
    private var w = init.width().coerceAtLeast(10 * d)
    private var h = init.height().coerceAtLeast(10 * d)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var grabDx = 0f
    private var grabDy = 0f

    init {
        setWillNotDraw(false)
        setBackgroundColor(0xCC1B1B2A.toInt())
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        col.addView(tv(ctx, "Перетащите рамку точно на камеру", 15f, WHITE, true).apply { gravity = Gravity.CENTER })
        col.addView(tv(ctx, "Размер и тонкая подгонка — кнопками ниже", 12f, SEC).apply { gravity = Gravity.CENTER; setPadding(0, dpi(2f), 0, dpi(8f)) })
        col.addView(buttons("◀" to { cx -= 0.5f * d }, "▲" to { top -= 0.5f * d }, "▼" to { top += 0.5f * d }, "▶" to { cx += 0.5f * d }))
        col.addView(buttons("Ширина −" to { w -= d }, "Ширина +" to { w += d }, "Высота −" to { h -= d }, "Высота +" to { h += d }))
        col.addView(buttons("Отмена" to { onDone(null) }, "Готово" to { onDone(RectF(cx - w / 2, top, cx + w / 2, top + h)) }, color = 0xFF0A84FF.toInt()))
        addView(col, LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = dpi(14f) })
    }

    private fun buttons(vararg b: Pair<String, () -> Unit>, color: Int = 0xFF2C2C2E.toInt()) = LinearLayout(context).apply {
        setPadding(dpi(10f), dpi(4f), dpi(10f), dpi(4f))
        for ((label, act) in b) addView(TextView(context).apply {
            text = label; setTextColor(WHITE); textSize = 14f; gravity = Gravity.CENTER
            setPadding(0, dpi(11f), 0, dpi(11f))
            background = GradientDrawable().apply { setColor(color); cornerRadius = 12 * d }
            setOnClickListener { act(); clampAll(); invalidate() }
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dpi(3f), 0, dpi(3f), 0) })
    }

    private fun dpi(v: Float) = (v * d + .5f).toInt()

    private fun clampAll() {
        w = w.coerceIn(8 * d, sw * 0.9f); h = h.coerceIn(8 * d, 90 * d)
        cx = cx.coerceIn(w / 2, sw - w / 2); top = top.coerceIn(-2 * d, 150 * d)
    }

    override fun onDraw(c: Canvas) {
        val r = RectF(cx - w / 2, top, cx + w / 2, top + h)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f * d; p.color = 0xFF30D158.toInt()
        c.drawRoundRect(r, h / 2, h / 2, p)
        p.strokeWidth = 1f * d; p.color = 0x6630D158
        c.drawLine(cx, 0f, cx, top, p)
        c.drawLine(0f, top + h / 2, r.left, top + h / 2, p)
        c.drawLine(r.right, top + h / 2, sw, top + h / 2, p)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val near = abs(e.x - cx) < w / 2 + 28 * d && abs(e.y - (top + h / 2)) < h / 2 + 28 * d
                if (near) { grabDx = cx - e.x; grabDy = top - e.y } else { grabDx = 0f; grabDy = -h / 2 }
                cx = e.x + grabDx; top = e.y + grabDy
            }
            MotionEvent.ACTION_MOVE -> { cx = e.x + grabDx; top = e.y + grabDy }
        }
        clampAll(); invalidate()
        return true
    }
}
