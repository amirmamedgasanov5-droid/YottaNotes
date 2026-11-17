package app.tvdesk

import android.content.Context
import android.content.SharedPreferences

/** Все пользовательские настройки в одном месте. */
class Prefs(ctx: Context) {
    private val sp: SharedPreferences =
        ctx.applicationContext.getSharedPreferences("tvdesk", Context.MODE_PRIVATE)

    var pointerSpeed: Float
        get() = sp.getFloat("speed", 1.5f)
        set(v) = sp.edit().putFloat("speed", v).apply()
    var scrollSpeed: Float
        get() = sp.getFloat("scroll", 1.0f)
        set(v) = sp.edit().putFloat("scroll", v).apply()
    var panelScale: Float
        get() = sp.getFloat("scale", 1.0f)
        set(v) = sp.edit().putFloat("scale", v).apply()
    var naturalScroll: Boolean
        get() = sp.getBoolean("natural", false)
        set(v) = sp.edit().putBoolean("natural", v).apply()
    var keySound: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) = sp.edit().putBoolean("sound", v).apply()
    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit().putBoolean("haptics", v).apply()
    /** true — картинка на ТВ заполняет экран (с обрезкой краёв), false — вписывается с чёрными полосами. */
    var fillTv: Boolean
        get() = sp.getBoolean("fill", false)
        set(v) = sp.edit().putBoolean("fill", v).apply()
    var dockTop: Boolean
        get() = sp.getBoolean("dockTop", false)
        set(v) = sp.edit().putBoolean("dockTop", v).apply()
    /** 0 — English, 1 — Русский */
    var lang: Int
        get() = sp.getInt("lang", 0)
        set(v) = sp.edit().putInt("lang", v).apply()
}
