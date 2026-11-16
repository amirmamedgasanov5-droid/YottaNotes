package com.dynamicisland.app

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object Launch {
    /**
     * Открывает приложение / выполняет действие уведомления.
     * Для действий-активностей идём через невидимую активность-прокладку (обход запрета фонового запуска),
     * для обычных broadcast/service достаточно прямой отправки.
     */
    fun send(ctx: Context, pi: PendingIntent?, pkg: String?) {
        val needActivity = pi == null || Build.VERSION.SDK_INT < 31 || pi.isActivity
        if (!needActivity) {
            if (direct(ctx, pi!!)) return
        }
        try {
            ctx.startActivity(
                Intent(ctx, LaunchActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_HISTORY)
                    .putExtra("pi", pi).putExtra("pkg", pkg),
            )
            return
        } catch (_: Exception) {}
        if (pi != null && direct(ctx, pi)) return
        if (pkg != null) {
            try {
                ctx.packageManager.getLaunchIntentForPackage(pkg)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(it)
                }
            } catch (_: Exception) {}
        }
    }

    private fun direct(ctx: Context, pi: PendingIntent): Boolean = try {
        if (Build.VERSION.SDK_INT >= 34) {
            val o = ActivityOptions.makeBasic()
            o.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            pi.send(ctx, 0, null, null, null, null, o.toBundle())
        } else pi.send()
        true
    } catch (_: Exception) { false }
}
