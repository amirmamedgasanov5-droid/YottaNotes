package com.dynamicisland.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.provider.Settings

class IslandService : Service() {
    private var controller: IslandController? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("island", "Dynamic Island", NotificationManager.IMPORTANCE_MIN))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "island")
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Dynamic Island работает")
            .setContentText("Нажмите, чтобы открыть настройки")
            .setContentIntent(open).setOngoing(true).build()
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(1, n)
        } catch (_: Exception) { stopSelf(); return }

        if (IslandAccessibility.instance != null || !Settings.canDrawOverlays(this)) { stopSelf(); return }
        controller = IslandController(applicationContext).also { it.start() }
    }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onConfigurationChanged(c: Configuration) {
        super.onConfigurationChanged(c)
        controller?.onConfig()
    }

    override fun onDestroy() {
        controller?.stop(); controller = null
        super.onDestroy()
    }
}
