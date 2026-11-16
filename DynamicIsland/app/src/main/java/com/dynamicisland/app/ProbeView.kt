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

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        var found: RectF? = null
        val limit = 140 * resources.displayMetrics.density
        val cut = insets.displayCutout
        if (cut != null) {
            found = cut.boundingRects
                .filter { !it.isEmpty && it.top < limit && it.width() < width * 0.9f }
                .minByOrNull { it.top }
                ?.let { RectF(it) }
        }
        onCutout(found, width)
        return super.onApplyWindowInsets(insets)
    }
}
