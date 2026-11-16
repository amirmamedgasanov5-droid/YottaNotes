package com.dynamicisland.app

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle

/**
 * Невидимая «прокладка»: из острова (фон) запускать чужие приложения нельзя, а из активности на переднем плане — можно.
 * Поэтому нажатие сначала поднимает эту активность, а она уже отправляет нужный PendingIntent и закрывается.
 */
class LaunchActivity : Activity() {
    @Suppress("DEPRECATION")
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val pi: PendingIntent? = intent.getParcelableExtra("pi")
        val pkg = intent.getStringExtra("pkg")
        var ok = false
        if (pi != null) {
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    val o = ActivityOptions.makeBasic()
                    o.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    pi.send(this, 0, null, null, null, null, o.toBundle())
                } else pi.send()
                ok = true
            } catch (_: Exception) {}
        }
        if (!ok && pkg != null) {
            try {
                packageManager.getLaunchIntentForPackage(pkg)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(it)
                }
            } catch (_: Exception) {}
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
