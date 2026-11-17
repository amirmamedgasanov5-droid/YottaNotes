package app.tvdesk

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.TextureView
import android.view.WindowManager
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min

/** Окно на внешнем дисплее (ТВ). Показывает кусок экрана планшета, без области панели клавиатуры. */
class TvPresentation(
    ctx: Context,
    display: Display,
    private val onSurface: (SurfaceTexture?) -> Unit,
) : Presentation(ctx, display) {

    private lateinit var container: FrameLayout
    private lateinit var tex: TextureView
    private var srcW = 0
    private var srcH = 0
    private var crop = Rect()
    private var fill = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.apply {
            setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        container = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
        tex = TextureView(context)
        tex.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                if (srcW > 0) st.setDefaultBufferSize(srcW, srcH)
                onSurface(st)
            }
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { onSurface(null); return true }
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
        container.addView(tex, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        setContentView(container)
    }

    fun tvTexture(): SurfaceTexture? = if (::tex.isInitialized) tex.surfaceTexture else null

    fun tvSize(): Pair<Int, Int> {
        val dm = context.resources.displayMetrics
        val w = if (container.width > 0) container.width else dm.widthPixels
        val h = if (container.height > 0) container.height else dm.heightPixels
        return w to h
    }

    /** Настроить, какой кусок исходного экрана ([src] = размер экрана планшета) показывать на ТВ. */
    fun setCrop(srcW: Int, srcH: Int, crop: Rect, fill: Boolean) {
        this.srcW = srcW; this.srcH = srcH; this.crop = Rect(crop); this.fill = fill
        if (!::tex.isInitialized) return
        tex.surfaceTexture?.setDefaultBufferSize(srcW, srcH)
        container.post { applyTransform() }
    }

    private fun applyTransform() {
        if (srcW <= 0 || srcH <= 0 || crop.isEmpty) return
        val (vw, vh) = tvSize()
        var r = Rect(crop)
        val s: Float
        if (fill) {
            // обрезаем область по центру до пропорций ТВ
            s = max(vw.toFloat() / r.width(), vh.toFloat() / r.height())
            val cw = vw / s; val ch = vh / s
            val nx = r.centerX() - cw / 2; val ny = r.centerY() - ch / 2
            r = Rect(nx.toInt(), ny.toInt(), (nx + cw).toInt(), (ny + ch).toInt())
        } else {
            s = min(vw.toFloat() / r.width(), vh.toFloat() / r.height())
        }
        val viewW = (r.width() * s).toInt().coerceAtLeast(1)
        val viewH = (r.height() * s).toInt().coerceAtLeast(1)
        tex.layoutParams = FrameLayout.LayoutParams(viewW, viewH, Gravity.CENTER)
        // Текстура по умолчанию растянута на весь TextureView: увеличиваем до нужного куска.
        val m = Matrix()
        m.setScale(srcW.toFloat() / r.width(), srcH.toFloat() / r.height())
        m.postTranslate(-r.left * s, -r.top * s)
        tex.setTransform(m)
    }
}
