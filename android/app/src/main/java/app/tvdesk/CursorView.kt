package app.tvdesk

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Стрелка-курсор. Острие — в левом верхнем углу вьюхи. Попадает и на экран планшета, и на ТВ. */
class CursorView(ctx: Context) : View(ctx) {
    val size = (30 * ctx.resources.displayMetrics.density).toInt()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE
        strokeWidth = 1.6f * ctx.resources.displayMetrics.density; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(size, size)

    override fun onDraw(c: Canvas) {
        val s = size.toFloat()
        path.reset()
        path.moveTo(1f, 1f)
        path.lineTo(1f, s * 0.80f)
        path.lineTo(s * 0.22f, s * 0.62f)
        path.lineTo(s * 0.37f, s * 0.95f)
        path.lineTo(s * 0.50f, s * 0.89f)
        path.lineTo(s * 0.36f, s * 0.57f)
        path.lineTo(s * 0.62f, s * 0.57f)
        path.close()
        c.drawPath(path, fill)
        c.drawPath(path, edge)
    }
}
