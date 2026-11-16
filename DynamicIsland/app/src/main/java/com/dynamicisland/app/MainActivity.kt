package com.dynamicisland.app

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var phone: PhoneView
    private lateinit var camText: TextView
    private lateinit var mainBtn: TextView
    private lateinit var rowOverlay: PermRow
    private lateinit var rowListener: PermRow
    private lateinit var rowPost: PermRow
    private lateinit var rowA11y: PermRow
    private val modeChips = ArrayList<TextView>()
    private val bg = 0xFF000000.toInt()
    private val card = 0xFF1C1C1E.toInt()
    private val blue = 0xFF0A84FF.toInt()

    inner class PermRow(val title: String, val desc: String, val onClick: () -> Unit) {
        val btn = TextView(this@MainActivity)
        val root = LinearLayout(this@MainActivity)
        fun set(granted: Boolean) {
            btn.text = if (granted) "✓ Готово" else "Разрешить"
            btn.setTextColor(if (granted) GREEN else WHITE)
            btn.background = GradientDrawable().apply {
                setColor(if (granted) 0x2230D158 else blue); cornerRadius = dp(16f)
            }
            btn.isEnabled = !granted
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        prefs = Prefs(this)
        window.statusBarColor = bg; window.navigationBarColor = bg
        buildUi()
        window.decorView.setOnApplyWindowInsetsListener { v, ins ->
            val cut = if (Build.VERSION.SDK_INT >= 28) ins.displayCutout?.boundingRects?.minByOrNull { it.top } else null
            phone.setCutout(cut?.let { RectF(it) }, v.width)
            v.onApplyWindowInsets(ins)
        }
        if (!prefs.onboarded) {
            prefs.onboarded = true
            requestRuntimePerms()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        // сервис мог быть включён ранее — поднимем его, если все разрешения на месте
        if (prefs.enabled && canRun() && !IslandBus.running) startIsland()
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) { refresh() }

    // ------------------------------------------------------------ состояние

    private fun requestRuntimePerms() {
        val need = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) need += Manifest.permission.POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) need += Manifest.permission.BLUETOOTH_CONNECT
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), 1)
    }

    private fun hasA11y() = IslandAccessibility.instance != null ||
        (Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "").contains(packageName)

    /** Хватает ли способа рисовать остров: специальные возможности (лучше) или «поверх окон». */
    private fun canRun() = hasA11y() || hasOverlay()

    private fun hasOverlay() = Settings.canDrawOverlays(this)
    private fun hasListener() =
        (Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: "").contains(packageName)
    private fun hasPost() = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun refresh() {
        rowOverlay.set(hasOverlay()); rowListener.set(hasListener()); rowPost.set(hasPost()); rowA11y.set(hasA11y())
        val on = prefs.enabled && canRun()
        mainBtn.text = if (on) "Выключить Dynamic Island" else "Включить Dynamic Island"
        mainBtn.background = GradientDrawable().apply { setColor(if (on) 0xFF3A3A3C.toInt() else blue); cornerRadius = dp(18f) }
        camText.text = (IslandBus.controller?.describe() ?: phone.describe()) + "\n" +
            if (hasA11y()) "Режим: специальные возможности ✓ (нажатия работают)"
            else "⚠ Режим: только оверлей — нажатия на остров могут не работать. Включите «Специальные возможности»."
    }

    private fun startIsland() {
        if (IslandAccessibility.instance != null) return
        try { startForegroundService(Intent(this, IslandService::class.java)) } catch (e: Exception) {
            Toast.makeText(this, "Не удалось запустить сервис: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleMain() {
        if (prefs.enabled && canRun()) {
            prefs.enabled = false
            stopService(Intent(this, IslandService::class.java))
            IslandBus.post { render() }
        } else {
            if (!canRun()) { openA11ySettings(); return }
            prefs.enabled = true
            startIsland()
            IslandBus.post { render() }
            if (!hasListener()) Toast.makeText(this, "Дайте доступ к уведомлениям — иначе остров не увидит музыку и сообщения", Toast.LENGTH_LONG).show()
        }
        window.decorView.postDelayed({ refresh() }, 600)
    }

    private fun openA11ySettings() {
        Toast.makeText(this, "Найдите «Dynamic Island» в списке и включите", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun openListenerSettings() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(this, NotifListener::class.java).flattenToString()))
            } else startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (_: Exception) { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }

    private fun demo(kind: String) {
        if (!canRun()) { openA11ySettings(); return }
        if (!IslandBus.running) {
            prefs.enabled = true; startIsland()
            window.decorView.postDelayed({ IslandBus.post { demo(kind) }; refresh() }, 900)
        } else IslandBus.post { demo(kind) }
    }

    private fun calibrate() {
        if (!canRun()) { openA11ySettings(); return }
        if (!IslandBus.running) {
            prefs.enabled = true; startIsland()
            window.decorView.postDelayed({ IslandBus.post { startCalibration() }; refresh() }, 1000)
        } else IslandBus.post { startCalibration() }
    }

    private fun settingsChanged(preview: Boolean = true) = IslandBus.post { onSettingsChanged(preview) }

    // ------------------------------------------------------------ UI

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun dpi(v: Float) = (v * resources.displayMetrics.density + .5f).toInt()

    private fun cardView(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply { setColor(card); cornerRadius = dp(20f) }
        setPadding(dpi(16f), dpi(14f), dpi(16f), dpi(14f))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dpi(12f) }
    }

    private fun header(t: String) = tv(this, t.uppercase(), 12f, 0xFF8E8E93.toInt(), true).apply {
        setPadding(dpi(6f), dpi(22f), 0, 0)
    }

    private fun addPermRow(parent: LinearLayout, r: PermRow, last: Boolean) {
        r.root.apply {
            gravity = Gravity.CENTER_VERTICAL
            val col = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(tv(this@MainActivity, r.title, 16f, WHITE, true))
                addView(tv(this@MainActivity, r.desc, 13f, 0xFF8E8E93.toInt(), false, 3).apply { setPadding(0, dpi(2f), 0, 0) })
            }
            addView(col, LinearLayout.LayoutParams(0, -2, 1f))
            r.btn.apply {
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), 600, false)
                setPadding(dpi(14f), dpi(8f), dpi(14f), dpi(8f)); setOnClickListener { r.onClick() }
            }
            addView(r.btn, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dpi(10f) })
            setPadding(0, dpi(8f), 0, dpi(8f))
        }
        parent.addView(r.root)
        if (!last) parent.addView(View(this).apply { setBackgroundColor(0xFF38383A.toInt()) }, LinearLayout.LayoutParams(-1, 1))
    }

    private fun switchRow(parent: LinearLayout, label: String, get: Boolean, onChange: (Boolean) -> Unit) {
        val sw = Switch(this).apply { isChecked = get; setOnCheckedChangeListener { _, v -> onChange(v) } }
        parent.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(0, dpi(6f), 0, dpi(6f))
            addView(tv(this@MainActivity, label, 16f), LinearLayout.LayoutParams(0, -2, 1f)); addView(sw)
        })
    }

    private fun slider(parent: LinearLayout, label: String, min: Int, max: Int, get: Int, onChange: (Int) -> Unit) {
        val value = tv(this, "$get", 14f, 0xFF8E8E93.toInt())
        val sb = SeekBar(this).apply {
            this.max = max - min; progress = get - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) { value.text = "${p + min}"; if (user) onChange(p + min) }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        parent.addView(LinearLayout(this).apply {
            setPadding(0, dpi(8f), 0, 0)
            addView(tv(this@MainActivity, label, 15f), LinearLayout.LayoutParams(0, -2, 1f)); addView(value)
        })
        parent.addView(sb)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpi(16f), dpi(8f), dpi(16f), dpi(40f))
        }
        root.addView(tv(this, "Dynamic Island", 32f, WHITE, true).apply { setPadding(dpi(6f), dpi(6f), 0, 0) })
        root.addView(tv(this, "Остров сам находит камеру и растёт прямо из неё", 15f, 0xFF8E8E93.toInt(), false, 2)
            .apply { setPadding(dpi(6f), dpi(4f), 0, 0) })

        // --- камера
        phone = PhoneView(this)
        root.addView(phone, LinearLayout.LayoutParams(-1, dpi(150f)).apply { topMargin = dpi(14f) })
        camText = tv(this, "", 14f, 0xFF8E8E93.toInt(), false, 2).apply { gravity = Gravity.CENTER; setPadding(0, dpi(8f), 0, 0) }
        root.addView(camText, LinearLayout.LayoutParams(-1, -2))

        mainBtn = tv(this, "", 17f, WHITE, true).apply {
            gravity = Gravity.CENTER; setPadding(0, dpi(16f), 0, dpi(16f))
            setOnClickListener { toggleMain() }
        }
        root.addView(mainBtn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dpi(14f) })

        // --- разрешения
        root.addView(header("Разрешения"))
        val perms = cardView()
        rowA11y = PermRow("Специальные возможности ★",
            "Рекомендуется: остров рисуется поверх часов и значков статус-бара и принимает нажатия. Содержимое экрана не читается.") { openA11ySettings() }
        rowOverlay = PermRow("Поверх других окон", "Запасной вариант, если специальные возможности не включены (нажатия в зоне статус-бара могут не работать)") { openOverlaySettings() }
        rowListener = PermRow("Доступ к уведомлениям",
            "Сообщения, звонки и музыка. Если переключатель серый: Настройки → Приложения → Dynamic Island → ⋮ → «Разрешить ограниченные настройки»") { openListenerSettings() }
        rowPost = PermRow("Уведомления и Bluetooth", "Значок службы и показ подключения наушников (Android 12–13+)") { requestRuntimePerms() }
        addPermRow(perms, rowA11y, false); addPermRow(perms, rowOverlay, false); addPermRow(perms, rowListener, false); addPermRow(perms, rowPost, true)
        root.addView(perms)

        // --- демо
        root.addView(header("Посмотреть в деле"))
        val demoCard = cardView()
        val rows = List(4) { LinearLayout(this) }
        fun chip(l: String, kind: String, p: LinearLayout) = tv(this, l, 14f, WHITE, true).apply {
            gravity = Gravity.CENTER; setPadding(0, dpi(12f), 0, dpi(12f))
            background = GradientDrawable().apply { setColor(0xFF2C2C2E.toInt()); cornerRadius = dp(14f) }
            setOnClickListener { demo(kind) }
            p.addView(this, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dpi(4f), dpi(4f), dpi(4f), dpi(4f)) })
        }
        chip("💬 Сообщение", "notif", rows[0]); chip("🎵 Музыка", "music", rows[0])
        chip("⚡ Зарядка", "charge", rows[1]); chip("📞 Звонок", "call", rows[1])
        chip("⏱ Таймер", "timer", rows[2]); chip("⏰ Будильник", "alarm", rows[2])
        chip("🔕 Без звука", "ringer", rows[3]); chip("🎧 Наушники", "bt", rows[3])
        val rows5 = LinearLayout(this)
        chip("🔓 Разблокировка", "unlock", rows5); chip("🫧 Две активности", "split", rows5)
        rows.forEach { demoCard.addView(it) }; demoCard.addView(rows5)
        root.addView(demoCard)

        // --- настройки
        root.addView(header("Поведение"))
        val st = cardView()
        switchRow(st, "Сообщения и уведомления", prefs.notifications) { prefs.notifications = it }
        switchRow(st, "Музыка", prefs.media) { prefs.media = it; IslandBus.post { if (!it) onMedia(null) else refreshMedia() } }
        switchRow(st, "Звонки", prefs.calls) { prefs.calls = it }
        switchRow(st, "Таймеры и будильники", prefs.timers) { prefs.timers = it }
        switchRow(st, "Звук, наушники, разблокировка", prefs.system) { prefs.system = it }
        switchRow(st, "Зарядка и низкий заряд", prefs.charging) { prefs.charging = it }
        switchRow(st, "Вибро-отклик", prefs.haptics) { prefs.haptics = it }
        switchRow(st, "Тонкий контур (видно на чёрном фоне)", prefs.outline) { prefs.outline = it }
        switchRow(st, "Всегда показывать капсулу", prefs.idleAlways) { prefs.idleAlways = it; settingsChanged(false) }
        root.addView(st)

        root.addView(header("Положение камеры"))
        val pos = cardView()
        val chips = LinearLayout(this)
        listOf("Авто", "Слева", "Центр", "Справа", "Вручную").forEachIndexed { i, name ->
            val t = tv(this, name, 12.5f, WHITE, true).apply {
                gravity = Gravity.CENTER; setPadding(0, dpi(10f), 0, dpi(10f))
                setOnClickListener { prefs.posMode = i; styleChips(); settingsChanged(); refreshSoon() }
            }
            modeChips.add(t)
            chips.addView(t, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dpi(3f), 0, dpi(3f), 0) })
        }
        pos.addView(chips)
        pos.addView(tv(this, "🎯 Подогнать под камеру вручную", 15f, WHITE, true).apply {
            gravity = Gravity.CENTER; setPadding(0, dpi(13f), 0, dpi(13f))
            background = GradientDrawable().apply { setColor(blue); cornerRadius = dp(14f) }
            setOnClickListener { calibrate() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dpi(10f) })
        pos.addView(tv(this, "«Авто» ищет вырез камеры через систему. Если камера на рамке (нет выреза) — рисуется виртуальный остров по центру, как на iPhone. Остальное — ручной режим.",
            12f, 0xFF8E8E93.toInt(), false, 5).apply { setPadding(0, dpi(10f), 0, 0) })
        slider(pos, "Сдвиг по горизонтали, dp", -80, 80, prefs.offX) { prefs.offX = it; settingsChanged() }
        slider(pos, "Сдвиг по вертикали, dp", -20, 60, prefs.offY) { prefs.offY = it; settingsChanged() }
        slider(pos, "Отступ вокруг камеры, dp", 0, 10, prefs.pad) { prefs.pad = it; settingsChanged() }
        root.addView(pos)
        styleChips()

        setContentView(ScrollView(this).apply { setBackgroundColor(bg); addView(root) })
    }

    private fun refreshSoon() = window.decorView.postDelayed({ refresh() }, 300)

    private fun styleChips() {
        modeChips.forEachIndexed { i, t ->
            t.background = GradientDrawable().apply {
                setColor(if (i == prefs.posMode) blue else 0xFF2C2C2E.toInt()); cornerRadius = dp(12f)
            }
        }
    }

    override fun onApplyThemeResource(theme: android.content.res.Resources.Theme?, resid: Int, first: Boolean) {
        super.onApplyThemeResource(theme, resid, first)
    }

    /** Схема верхней части телефона с найденной камерой и пульсирующим кольцом. */
    inner class PhoneView(c: android.content.Context) : View(c) {
        private var cut: RectF? = null
        private var sw = 0
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private var t = 0f

        fun setCutout(r: RectF?, w: Int) { cut = r; sw = w; invalidate(); post { camText.text = IslandBus.controller?.describe() ?: describe() } }

        fun describe(): String {
            val r = cut ?: return "Выреза нет (камера на рамке) — остров будет по центру, как на iPhone"
            val side = when { r.centerX() < sw * .36f -> "слева"; r.centerX() > sw * .64f -> "справа"; else -> "по центру" }
            return "Камера найдена: $side (${(r.width() / resources.displayMetrics.density).toInt()}×${(r.height() / resources.displayMetrics.density).toInt()} dp)"
        }

        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            val pw = w * 0.62f; val left = (w - pw) / 2
            p.style = Paint.Style.FILL; p.color = 0xFF111114.toInt()
            c.drawRoundRect(left, dp(10f), left + pw, h + dp(60f), dp(38f), dp(38f), p)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(3f); p.color = 0xFF48484A.toInt()
            c.drawRoundRect(left, dp(10f), left + pw, h + dp(60f), dp(38f), dp(38f), p)
            val scr = RectF(left + dp(8f), dp(18f), left + pw - dp(8f), h + dp(60f))
            p.style = Paint.Style.FILL; p.shader = LinearGradient(0f, 0f, 0f, h, 0xFF2B2F6B.toInt(), 0xFF8B3A8C.toInt(), Shader.TileMode.CLAMP)
            c.drawRoundRect(scr, dp(30f), dp(30f), p); p.shader = null

            val r = cut
            val rel = if (r != null && sw > 0) RectF(r.left / sw, r.top / sw, r.right / sw, r.bottom / sw) else null
            val k = scr.width()
            val cam = if (rel != null) RectF(scr.left + rel.left * k, scr.top + rel.top * k, scr.left + rel.right * k, scr.top + rel.bottom * k)
            else { val ww = k * .32f; RectF(scr.centerX() - ww / 2, scr.top + k * .028f, scr.centerX() + ww / 2, scr.top + k * .028f + ww * .294f) }
            val mn = dp(9f)
            if (cam.width() < mn) cam.inset(-(mn - cam.width()) / 2, 0f)
            if (cam.height() < mn) cam.inset(0f, -(mn - cam.height()) / 2)
            p.color = Color.BLACK
            c.drawRoundRect(cam, cam.height() / 2, cam.height() / 2, p)
            t += 0.02f
            val ph = (t % 1f)
            p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f)
            p.color = Color.argb(((1 - ph) * 200).toInt(), 10, 132, 255)
            val g = dp(2f) + ph * dp(12f)
            c.drawRoundRect(cam.left - g, cam.top - g, cam.right + g, cam.bottom + g, cam.height() / 2 + g, cam.height() / 2 + g, p)
            postInvalidateOnAnimation()
        }
    }
}
