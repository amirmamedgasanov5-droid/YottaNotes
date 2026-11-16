package com.dynamicisland.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.WindowManager
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class IslandController(private val ctx: Context) {
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

    private var media: MediaItem? = null
    private var call: CallItem? = null
    private var current: Item? = null
    private val queue = ArrayDeque<Item>()
    private val lastSig = HashMap<String, Int>()
    private var userExpanded = false
    private var touching = false

    private val mediaWatcher = MediaWatcher(ctx) { m -> onMedia(m) }

    private val dismissR = Runnable { dismissCurrent() }
    private val collapseR = Runnable { userExpanded = false; render() }
    private val renderR = Runnable { render() }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_POWER_CONNECTED -> if (prefs.charging) enqueue(ChargeItem(batteryLevel(), 0))
                Intent.ACTION_BATTERY_LOW -> if (prefs.charging) enqueue(ChargeItem(batteryLevel(), 1))
            }
        }
    }

    // ------------------------------------------------------------ жизненный цикл

    private fun baseLp() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
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
        landscape = ctx.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        probeLp.width = WindowManager.LayoutParams.MATCH_PARENT
        probeLp.height = (140 * d).toInt()
        probeLp.x = 0; probeLp.y = 0
        probeLp.flags = probeLp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        probeLp.title = "IslandProbe"
        try { wm.addView(probe, probeLp) } catch (_: Exception) {}
        islandLp.title = "DynamicIsland"
        val f = IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_BATTERY_LOW) }
        ctx.registerReceiver(powerReceiver, f)
        mediaWatcher.start()
        IslandBus.controller = this
    }

    fun stop() {
        IslandBus.controller = null
        h.removeCallbacksAndMessages(null)
        mediaWatcher.stop()
        try { ctx.unregisterReceiver(powerReceiver) } catch (_: Exception) {}
        try { wm.removeViewImmediate(probe) } catch (_: Exception) {}
        if (islandAttached) try { wm.removeViewImmediate(island) } catch (_: Exception) {}
        islandAttached = false
    }

    fun onConfig() {
        landscape = ctx.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        render()
    }

    fun refreshMedia() = mediaWatcher.retry()

    // ------------------------------------------------------------ геометрия (поиск камеры)

    private fun onCutout(r: RectF?, w: Int) {
        if (r == cutout && w == screenW) return
        cutout = r; screenW = w
        h.post { geo = buildGeo(); render() }
    }

    private fun buildGeo(): Geometry? {
        val sw = screenW.toFloat()
        if (sw <= 0f) return null
        val co = cutout
        val mode = prefs.posMode
        val cam: RectF
        var real = false
        var attached = false
        if (mode == 0 && co != null) {
            cam = RectF(co); real = true; attached = co.top <= 1.5f
        } else {
            cam = when (mode) {
                1 -> dot(sw * 0.14f)
                3 -> dot(sw * 0.86f)
                else -> { // «рамка»: виртуальный остров как на iPhone 14 Pro
                    val w = sw * 0.32f
                    val top = sw * 0.028f
                    RectF(sw / 2 - w / 2, top, sw / 2 + w / 2, top + w * 0.294f)
                }
            }
        }
        cam.offset(prefs.offX * d, prefs.offY * d)
        return Geometry(sw, cam, d, attached, if (real || mode in 1..3) prefs.pad * d else 0f, real)
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
        geo = buildGeo()
        if (preview) {
            previewUntil = SystemClock.uptimeMillis() + 2500
            h.removeCallbacks(renderR); h.postDelayed(renderR, 2600)
        }
        render()
    }

    // ------------------------------------------------------------ события

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

    fun onCall(c: CallItem) {
        if (!prefs.calls) return
        val wasRinging = call?.ringing == true
        call = c
        userExpanded = c.ringing
        if (c.ringing && !wasRinging) buzz()
        render(); armTimers()
    }

    fun onCallRemoved(id: String) {
        if (call?.id == id) { call = null; userExpanded = false; render(); armTimers() }
    }

    fun onMedia(m: MediaItem?) {
        media = if (prefs.media) m else null
        if (media == null && call == null) userExpanded = false
        render()
    }

    private fun enqueue(i: Item) {
        if (current?.id == i.id) { current = i; render(); armTimers(); return }
        queue.removeAll { it.id == i.id }
        queue.addLast(i)
        while (queue.size > 4) queue.removeFirst()
        if (current == null) advance()
    }

    private fun advance() {
        current = queue.removeFirstOrNull()
        if (current != null) buzz()
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
        if (c != null) h.postDelayed(dismissR, if (c is ChargeItem) 3200L else 5200L)
        else if (userExpanded && call?.ringing != true) h.postDelayed(collapseR, 6500)
    }

    // ------------------------------------------------------------ жесты

    fun onTouch(down: Boolean) { touching = down; armTimers() }

    fun onOutside() {
        if (current != null) dismissCurrent()
        else if (userExpanded && call?.ringing != true) { userExpanded = false; render(); armTimers() }
    }

    fun onTap() {
        val c = current
        if (c is NotifItem) { c.open?.invoke(); dismissCurrent(); return }
        if (c != null) { dismissCurrent(); return }
        val p: Item = call ?: media ?: return
        if (userExpanded || call?.ringing == true) {
            when (p) { is MediaItem -> p.open?.invoke(); is CallItem -> p.open?.invoke(); else -> {} }
            if (call?.ringing != true) { userExpanded = false; render(); armTimers() }
        } else { userExpanded = true; buzz(); render(); armTimers() }
    }

    fun onLongPress() {
        if (current != null) { dismissCurrent(); return }
        if (call == null && media == null) return
        if (call?.ringing == true) return
        userExpanded = !userExpanded; buzz(); render(); armTimers()
    }

    fun onSwipeUp() {
        if (current != null) dismissCurrent()
        else if (userExpanded && call?.ringing != true) { userExpanded = false; render(); armTimers() }
    }

    // ------------------------------------------------------------ рендер

    fun render() {
        val g = geo ?: return
        val persistent: Item? = call ?: media
        val cur = current
        val spec = when {
            landscape -> hiddenSpec(g)
            cur != null -> expandedSpec(g, cur)
            persistent != null && (userExpanded || call?.ringing == true) -> expandedSpec(g, persistent)
            persistent != null -> compactSpec(g, persistent)
            prefs.idleAlways || SystemClock.uptimeMillis() < previewUntil -> idleSpec(g)
            else -> hiddenSpec(g)
        }
        present(spec)
    }

    private fun hiddenSpec(g: Geometry) = Spec("hidden", g.camRect(), g.camRadius(), false, g.camRect(), false)

    private fun idleSpec(g: Geometry): Spec {
        val r = g.idle()
        return Spec("idle", r, g.pillRadius(r), true, g.camRect(), false)
    }

    private fun compactSpec(g: Geometry, p: Item): Spec {
        val c = g.compact(24 * d, if (p is CallItem) 46 * d else 24 * d)
        return when (p) {
            is MediaItem -> Spec("media:c", c.rect, c.radius, true, g.camRect(), true,
                { MediaCompactView(ctx, c, p) }, { (it as Bindable).bind(p) })
            is CallItem -> Spec("call:c:${p.id}", c.rect, c.radius, true, g.camRect(), true,
                { CallCompactView(ctx, c, p) }, { (it as Bindable).bind(p) })
            else -> idleSpec(g)
        }
    }

    private fun expandedSpec(g: Geometry, p: Item): Spec {
        val maxW = 400 * d
        return when (p) {
            is NotifItem -> {
                val e = g.expanded(48 * d, maxW)
                Spec("n:${p.id}:${(p.title.toString() + p.text).hashCode()}", e.rect, e.radius, true, g.camRect(), true,
                    { NotifView(ctx, e, p) })
            }
            is ChargeItem -> {
                val e = g.expanded(36 * d, maxW)
                Spec("charge:${p.kind}:${p.level}", e.rect, e.radius, true, g.camRect(), true, { ChargeView(ctx, e, p) })
            }
            is MediaItem -> {
                val e = g.expanded(152 * d, maxW)
                Spec("media:e", e.rect, e.radius, true, g.camRect(), true,
                    { MediaExpandedView(ctx, e, p) }, { (it as Bindable).bind(p) })
            }
            is CallItem -> {
                val e = g.expanded((if (p.ringing) 106 else 48 + if (p.decline != null) 56 else 0) * d, maxW)
                Spec("call:e:${p.id}:${p.ringing}", e.rect, e.radius, true, g.camRect(), true,
                    { CallExpandedView(ctx, e, p) })
            }
        }
    }

    private fun present(s: Spec) {
        lastSpec = s
        val u = RectF(s.rect)
        if (islandAttached) u.union(island.bounds())
        if (s.visible) u.union(s.origin)
        u.inset(-26 * d, -26 * d)
        setWindow(u, s.touchable)
        island.present(s)
    }

    /** Когда пружины успокоились — ужимаем окно до размера острова, чтобы не перехватывать касания. */
    fun onSettled() {
        val s = lastSpec ?: return
        val r = RectF(s.rect)
        if (s.visible) r.inset(-4 * d, -4 * d)
        setWindow(r, s.touchable)
    }

    private fun setWindow(r: RectF, touchable: Boolean) {
        val sw = screenW.toFloat()
        val l = max(0f, r.left); val t = max(0f, r.top)
        val rr = min(sw, r.right); val b = max(t + 2f, r.bottom)
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
            vib?.vibrate(
                if (Build.VERSION.SDK_INT >= 29) VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                else VibrationEffect.createOneShot(10, 120),
            )
        } catch (_: Exception) {}
    }

    // ------------------------------------------------------------ демо

    fun demo(kind: String) {
        when (kind) {
            "notif" -> showNotification(NotifItem(
                "demo:" + SystemClock.uptimeMillis(), "Сообщения", ctx.packageManager.getApplicationIcon(ctx.packageName),
                letterAvatar(ctx, "А", 0xFF0A84FF.toInt()), "Алекс", "Ты уже видел новый Dynamic Island? Выглядит огонь 🔥", null))
            "charge" -> enqueue(ChargeItem(batteryLevel().coerceIn(1, 100), 0))
            "low" -> enqueue(ChargeItem(14, 1))
            "music" -> {
                val m = MediaItem().apply {
                    app = "Музыка"; title = "Blinding Lights"; artist = "The Weeknd"
                    art = coverArt(ctx, 0xFFFF2D55.toInt(), 0xFF3A0CA3.toInt()); color = 0xFFFF375F.toInt()
                    playing = true; durMs = 200_000; posMs = 42_000; stamp = SystemClock.elapsedRealtime()
                }
                m.onToggle = { }
                onMedia(m)
                h.removeCallbacks(clearDemoMedia); h.postDelayed(clearDemoMedia, 45_000)
            }
            "call" -> startDemoCall()
        }
    }

    private val clearDemoMedia = Runnable { if (media?.app == "Музыка") onMedia(null) }

    private fun startDemoCall() {
        val avatar = letterAvatar(ctx, "М", 0xFFFF9F0A.toInt())
        lateinit var ring: CallItem
        val end = Act("Сбросить") { onCallRemoved("demo-call") }
        ring = CallItem("demo-call", "Мама", null, avatar, true, System.currentTimeMillis(),
            Act("Принять") {
                onCall(CallItem("demo-call", "Мама", null, avatar, false, System.currentTimeMillis(), null, end, null))
            }, end, null)
        onCall(ring)
    }
}
