package com.yotta.keypad

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = resources.displayMetrics.density
        val p = (20 * d).toInt()
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(p, p, p, p) }
        fun text(s: String, size: Float = 15f) = TextView(this).apply { text = s; textSize = size; setPadding(0, (8 * d).toInt(), 0, (8 * d).toInt()) }
        fun button(s: String, f: () -> Unit) = Button(this).apply { text = s; isAllCaps = false; setOnClickListener { f() } }

        col.addView(text("Yotta Keypad", 24f))
        col.addView(text("Плавающая панель поверх любых приложений (например, Figma в браузере): Ctrl/Alt/Shift, горячие клавиши и тачпад."))
        col.addView(text("Шаг 1. Разрешите показ поверх других приложений"))
        col.addView(button("Разрешить поверх приложений") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        col.addView(text("Шаг 2. Включите службу «Yotta Keypad» в Специальных возможностях (для курсора, кликов и прокрутки)"))
        col.addView(button("Открыть спец. возможности") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        col.addView(text("Шаг 3. Для горячих клавиш нужен Shizuku (бесплатное приложение). Запустите его по инструкции, затем выдайте доступ:"))
        col.addView(button("Выдать доступ Shizuku") {
            try { if (Shizuku.pingBinder()) Shizuku.requestPermission(1) } catch (_: Throwable) {}
            refresh()
        })
        col.addView(button("Скачать Shizuku") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
        })
        status = text("")
        col.addView(status)
        col.addView(button("Включить панель") {
            if (!Settings.canDrawOverlays(this)) { refresh(); return@button }
            if (Build.VERSION.SDK_INT >= 33)
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            startForegroundService(Intent(this, OverlayService::class.java))
        })
        col.addView(button("Выключить панель") { stopService(Intent(this, OverlayService::class.java)) })

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val overlay = Settings.canDrawOverlays(this)
        val a11y = PointerService.instance != null
        status.text = "Поверх приложений: ${if (overlay) "да" else "нет"}\n" +
            "Служба курсора: ${if (a11y) "включена" else "выключена"}\n" +
            "Клавиши: ${KeySender.status()}"
    }
}
