package com.yotta.keypad

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var panel: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private val main = Handler(Looper.getMainLooper())
    private val mods = linkedSetOf<String>()
    private val modButtons = mutableMapOf<String, Button>()

    private val dp by lazy { resources.displayMetrics.density }
    private fun px(v: Int) = (v * dp).toInt()

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "stop") { stopSelf(); return START_NOT_STICKY }
        startForegroundCompat()
        if (panel == null) buildPanel()
        PointerService.instance?.showCursor()
        return START_STICKY
    }

    override fun onDestroy() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
        PointerService.instance?.hideCursor()
        running = false
        super.onDestroy()
    }

    companion object { @Volatile var running = false }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("kp", "Yotta Keypad", NotificationManager.IMPORTANCE_LOW))
        val stop = android.app.PendingIntent.getService(
            this, 0, Intent(this, OverlayService::class.java).setAction("stop"),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, "kp")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Yotta Keypad работает")
            .addAction(Notification.Action.Builder(null, "Выключить", stop).build())
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
        running = true
    }

    private fun toast(s: String) = main.post { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() }

    private fun bg(color: Int, r: Int = 8) = GradientDrawable().apply { setColor(color); cornerRadius = px(r).toFloat() }

    private fun btn(text: String, w: Int = 36, onClick: (Button) -> Unit): Button = Button(this).apply {
        this.text = text; isAllCaps = false; textSize = 12f; setTextColor(Color.WHITE)
        background = bg(0xFF3A3A3C.toInt()); minWidth = 0; minHeight = 0
        setPadding(0, 0, 0, 0)
        layoutParams = LinearLayout.LayoutParams(px(w), px(34)).apply { setMargins(px(1), px(1), px(1), px(1)) }
        setOnClickListener { onClick(this) }
    }

    private fun row(vararg v: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        v.forEach { addView(it) }
    }

    private fun press(key: String, extraMods: List<String> = emptyList()) {
        val all = (mods + extraMods).distinct()
        if (!KeySender.isReady()) { toast("${KeySender.status()} — откройте приложение Yotta Keypad"); return }
        KeySender.send(all, key) { toast(it) }
        mods.clear(); modButtons.values.forEach { it.background = bg(0xFF3A3A3C.toInt()) }
    }

    private fun buildPanel() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg(0xE61C1C1E.toInt(), 14)
            setPadding(px(6), px(6), px(6), px(6))
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // Заголовок: перетаскивание панели, свернуть, закрыть
        val handle = TextView(this).apply {
            text = "≡  Yotta Keypad (тяните)"; setTextColor(Color.LTGRAY); textSize = 12f
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, px(34), 1f)
        }
        val collapse = btn("—") { content.visibility = if (content.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        val close = btn("✕") { stopSelf() }
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(handle); addView(collapse); addView(close)
        })

        // Модификаторы (залипают до следующей клавиши)
        val modDefs = listOf("Ctrl" to "KEYCODE_CTRL_LEFT", "Alt" to "KEYCODE_ALT_LEFT",
            "Shift" to "KEYCODE_SHIFT_LEFT", "Win/⌘" to "KEYCODE_META_LEFT")
        content.addView(row(*modDefs.map { (label, code) ->
            btn(label, 64) { b ->
                if (mods.remove(code)) b.background = bg(0xFF3A3A3C.toInt())
                else { mods.add(code); b.background = bg(0xFF0A84FF.toInt()) }
            }.also { modButtons[code] = it }
        }.toTypedArray()))

        // Быстрые сочетания
        val ctrl = "KEYCODE_CTRL_LEFT"; val shift = "KEYCODE_SHIFT_LEFT"
        fun combo(label: String, key: String, vararg m: String) = btn(label, 52) { press(key, m.toList()) }
        content.addView(row(combo("Undo", "KEYCODE_Z", ctrl), combo("Redo", "KEYCODE_Z", ctrl, shift),
            combo("Copy", "KEYCODE_C", ctrl), combo("Paste", "KEYCODE_V", ctrl), combo("Dup", "KEYCODE_D", ctrl)))
        content.addView(row(combo("Group", "KEYCODE_G", ctrl), combo("Ungrp", "KEYCODE_G", ctrl, shift),
            combo("All", "KEYCODE_A", ctrl), combo("Save", "KEYCODE_S", ctrl), combo("Find", "KEYCODE_F", ctrl)))

        // Буквы A-Z
        ('A'..'Z').chunked(9).forEach { chunk ->
            content.addView(row(*chunk.map { c -> btn(c.toString()) { press("KEYCODE_$c") } }.toTypedArray()))
        }
        // Цифры и знаки
        content.addView(row(*('0'..'9').map { c -> btn(c.toString()) { press("KEYCODE_$c") } }.toTypedArray()))
        content.addView(row(
            btn("Esc", 44) { press("KEYCODE_ESCAPE") }, btn("Tab", 44) { press("KEYCODE_TAB") },
            btn("Enter", 52) { press("KEYCODE_ENTER") }, btn("Space", 56) { press("KEYCODE_SPACE") },
            btn("⌫", 40) { press("KEYCODE_DEL") }, btn("Del", 44) { press("KEYCODE_FORWARD_DEL") }))
        content.addView(row(
            btn("+", 40) { press("KEYCODE_EQUALS") }, btn("−", 40) { press("KEYCODE_MINUS") },
            btn("←", 40) { press("KEYCODE_DPAD_LEFT") }, btn("↑", 40) { press("KEYCODE_DPAD_UP") },
            btn("↓", 40) { press("KEYCODE_DPAD_DOWN") }, btn("→", 40) { press("KEYCODE_DPAD_RIGHT") }))

        // Тачпад
        val pad = TouchPad(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(150)).apply { topMargin = px(6) }
            background = bg(0xFF2C2C2E.toInt(), 10)
        }
        content.addView(pad)
        val drag = Button(this).apply {
            text = "Тянуть (держать)"; isAllCaps = false; textSize = 12f; setTextColor(Color.WHITE)
            background = bg(0xFF3A3A3C.toInt()); setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(px(120), px(36)).apply { setMargins(px(1), px(1), px(1), px(1)) }
            setOnTouchListener { v, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { PointerService.instance?.dragStart(); v.background = bg(0xFF0A84FF.toInt()) }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { PointerService.instance?.dragEnd(); v.background = bg(0xFF3A3A3C.toInt()) }
                }
                true
            }
        }
        content.addView(row(
            btn("ЛКМ", 70) { PointerService.instance?.click() },
            btn("ПКМ", 70) { PointerService.instance?.rightClick() }, drag))
        root.addView(content)

        // Перетаскивание панели за заголовок
        val lp = WindowManager.LayoutParams(
            px(340), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = px(16); y = px(60) }
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0
        handle.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = ox + (e.rawX - sx).toInt(); lp.y = oy + (e.rawY - sy).toInt()
                    wm.updateViewLayout(root, lp)
                }
            }
            true
        }
        wm.addView(root, lp)
        panel = root; params = lp
    }

    /** Тачпад: 1 палец — курсор, тап — ЛКМ, 2 пальца — прокрутка, тап двумя — ПКМ. */
    private inner class TouchPad(c: Context) : View(c) {
        private var lx = 0f; private var ly = 0f; private var have = false
        private var down = 0L; private var moved = 0f; private var maxP = 1
        override fun onTouchEvent(e: MotionEvent): Boolean {
            val ps = PointerService.instance
            val n = e.pointerCount
            var x = 0f; var y = 0f
            for (i in 0 until n) { x += e.getX(i); y += e.getY(i) }
            x /= n; y /= n
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { down = System.currentTimeMillis(); moved = 0f; maxP = 1; lx = x; ly = y; have = true }
                MotionEvent.ACTION_POINTER_DOWN -> { maxP = maxOf(maxP, n); lx = x; ly = y }
                MotionEvent.ACTION_POINTER_UP -> { have = false }
                MotionEvent.ACTION_MOVE -> {
                    if (!have) { lx = x; ly = y; have = true; return true }
                    val dx = x - lx; val dy = y - ly
                    moved += kotlin.math.abs(dx) + kotlin.math.abs(dy)
                    if (n == 1) ps?.move(dx * 2f, dy * 2f) else ps?.scroll(dy)
                    lx = x; ly = y
                }
                MotionEvent.ACTION_UP -> {
                    if (moved < 12f * dp && System.currentTimeMillis() - down < 250) {
                        if (maxP >= 2) ps?.rightClick() else ps?.click()
                    }
                }
            }
            if (ps == null && e.actionMasked == MotionEvent.ACTION_DOWN)
                toast("Включите службу «Yotta Keypad» в Специальных возможностях")
            return true
        }
    }
}
