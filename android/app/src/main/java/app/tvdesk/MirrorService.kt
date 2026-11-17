package app.tvdesk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface

/**
 * Берёт картинку с экрана планшета (MediaProjection) и рисует её на внешнем дисплее (ТВ по USB-C).
 * Область, закрытая панелью клавиатуры, на ТВ не попадает.
 */
class MirrorService : Service() {

    companion object {
        const val ACTION_START = "app.tvdesk.START"
        const val ACTION_STOP = "app.tvdesk.STOP"
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
        private const val CHANNEL = "mirror"
        private const val NOTIF_ID = 7
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var dm: DisplayManager
    private var projection: MediaProjection? = null
    private var vd: VirtualDisplay? = null
    private var presentation: TvPresentation? = null
    private var surface: Surface? = null
    private var srcW = 0
    private var srcH = 0
    private var srcDpi = 0

    private val cropListener: () -> Unit = { updateCrop() }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(id: Int) { handler.postDelayed({ attachTv() }, 400) }
        override fun onDisplayRemoved(id: Int) { handler.post { if (presentation?.display?.displayId == id) detachTv() } }
        override fun onDisplayChanged(id: Int) {
            if (id == Display.DEFAULT_DISPLAY) handler.post { onRotation() }
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { shutdown(); return START_NOT_STICKY }
        if (intent?.action != ACTION_START) { stopSelf(); return START_NOT_STICKY }

        startForeground(NOTIF_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        val code = intent.getIntExtra(EXTRA_CODE, 0)
        val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
        if (data == null) { shutdown(); return START_NOT_STICKY }

        try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = mpm.getMediaProjection(code, data)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { handler.post { shutdown() } }
            }, handler)
        } catch (e: Exception) {
            AppState.mirrorNote = "Не удалось начать захват экрана: ${e.message}"
            shutdown(); return START_NOT_STICKY
        }

        dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        dm.registerDisplayListener(displayListener, handler)
        AppState.addListener(cropListener)
        readSourceSize()
        AppState.mirror = AppState.Mirror.WAITING_TV
        AppState.mirrorNote = "Ищу ТВ…"
        AppState.changed()
        attachTv()
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Вывод на ТВ", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(
            this, 0, Intent(this, MirrorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_slideshow)
            .setContentTitle("TV Desk: экран выводится на ТВ")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Остановить", stop).build())
            .setOngoing(true)
            .build()
    }

    private fun readSourceSize() {
        val d = dm.getDisplay(Display.DEFAULT_DISPLAY)
        val m = DisplayMetrics()
        @Suppress("DEPRECATION") d.getRealMetrics(m)
        srcW = m.widthPixels; srcH = m.heightPixels; srcDpi = m.densityDpi
    }

    // ---------- ТВ ----------

    private fun findTv(): Display? =
        dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .firstOrNull { it.displayId != Display.DEFAULT_DISPLAY && it.state != Display.STATE_OFF }

    private fun attachTv() {
        if (projection == null || presentation != null) return
        val tv = findTv()
        if (tv == null) {
            AppState.mirror = AppState.Mirror.WAITING_TV
            AppState.mirrorNote = "ТВ не найден как отдельный экран. Подключите ТВ по USB-C. " +
                "Если планшет зеркалит экран сам (без отдельного дисплея), эта функция недоступна, " +
                "но панель клавиатуры будет скрыта от ТВ защитой от записи экрана."
            AppState.changed()
            return
        }
        try {
            val p = TvPresentation(this, tv) { st -> onTvSurface(st) }
            presentation = p
            p.show()
            p.setCrop(srcW, srcH, currentCrop(), Prefs(this).fillTv)
            AppState.mirrorNote = "ТВ: ${tv.name}"
        } catch (e: Exception) {
            presentation = null
            AppState.mirrorNote = "Не удалось открыть окно на ТВ: ${e.message}. Проверьте разрешение «Поверх других окон»."
            AppState.changed()
        }
    }

    private fun detachTv() {
        try { vd?.surface = null } catch (_: Exception) {}
        surface?.release(); surface = null
        try { presentation?.dismiss() } catch (_: Exception) {}
        presentation = null
        AppState.mirror = AppState.Mirror.WAITING_TV
        AppState.mirrorNote = "ТВ отключён"
        AppState.changed()
    }

    private fun onTvSurface(st: SurfaceTexture?) {
        val proj = projection ?: return
        if (st == null) {
            try { vd?.surface = null } catch (_: Exception) {}
            surface?.release(); surface = null
            return
        }
        st.setDefaultBufferSize(srcW, srcH)
        val s = Surface(st)
        surface = s
        try {
            val existing = vd
            if (existing == null) {
                // Android 14+: на один MediaProjection можно создать только один VirtualDisplay —
                // дальше только подменяем поверхность.
                vd = proj.createVirtualDisplay(
                    "tvdesk-mirror", srcW, srcH, srcDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, s, null, handler,
                )
            } else {
                existing.surface = s
            }
            AppState.mirror = AppState.Mirror.STREAMING
            AppState.changed()
        } catch (e: Exception) {
            AppState.mirrorNote = "Ошибка захвата экрана: ${e.message}"
            AppState.changed()
        }
    }

    private fun onRotation() {
        val ow = srcW; val oh = srcH
        readSourceSize()
        if (ow == srcW && oh == srcH) return
        try { vd?.resize(srcW, srcH, srcDpi) } catch (_: Exception) {}
        presentation?.tvTexture()?.setDefaultBufferSize(srcW, srcH)
        updateCrop()
    }

    private fun currentCrop(): Rect = AppState.visibleRect(srcW, srcH)

    private fun updateCrop() {
        presentation?.setCrop(srcW, srcH, currentCrop(), Prefs(this).fillTv)
    }

    private fun shutdown() {
        AppState.removeListener(cropListener)
        try { if (::dm.isInitialized) dm.unregisterDisplayListener(displayListener) } catch (_: Exception) {}
        try { presentation?.dismiss() } catch (_: Exception) {}
        presentation = null
        try { vd?.release() } catch (_: Exception) {}
        vd = null
        surface?.release(); surface = null
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
        AppState.mirror = AppState.Mirror.OFF
        AppState.mirrorNote = ""
        AppState.changed()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (projection != null) shutdown()
        super.onDestroy()
    }
}
