package com.dynamicisland.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (Prefs(c).enabled && Settings.canDrawOverlays(c)) {
            try { c.startForegroundService(Intent(c, IslandService::class.java)) } catch (_: Exception) {}
        }
    }
}
