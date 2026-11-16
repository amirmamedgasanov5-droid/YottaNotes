package com.dynamicisland.app

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifListener : NotificationListenerService() {

    override fun onListenerConnected() {
        val p = Prefs(this)
        if (p.enabled && Settings.canDrawOverlays(this) && !IslandBus.running) {
            try { startForegroundService(Intent(this, IslandService::class.java)) } catch (_: Exception) {}
        }
        IslandBus.post { refreshMedia() }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!IslandBus.running) return
        try { handle(sbn) } catch (_: Exception) {}
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val id = sbn.key
        IslandBus.post { notificationRemoved(id); onCallRemoved(id) }
    }

    private fun handle(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val n = sbn.notification
        val ex = n.extras
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val title = ex.getCharSequence(Notification.EXTRA_TITLE) ?: ""
        val text = ex.getCharSequence(Notification.EXTRA_TEXT) ?: ex.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: ""
        val appName: CharSequence = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0))
        } catch (_: Exception) { sbn.packageName }
        val appIcon: Drawable? = try { packageManager.getApplicationIcon(sbn.packageName) } catch (_: Exception) {
            try { n.smallIcon?.loadDrawable(this) } catch (_: Exception) { null }
        }
        val avatar: Drawable? = try { n.getLargeIcon()?.loadDrawable(this) } catch (_: Exception) { null }
        val open: (() -> Unit)? = n.contentIntent?.let { pi -> { send(pi) } }

        // ---- звонки
        val callType = ex.getInt("android.callType", 0) // 1 входящий, 2 исходящий/идёт, 3 скрининг
        val chrono = ex.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
        if (n.category == Notification.CATEGORY_CALL && (callType != 0 || chrono || n.fullScreenIntent != null)) {
            val ringing = callType == 1 || (callType == 0 && !chrono)
            val acts = n.actions?.filter { it?.actionIntent != null } ?: emptyList()
            val declineRe = Regex("decl|reject|hang|end|dismiss|отклон|сброс|заверш|откл", RegexOption.IGNORE_CASE)
            val decline = acts.firstOrNull { declineRe.containsMatchIn(it.title) } ?: if (acts.size > 1) acts.first() else null
            val answer = acts.firstOrNull { it !== decline }
            fun act(a: Notification.Action?) = a?.let { x -> Act(x.title) { send(x.actionIntent) } }
            val item = CallItem(sbn.key, title.ifEmpty { appName }, appIcon, avatar, ringing,
                if (n.`when` > 0) n.`when` else System.currentTimeMillis(), act(answer), act(decline), open)
            IslandBus.post { onCall(item) }
            return
        }

        // ---- обычные уведомления
        if (n.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE) != 0) return
        if (ex.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true) return
        if (ex.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0) return
        if (title.isEmpty() && text.isEmpty()) return
        val r = Ranking()
        if (currentRanking.getRanking(sbn.key, r) && r.importance < android.app.NotificationManager.IMPORTANCE_DEFAULT) return

        val item = NotifItem(sbn.key, appName, appIcon, avatar, title.ifEmpty { appName }, text, open)
        IslandBus.post { showNotification(item) }
    }

    private fun send(pi: PendingIntent) {
        try { pi.send() } catch (_: Exception) {}
    }
}
