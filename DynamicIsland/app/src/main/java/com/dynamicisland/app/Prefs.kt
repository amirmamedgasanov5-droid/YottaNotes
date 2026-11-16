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
    var calls: Boolean get() = b("calls", true); set(v) = putB("calls", v)

    /** 0 — авто (искать камеру), 1 — слева, 2 — по центру, 3 — справа */
    var posMode: Int get() = i("pos", 0); set(v) = putI("pos", v)
    var offX: Int get() = i("offx", 0); set(v) = putI("offx", v)
    var offY: Int get() = i("offy", 0); set(v) = putI("offy", v)
    var pad: Int get() = i("pad", 2); set(v) = putI("pad", v)
}
