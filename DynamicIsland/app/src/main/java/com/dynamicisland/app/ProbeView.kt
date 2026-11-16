package com.dynamicisland.app

import android.content.Context
import android.graphics.RectF
import android.view.View
import android.view.WindowInsets

/**
 * Прозрачное окно на всю ширину сверху экрана, нужное только затем, чтобы система
 * прислала DisplayCutout.boundingRects — это и есть реальное расположение камеры.
 */
class ProbeView(
    ctx: Context,
    private val onCutout: (RectF?, Int) -> Unit,
) : View(ctx) {

    private var found: RectF? = null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (w > 0) onCutout(found, w)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        found = null
        val w = if (width > 0) width else resources.displayMetrics.widthPixels
        val limit = 140 * resources.displayMetrics.density
        val cut = insets.displayCutout
        if (cut != null) {
            found = cut.boundingRects
                .filter { !it.isEmpty && it.top < limit && it.width() < w * 0.9f }
                .minByOrNull { it.top }
                ?.let { RectF(it) }
        }
        onCutout(found, w)
        return super.onApplyWindowInsets(insets)
    }
}
