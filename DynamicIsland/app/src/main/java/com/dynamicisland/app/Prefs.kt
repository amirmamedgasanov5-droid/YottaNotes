package com.dynamicisland.app

import android.content.Context

class Prefs(c: Context) {
    private val sp = c.applicationContext.getSharedPreferences("island", Context.MODE_PRIVATE)

    private fun b(k: String, d: Boolean) = sp.getBoolean(k, d)
    private fun putB(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
    private fun i(k: String, d: Int) = sp.getInt(k, d)
    private fun putI(k: String, v: Int) = sp.edit().putInt(k, v).apply()

    var enabled: Boolean get() = b("enabled", false); set(v) = putB("enabled", v)
    var onboarded: Boolean get() = b("onboarded", false); set(v) = putB("onboarded", v)
    var idleAlways: Boolean get() = b("idle", false); set(v) = putB("idle", v)
    var haptics: Boolean get() = b("haptics", true); set(v) = putB("haptics", v)
    var notifications: Boolean get() = b("notif", true); set(v) = putB("notif", v)
    var media: Boolean get() = b("media", true); set(v) = putB("media", v)
    var charging: Boolean get() = b("charging", true); set(v) = putB("charging", v)
    var system: Boolean get() = b("system", true); set(v) = putB("system", v)
    var timers: Boolean get() = b("timers", true); set(v) = putB("timers", v)
    var outline: Boolean get() = b("outline", true); set(v) = putB("outline", v)
    var calls: Boolean get() = b("calls", true); set(v) = putB("calls", v)

    /** 0 — авто (искать камеру), 1 — слева, 2 — по центру, 3 — справа */
    var posMode: Int get() = i("pos", 0); set(v) = putI("pos", v)
    var offX: Int get() = i("offx", 0); set(v) = putI("offx", v)
    var offY: Int get() = i("offy", 0); set(v) = putI("offy", v)
    var pad: Int get() = i("pad", 2); set(v) = putI("pad", v)

    /** Ручная калибровка камеры: [cx‰ от ширины, top×10 dp, w×10 dp, h×10 dp], отдельно для портрета и ландшафта. */
    fun cal(land: Boolean): IntArray? {
        val k = if (land) "l" else "p"
        if (!sp.contains("cal_cx_$k")) return null
        return intArrayOf(sp.getInt("cal_cx_$k", 500), sp.getInt("cal_t_$k", 0), sp.getInt("cal_w_$k", 200), sp.getInt("cal_h_$k", 200))
    }

    fun setCal(land: Boolean, cx: Int, t: Int, w: Int, h: Int) {
        val k = if (land) "l" else "p"
        sp.edit().putInt("cal_cx_$k", cx).putInt("cal_t_$k", t).putInt("cal_w_$k", w).putInt("cal_h_$k", h).apply()
    }
}
