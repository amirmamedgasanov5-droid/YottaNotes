package com.dynamicisland.app

import android.content.Context
import android.graphics.RectF
import android.view.View
import android.view.WindowInsets

/**
 * Прозрачное окно на всю ширину сверху экрана, нужное только затем, чтобы система
 * прислала DisplayCutout.boundingRects — это и есть реальное расположение камеры.
 * Координаты переводятся в экранные с учётом того, куда оболочка реально поставила окно.
 */
class ProbeView(
    ctx: Context,
    private val onCutout: (RectF?, Int) -> Unit,
) : View(ctx) {

    private var raw: RectF? = null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        post { report() }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        raw = null
        val w = if (width > 0) width else resources.displayMetrics.widthPixels
        val limit = 160 * resources.displayMetrics.density
        val cut = insets.displayCutout
        if (cut != null) {
            raw = cut.boundingRects
                .filter { !it.isEmpty && it.top < limit && it.width() < w * 0.9f }
                .minByOrNull { it.top }
                ?.let { RectF(it) }
        }
        report()
        post { report() }
        return super.onApplyWindowInsets(insets)
    }

    private fun report() {
        val w = if (width > 0) width else resources.displayMetrics.widthPixels
        val loc = IntArray(2)
        if (isAttachedToWindow) getLocationOnScreen(loc)
        val r = raw?.let { RectF(it).apply { offset(loc[0].toFloat(), loc[1].toFloat()) } }
        onCutout(r, w)
    }
}
