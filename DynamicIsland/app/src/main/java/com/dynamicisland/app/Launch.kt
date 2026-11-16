package com.dynamicisland.app

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object Launch {
    /** Отправляет PendingIntent с разрешением фонового запуска (Android 14+); если нет — открывает приложение по пакету. */
    fun send(ctx: Context, pi: PendingIntent?, pkg: String?) {
        if (pi != null) {
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    val o = ActivityOptions.makeBasic()
                    o.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    pi.send(ctx, 0, null, null, null, null, o.toBundle())
                } else pi.send()
                return
            } catch (_: Exception) {}
        }
        if (pkg != null) {
            try {
                ctx.packageManager.getLaunchIntentForPackage(pkg)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(it)
                }
            } catch (_: Exception) {}
        }
    }
}
