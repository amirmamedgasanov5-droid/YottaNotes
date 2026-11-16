package com.dynamicisland.app

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class IslandController(
    private val ctx: Context,
    private val windowType: Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
) {
    val prefs = Prefs(ctx)
    private val h = Handler(Looper.getMainLooper())
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private val d = ctx.resources.displayMetrics.density
    private val vib: Vibrator? = ctx.getSystemService(Vibrator::class.java)

    private val probe = ProbeView(ctx) { r, w -> onCutout(r, w) }
    private val island = IslandView(ctx, this)
    private val probeLp = baseLp()
    private val islandLp = baseLp()
    private var islandAttached = false

    private var cutout: RectF? = null
    private var screenW = 0
    private var geo: Geometry? = null
    private var landscape = false
    private var lastSpec: Spec? = null
    private var previewUntil = 0L

    // «живые активности»: звонки, будильники, таймеры
    private val live = LinkedHashMap<String, Item>()
    private var media: MediaItem? = null
    private val suppressed = HashSet<String>()
    private var primaryOverride: String? = null
    private var expandedId: String? = null

    // разовые события
    private var current: Item? = null
    private val queue = ArrayDeque<Item>()
    private val lastSig = HashMap<String, Int>()
    private var touching = false
    private var fullShown = true
    private var chargeInit = false

    private val sb = SpecBuilder(ctx, d)
    private val mediaWatcher = MediaWatcher(ctx) { m -> onMedia(m) }

    private val dismissR = Runnable { dismissCurrent() }
    private val collapseR = Runnable { expandedId = null; render() }
    private val renderR = Runnable { render() }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_POWER_CONNECTED -> if (prefs.charging) enqueue(ChargeItem(batteryLevel(), 0))
                Intent.ACTION_POWER_DISCONNECTED -> fullShown = false
                Intent.ACTION_BATTERY_LOW -> if (prefs.charging) enqueue(ChargeItem(batteryLevel(), 1))
                Intent.ACTION_BATTERY_CHANGED -> onBattery(i)
                AudioManager.RINGER_MODE_CHANGED_ACTION -> if (prefs.system) onRinger()
                Intent.ACTION_USER_PRESENT -> if (prefs.system) enqueue(MiniItem("unlock", 2, "", WHITE, R.drawable.ic_lock))
                BluetoothDevice.ACTION_ACL_CONNECTED -> if (prefs.system) onBluetooth(i, true)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> if (prefs.system) onBluetooth(i, false)
            }
        }
    }

    // ------------------------------------------------------------ жизненный цикл

    private fun baseLp() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        windowType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= 30)
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        else WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    fun start() {
        landscape = ctx.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        probeLp.width = WindowManager.LayoutParams.MATCH_PARENT
        probeLp.height = (160 * d).toInt()
        probeLp.x = 0; probeLp.y = 0
        probeLp.flags = probeLp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        probeLp.title = "IslandProbe"
        try { wm.addView(probe, probeLp) } catch (_: Exception) {}
        islandLp.title = "DynamicIsland"
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW); addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION); addAction(Intent.ACTION_USER_PRESENT)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED); addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ctx.registerReceiver(receiver, f)
        mediaWatcher.start()
        IslandBus.controller = this
    }

    fun stop() {
        if (IslandBus.controller === this) IslandBus.controller = null
        h.removeCallbacksAndMessages(null)
        mediaWatcher.stop()
        try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {}
        try { wm.removeViewImmediate(probe) } catch (_: Exception) {}
        calView?.let { try { wm.removeViewImmediate(it) } catch (_: Exception) {} }
        if (islandAttached) try { wm.removeViewImmediate(island) } catch (_: Exception) {}
        islandAttached = false
    }

    fun onConfig() {
        landscape = ctx.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        geo = buildGeo(); sb.geoVer++
        render()
    }

    fun refreshMedia() = mediaWatcher.retry()

    // ------------------------------------------------------------ геометрия (поиск камеры)

    private fun onCutout(r: RectF?, w: Int) {
        if (r == cutout && w == screenW) return
        cutout = r; screenW = w
        h.post { geo = buildGeo(); sb.geoVer++; render() }
    }

    private fun buildGeo(): Geometry? {
        val sw = screenW.toFloat()
        if (sw <= 0f) return null
        val co = cutout
        val mode = prefs.posMode
        var cam: RectF? = null
        var real = false
        var attached = false
        if (mode == 4) {
            prefs.cal(landscape)?.let {
                val cx = it[0] / 1000f * sw
                val top = it[1] / 10f * d
                cam = RectF(cx - it[2] / 20f * d, top, cx + it[2] / 20f * d, top + it[3] / 10f * d)
                real = true; attached = top <= 1.5f
            }
        } else if (mode == 0 && co != null) {
            cam = RectF(co); real = true; attached = co.top <= 1.5f
        }
        val c: RectF = cam ?: when (mode) {
            1 -> dot(sw * 0.14f)
            3 -> dot(sw * 0.86f)
            else -> { // «рамка»: виртуальный остров как на iPhone 14 Pro
                val w = min(sw * 0.32f, 140 * d)
                val top = min(sw * 0.028f, 12 * d)
                RectF(sw / 2 - w / 2, top, sw / 2 + w / 2, top + w * 0.294f)
            }
        }
        c.offset(prefs.offX * d, prefs.offY * d)
        val pad = if (mode == 4 && real) 0f else if (real || mode in 1..3) prefs.pad * d else 0f
        return Geometry(sw, c, d, attached, pad, real)
    }

    private fun dot(cx: Float) = RectF(cx - 11 * d, 14 * d, cx + 11 * d, 36 * d)

    /** Текст для экрана настроек: что найдено. */
    fun describe(): String {
        val g = geo ?: return "определяю…"
        val side = when (g.side) { Side.LEFT -> "слева"; Side.CENTER -> "по центру"; Side.RIGHT -> "справа" }
        return if (g.real) "Камера найдена: $side (${(g.cam.width() / d).toInt()}×${(g.cam.height() / d).toInt()} dp)"
        else "Выреза нет — виртуальный остров ($side)"
    }

    fun onSettingsChanged(preview: Boolean) {
        geo = buildGeo(); sb.geoVer++
        if (preview) {
            previewUntil = SystemClock.uptimeMillis() + 2500
            h.removeCallbacks(renderR); h.postDelayed(renderR, 2600)
        }
        render()
    }

    // ------------------------------------------------------------ ручная калибровка

    private var calView: CalView? = null

    fun startCalibration() {
        val g = geo ?: return
        if (calView != null) return
        val lp = baseLp().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT; height = (340 * d).toInt()
            x = 0; y = 0; title = "IslandCalibrate"
        }
        val v = CalView(ctx, g.cam, screenW.toFloat()) { r ->
            try { wm.removeViewImmediate(calView) } catch (_: Exception) {}
            calView = null
            if (r != null) {
                prefs.setCal(landscape, Math.round(r.centerX() / screenW * 1000), Math.round(r.top / d * 10),
                    Math.round(r.width() / d * 10), Math.round(r.height() / d * 10))
                prefs.posMode = 4
                prefs.offX = 0; prefs.offY = 0
            }
            geo = buildGeo(); sb.geoVer++
            previewUntil = SystemClock.uptimeMillis() + 2500
            h.removeCallbacks(renderR); h.postDelayed(renderR, 2600)
            render()
        }
        calView = v
        try { wm.addView(v, lp) } catch (_: Exception) { calView = null }
        render()
    }

    // ------------------------------------------------------------ источники событий

    fun showNotification(n: NotifItem) {
        if (!prefs.notifications || !screenOn()) return
        val sig = (n.title.toString() + "|" + n.text).hashCode()
        if (lastSig[n.id] == sig) return
        lastSig[n.id] = sig
        enqueue(n)
    }

    fun notificationRemoved(id: String) {
        lastSig.remove(id)
        queue.removeAll { it.id == id }
        if (current?.id == id && current is NotifItem) dismissCurrent()
    }

    /** Звонок, будильник или таймер. */
    fun setLive(item: Item) {
        when (item) {
            is CallItem -> if (!prefs.calls) return
            is AlertItem, is LiveItem -> if (!prefs.timers) return
            else -> return
        }
        val old = live[item.id]
        if (old == null) suppressed.remove(item.id)
        live[item.id] = item
        if (item.urgent && old?.urgent != true) buzz()
        render(); armTimers()
    }

    fun removeLive(id: String) {
        if (live.remove(id) != null) {
            suppressed.remove(id)
            if (primaryOverride == id) primaryOverride = null
            if (expandedId == id) expandedId = null
            render(); armTimers()
        }
    }

    fun onMedia(m: MediaItem?) {
        val prev = media
        media = if (prefs.media) m else null
        if (prev == null || media == null || prev.title != media?.title) suppressed.remove("media")
        if (media == null) { if (primaryOverride == "media") primaryOverride = null; if (expandedId == "media") expandedId = null }
        render()
    }

    private fun onBattery(i: Intent) {
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val full = status == BatteryManager.BATTERY_STATUS_FULL && plugged
        if (!chargeInit) { chargeInit = true; fullShown = full; return }
        if (!plugged) fullShown = false
        else if (full && !fullShown) { fullShown = true; if (prefs.charging) enqueue(ChargeItem(100, 2)) }
    }

    private fun onRinger() {
        val am = ctx.getSystemService(AudioManager::class.java)
        val orange = 0xFFFF9F0A.toInt()
        enqueue(when (am.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> MiniItem("ringer", 0, "Без звука", orange, R.drawable.ic_bell, true)
            AudioManager.RINGER_MODE_VIBRATE -> MiniItem("ringer", 0, "Вибро", orange, R.drawable.ic_bell, false)
            else -> MiniItem("ringer", 0, "Звонок", WHITE, R.drawable.ic_bell, false)
        })
    }

    @Suppress("DEPRECATION")
    private fun onBluetooth(i: Intent, connected: Boolean) {
        val dev: BluetoothDevice = (if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        else i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)) ?: return
        val major = try { dev.bluetoothClass?.majorDeviceClass } catch (_: SecurityException) { null }
        if (major != BluetoothClass.Device.Major.AUDIO_VIDEO) return
        val name = try { dev.name } catch (_: SecurityException) { null } ?: "Наушники"
        enqueue(MiniItem("bt", 1, if (connected) name else "Отключено", if (connected) GREEN else SEC, R.drawable.ic_headphones))
    }

    // ------------------------------------------------------------ очередь разовых событий

    private fun enqueue(i: Item) {
        if (current?.id == i.id) { current = i; render(); armTimers(); return }
        queue.removeAll { it.id == i.id }
        queue.addLast(i)
        while (queue.size > 4) queue.removeFirst()
        if (current == null) advance()
    }

    private fun advance() {
        current = queue.removeFirstOrNull()
        if (current != null && current !is MiniItem) buzz()
        render(); armTimers()
    }

    private fun dismissCurrent() {
        h.removeCallbacks(dismissR)
        current = null
        advance()
    }

    private fun armTimers() {
        h.removeCallbacks(dismissR); h.removeCallbacks(collapseR)
        if (touching) return
        val c = current
        if (c != null) h.postDelayed(dismissR, when (c) {
            is ChargeItem -> 3200L
            is MiniItem -> if (c.kind == 2) 1700L else 2600L
            else -> 5200L
        })
        else {
            val p = primary()
            if (p != null && expandedId == p.id && !p.urgent) h.postDelayed(collapseR, 6500)
        }
    }

    // ------------------------------------------------------------ активности

    private fun plist(): List<Item> {
        val l = ArrayList<Item>()
        l += live.values.filter { it.urgent }
        l += live.values.filter { it is CallItem && !it.urgent }
        l += live.values.filterIsInstance<LiveItem>()
        media?.let { l += it }
        return l.filter { it.urgent || it.id !in suppressed }
    }

    private fun primary(list: List<Item> = plist()): Item? =
        list.firstOrNull { it.urgent } ?: list.firstOrNull { it.id == primaryOverride } ?: list.firstOrNull()

    private fun openItem(i: Item) {
        when (i) {
            is MediaItem -> i.open?.invoke()
            is CallItem -> i.open?.invoke()
            is LiveItem -> i.open?.invoke()
            is AlertItem -> i.open?.invoke()
            is NotifItem -> i.open?.invoke()
            else -> {}
        }
    }

    // ------------------------------------------------------------ жесты (как в iOS)

    fun onTouch(down: Boolean) { touching = down; armTimers() }

    fun onOutside() {
        val p = primary()
        if (current != null) dismissCurrent()
        else if (p != null && expandedId == p.id && !p.urgent) { expandedId = null; render(); armTimers() }
    }

    /** Нажатие: открыть приложение (раскрытое или свёрнутое — как на iPhone). */
    fun onTap() {
        val c = current
        if (c != null) { if (c is NotifItem) openItem(c); dismissCurrent(); return }
        val p = primary() ?: return
        openItem(p)
        if (!p.urgent && expandedId == p.id) { expandedId = null; render(); armTimers() }
    }

    /** Долгое нажатие: раскрыть / свернуть. */
    fun onLongPress() {
        if (current != null) { dismissCurrent(); return }
        val p = primary() ?: return
        if (p.urgent) return
        expandedId = if (expandedId == p.id) null else p.id
        buzz(); render(); armTimers()
    }

    fun onSwipeUp() {
        val p = primary()
        if (current != null) dismissCurrent()
        else if (p != null && expandedId == p.id && !p.urgent) { expandedId = null; render(); armTimers() }
    }

    /** Свайп вбок: убрать остров (до следующего события). */
    fun onSwipeSide() {
        if (current != null) { dismissCurrent(); return }
        val p = primary() ?: return
        if (p.urgent) return
        suppressed.add(p.id); expandedId = null
        if (primaryOverride == p.id) primaryOverride = null
        buzz(); render()
    }

    fun onBubbleTap() {
        val list = plist()
        val p = primary(list)
        val s = list.firstOrNull { it !== p } ?: return
        primaryOverride = s.id; expandedId = null
        buzz(); render()
    }

    // ------------------------------------------------------------ рендер

    fun render() {
        val g = geo ?: return
        val list = plist()
        val p = primary(list)
        val cur = current
        val tablet = ctx.resources.configuration.smallestScreenWidthDp >= 600
        val hideLand = landscape && !tablet && !(prefs.posMode == 4 && prefs.cal(true) != null)
        val spec = when {
            !prefs.enabled || calView != null || hideLand -> sb.hidden(g)
            cur != null -> sb.transient(g, cur)
            p != null && (p.urgent || expandedId == p.id) -> sb.expanded(g, p)
            p != null -> sb.compact(g, p, list.firstOrNull { it !== p })
            prefs.idleAlways || SystemClock.uptimeMillis() < previewUntil -> sb.idle(g)
            else -> sb.hidden(g)
        }
        present(spec)
    }

    private var winRect: RectF? = null
    private var winTouch = false
    private var presentPending: Runnable? = null
    private val shrinkR = Runnable {
        val s = lastSpec ?: return@Runnable
        val r = RectF(s.rect)
        s.bubble?.let { if (s.visible) r.union(it) }
        if (s.visible) r.inset(-4 * d, -4 * d)
        setWindow(r, s.touchable)
    }

    private fun present(s: Spec) {
        lastSpec = s
        h.removeCallbacks(shrinkR)
        presentPending?.let { h.removeCallbacks(it) }
        val u = RectF(s.rect)
        s.bubble?.let { u.union(it) }
        if (islandAttached) { u.union(island.bounds()); island.bubbleBounds()?.let { u.union(it) } }
        if (s.visible) u.union(s.origin)
        u.inset(-22 * d, -22 * d)
        val w = winRect
        val fits = islandAttached && w != null && w.left <= max(0f, u.left) + 1 && w.top <= max(0f, u.top) + 1 &&
            w.right >= min(screenW.toFloat(), u.right) - 1 && w.bottom >= u.bottom - 1 && winTouch == s.touchable
        if (fits) { island.present(s); return }
        setWindow(u, s.touchable)
        // даём системе применить новый размер окна и только потом запускаем пружины — без обрезанных первых кадров
        val run = Runnable { presentPending = null; island.present(s) }
        presentPending = run
        h.postDelayed(run, 48)
    }

    /** Когда пружины успокоились — ужимаем окно до размера острова, чтобы не перехватывать касания. */
    fun onSettled() {
        h.removeCallbacks(shrinkR)
        h.postDelayed(shrinkR, 350)
    }

    private fun setWindow(r: RectF, touchable: Boolean) {
        val sw = screenW.toFloat()
        val l = max(0f, r.left); val t = max(0f, r.top)
        val rr = min(sw, r.right); val b = max(t + 2f, r.bottom)
        winRect = RectF(l, t, rr, b); winTouch = touchable
        islandLp.x = floor(l).toInt(); islandLp.y = floor(t).toInt()
        islandLp.width = max(2, ceil(rr - l).toInt()); islandLp.height = max(2, ceil(b - t).toInt())
        val base = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        islandLp.flags = base or if (touchable) WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        try {
            if (!islandAttached) { wm.addView(island, islandLp); islandAttached = true }
            else wm.updateViewLayout(island, islandLp)
        } catch (_: Exception) {}
    }

    // ------------------------------------------------------------ утилиты

    private fun screenOn() = ctx.getSystemService(PowerManager::class.java).isInteractive

    private fun batteryLevel(): Int {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val lvl = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (scale > 0) lvl * 100 / scale else lvl
    }

    private fun buzz() {
        if (!prefs.haptics) return
        try {
            vib?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } catch (_: Exception) {}
    }

    // ------------------------------------------------------------ демо

    fun demo(kind: String) {
        val appIcon = ctx.packageManager.getApplicationIcon(ctx.packageName)
        val orange = 0xFFFF9F0A.toInt()
        when (kind) {
            "notif" -> showNotification(NotifItem(
                "demo:" + SystemClock.uptimeMillis(), "Сообщения", appIcon,
                letterAvatar(ctx, "А", 0xFF0A84FF.toInt()), "Алекс",
                "Ты уже видел новый Dynamic Island? Выглядит огонь 🔥 Давай попробуем на твоём телефоне, там вообще всё очень плавно.",
                { Launch.send(ctx, null, ctx.packageName) }))
            "charge" -> enqueue(ChargeItem(batteryLevel().coerceIn(1, 100), 0))
            "low" -> enqueue(ChargeItem(14, 1))
            "ringer" -> enqueue(MiniItem("ringer", 0, "Без звука", orange, R.drawable.ic_bell, true))
            "bt" -> enqueue(MiniItem("bt", 1, "AirPods", GREEN, R.drawable.ic_headphones))
            "unlock" -> enqueue(MiniItem("unlock", 2, "", WHITE, R.drawable.ic_lock))
            "music" -> demoMusic()
            "call" -> startDemoCall()
            "timer" -> {
                val stop = Act("Стоп") { removeLive("demo-timer") }
                setLive(LiveItem("demo-timer", "Таймер", "Таймер", "Чай 🍵", appIcon,
                    SystemClock.elapsedRealtime() + 5 * 60_000L, true, listOf(stop, Act("Пауза") {}), null))
            }
            "alarm" -> setLive(AlertItem("demo-alarm", "07:30", "Подъём", appIcon,
                listOf(Act("Отложить") { removeLive("demo-alarm") }, Act("Стоп") { removeLive("demo-alarm") }), null))
            "split" -> { demoMusic(); demo("timer") }
        }
    }

    private fun demoMusic() {
        val m = MediaItem().apply {
            app = "Музыка"; title = "Blinding Lights"; artist = "The Weeknd"
            art = coverArt(ctx, 0xFFFF2D55.toInt(), 0xFF3A0CA3.toInt()); color = 0xFFFF375F.toInt()
            playing = true; durMs = 200_000; posMs = 42_000; stamp = SystemClock.elapsedRealtime()
        }
        m.onToggle = { }
        m.onSeek = { }
        m.open = { Launch.send(ctx, null, ctx.packageName) }
        onMedia(m)
        h.removeCallbacks(clearDemoMedia); h.postDelayed(clearDemoMedia, 60_000)
    }

    private val clearDemoMedia = Runnable { if (media?.app == "Музыка") onMedia(null) }

    private fun startDemoCall() {
        val avatar = letterAvatar(ctx, "М", 0xFFFF9F0A.toInt())
        val end = Act("Сбросить") { removeLive("demo-call") }
        val ring = CallItem("demo-call", "Мама", null, avatar, true, System.currentTimeMillis(),
            Act("Принять") {
                setLive(CallItem("demo-call", "Мама", null, avatar, false, System.currentTimeMillis(), null, end, null))
            }, end, null)
        setLive(ring)
    }
}
