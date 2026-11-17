package com.dynamicisland.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Окно специальных возможностей лежит выше статус-бара: остров рисуется поверх часов и значков
 * и получает нажатия — как настоящий. Содержимое экрана приложение не читает.
 */
class IslandAccessibility : AccessibilityService() {
    companion object {
        @Volatile var instance: IslandAccessibility? = null
    }

    private var controller: IslandController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        // обычный оверлей больше не нужен
        if (IslandBus.controller != null) stopService(Intent(this, IslandService::class.java))
        Prefs(this).enabled = true
        controller = IslandController(this, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY).also { it.start() }
        WatchdogJob.schedule(this)
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onConfigurationChanged(c: Configuration) {
        super.onConfigurationChanged(c)
        controller?.onConfig()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        controller?.stop(); controller = null
        if (instance === this) instance = null
        // откат на обычный оверлей, если он разрешён
        val p = Prefs(this)
        if (p.enabled && Settings.canDrawOverlays(this)) {
            try { startForegroundService(Intent(this, IslandService::class.java)) } catch (_: Exception) {}
        }
    }
}
