package app.tvdesk

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var root: LinearLayout
    private val listener: () -> Unit = { runOnUiThread { build() } }
    private val REQ_CAPTURE = 42
    private val REQ_NOTIF = 43

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = Prefs(this)
        window.statusBarColor = 0xFF0B0F14.toInt()
        window.navigationBarColor = 0xFF0B0F14.toInt()
        val sv = ScrollView(this).apply { setBackgroundColor(0xFF0B0F14.toInt()) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(40))
        }
        sv.addView(root)
        setContentView(sv)
    }

    override fun onResume() {
        super.onResume()
        AppState.addListener(listener)
        build()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
    }

    override fun onPause() {
        AppState.removeListener(listener)
        super.onPause()
    }

    // ---------- проверки ----------

    private fun overlayOk() = Settings.canDrawOverlays(this)
    private fun a11yOk() = AppState.service != null
    private fun imeEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == packageName }
    }
    private fun imeSelected(): Boolean {
        val cur = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        return cur.startsWith("$packageName/")
    }

    // ---------- интерфейс ----------

    private fun text(t: String, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = t; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = GradientDrawable().apply { setColor(0xFF151B23.toInt()); cornerRadius = dp(14).toFloat() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
    }

    private fun button(t: String, primary: Boolean = false, onClick: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            setColor(if (primary) 0xFF2E6BD6.toInt() else 0xFF2B323C.toInt()); cornerRadius = dp(10).toFloat()
        }
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) }
    }

    private fun step(ok: Boolean, title: String, hint: String, vararg actions: Pair<String, () -> Unit>): LinearLayout {
        val c = card()
        c.addView(text((if (ok) "✓  " else "○  ") + title, 16f, if (ok) 0xFF6EE7A0.toInt() else Color.WHITE, true))
        if (!ok || hint.isNotEmpty()) c.addView(text(hint, 13f, 0xFF9AA7B5.toInt()).apply { setPadding(0, dp(4), 0, 0) })
        if (!ok) for ((label, act) in actions) c.addView(button(label) { act() })
        return c
    }

    private fun build() {
        root.removeAllViews()
        root.addView(text("TV Desk", 28f, Color.WHITE, true))
        root.addView(text("Планшет → ТВ по USB-C + клавиатура и тачпад как у ноутбука. ТВ показывает только экран планшета, без клавиатуры.", 14f, 0xFF9AA7B5.toInt()).apply { setPadding(0, dp(6), 0, dp(4)) })

        // --- шаги настройки ---
        root.addView(text("Настройка (один раз)", 18f, Color.WHITE, true).apply { setPadding(0, dp(18), 0, 0) })
        root.addView(step(overlayOk(), "Показ поверх других окон", "Нужно для панели, курсора и окна на ТВ.",
            "Разрешить" to { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }))
        root.addView(step(a11yOk(), "Служба доступности «TV Desk»",
            "Включите её в списке. Если пункт серый (Android 13+ для APK не из магазина): «Данные приложения» → ⋮ → «Разрешить ограниченные настройки», потом снова включите.",
            "Открыть настройки доступности" to { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            "Данные приложения (ограниченные настройки)" to { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }))
        val imeOk = imeEnabled() && imeSelected()
        root.addView(step(imeOk, "Клавиатура «TV Desk»",
            "Через неё клавиши и Ctrl-сочетания попадают в приложения. Включите её и выберите текущей.",
            "1. Включить клавиатуру в списке" to { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
            "2. Выбрать TV Desk текущей" to { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }))

        // --- действия ---
        root.addView(text("Управление", 18f, Color.WHITE, true).apply { setPadding(0, dp(22), 0, 0) })
        val act = card()
        val shown = AppState.service?.isActive() == true
        act.addView(text(if (shown) "Панель клавиатуры и тачпада: включена" else "Панель клавиатуры и тачпада: выключена", 15f, Color.WHITE, true))
        act.addView(button(if (shown) "Выключить панель" else "Включить панель", true) {
            val s = AppState.service
            if (s == null) Toast.makeText(this, "Сначала включите службу доступности", Toast.LENGTH_LONG).show()
            else if (shown) { s.close() } else { s.showPanel(); moveTaskToBack(true) }
            build()
        })
        root.addView(act)

        val tv = card()
        tv.addView(text(
            when (AppState.mirror) {
                AppState.Mirror.OFF -> "Вывод на ТВ: выключен"
                AppState.Mirror.WAITING_TV -> "Вывод на ТВ: ждёт подключения ТВ"
                AppState.Mirror.STREAMING -> "Вывод на ТВ: идёт ▶"
            }, 15f, Color.WHITE, true))
        if (AppState.mirrorNote.isNotEmpty()) tv.addView(text(AppState.mirrorNote, 13f, 0xFF9AA7B5.toInt()).apply { setPadding(0, dp(4), 0, 0) })
        if (AppState.mirror == AppState.Mirror.OFF) {
            tv.addView(button("Вывести экран на ТВ", true) { startMirror() })
            tv.addView(text("Сначала подключите ТВ кабелем USB-C → HDMI/DisplayPort. В окне Android выберите «Весь экран».", 12f, 0xFF7C8996.toInt()).apply { setPadding(0, dp(6), 0, 0) })
        } else {
            tv.addView(button("Остановить вывод") {
                startService(Intent(this, MirrorService::class.java).setAction(MirrorService.ACTION_STOP))
            })
        }
        root.addView(tv)

        // --- настройки ---
        root.addView(text("Настройки", 18f, Color.WHITE, true).apply { setPadding(0, dp(22), 0, 0) })
        val st = card()
        st.addView(slider("Скорость курсора", prefs.pointerSpeed, 0.5f, 4f) { prefs.pointerSpeed = it })
        st.addView(slider("Скорость прокрутки", prefs.scrollSpeed, 0.3f, 3f) { prefs.scrollSpeed = it })
        st.addView(slider("Размер клавиатуры", prefs.panelScale, 0.7f, 1.3f) { prefs.panelScale = it })
        st.addView(text("Размер применится при следующем открытии панели.", 11f, 0xFF7C8996.toInt()))
        st.addView(toggle("Звук клавиш (через динамик планшета)", prefs.keySound) { prefs.keySound = it })
        st.addView(toggle("Вибро-отклик клавиш", prefs.haptics) { prefs.haptics = it })
        st.addView(toggle("Натуральная прокрутка (как на телефоне)", prefs.naturalScroll) { prefs.naturalScroll = it })
        st.addView(toggle("ТВ: заполнять весь экран (обрезать края)", prefs.fillTv) { prefs.fillTv = it; AppState.changed() })
        st.addView(toggle("Панель сверху (а не снизу)", prefs.dockTop) { prefs.dockTop = it })
        root.addView(st)
    }

    private fun slider(title: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit): LinearLayout {
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        l.addView(text(title, 14f, Color.WHITE))
        l.addView(SeekBar(this).apply {
            this.max = 100
            progress = ((value - min) / (max - min) * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) { if (fromUser) onChange(min + (max - min) * p / 100f) }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        return l
    }

    private fun toggle(title: String, value: Boolean, onChange: (Boolean) -> Unit) = Switch(this).apply {
        text = title; setTextColor(Color.WHITE); isChecked = value
        setPadding(0, dp(8), 0, dp(8))
        setOnCheckedChangeListener { _, c -> onChange(c) }
    }

    // ---------- вывод на ТВ ----------

    private fun startMirror() {
        if (!overlayOk()) {
            Toast.makeText(this, "Сначала разрешите показ поверх других окон", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = if (Build.VERSION.SDK_INT >= 34)
            mpm.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        else mpm.createScreenCaptureIntent()
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQ_CAPTURE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                startForegroundService(
                    Intent(this, MirrorService::class.java).setAction(MirrorService.ACTION_START)
                        .putExtra(MirrorService.EXTRA_CODE, resultCode).putExtra(MirrorService.EXTRA_DATA, data)
                )
            } else Toast.makeText(this, "Захват экрана не разрешён", Toast.LENGTH_SHORT).show()
        }
    }
}
