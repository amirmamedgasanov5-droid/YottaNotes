package com.dynamicisland.app

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Следит за активными медиасессиями (нужен доступ к уведомлениям). */
class MediaWatcher(private val ctx: Context, private val onItem: (MediaItem?) -> Unit) {
    private val msm = ctx.getSystemService(MediaSessionManager::class.java)
    private val comp = ComponentName(ctx, NotifListener::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var controllers: List<MediaController> = emptyList()
    private var started = false
    private var shownToken: MediaSession.Token? = null
    private var artCache: Pair<Bitmap, Int>? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { setControllers(it ?: emptyList()) }
    private val cb = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = evaluate()
        override fun onMetadataChanged(metadata: MediaMetadata?) = evaluate()
        override fun onSessionDestroyed() = evaluate()
    }
    private val dropR = Runnable { shownToken = null; onItem(null) }

    fun start() {
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, comp, handler)
            started = true
            setControllers(msm.getActiveSessions(comp))
        } catch (_: SecurityException) { started = false }
    }

    fun retry() { if (!started) start() }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        controllers.forEach { it.unregisterCallback(cb) }
        controllers = emptyList()
        if (started) try { msm.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) {}
        started = false
    }

    private fun setControllers(list: List<MediaController>) {
        controllers.forEach { it.unregisterCallback(cb) }
        controllers = list
        controllers.forEach { it.registerCallback(cb, handler) }
        evaluate()
    }

    private fun evaluate() {
        handler.removeCallbacks(dropR)
        val playing = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING && it.metadata != null }
        val paused = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PAUSED && it.metadata != null && it.sessionToken == shownToken
        }
        val c = playing ?: paused
        if (c == null) { shownToken = null; onItem(null); return }
        shownToken = c.sessionToken
        onItem(build(c))
        if (c !== playing) handler.postDelayed(dropR, 12_000)
    }

    private fun build(c: MediaController): MediaItem {
        val md = c.metadata!!
        val st = c.playbackState
        return MediaItem().apply {
            app = try { ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(c.packageName, 0)) } catch (_: Exception) { "Музыка" }
            title = md.getString(MediaMetadata.METADATA_KEY_TITLE) ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: ""
            artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
            art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            val a = art
            color = if (a == null) 0xFFFF375F.toInt() else {
                val cached = artCache
                if (cached != null && cached.first === a) cached.second else vividColor(a).also { artCache = a to it }
            }
            durMs = md.getLong(MediaMetadata.METADATA_KEY_DURATION)
            playing = st?.state == PlaybackState.STATE_PLAYING
            posMs = st?.position?.coerceAtLeast(0) ?: 0
            stamp = st?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime()
            speed = st?.playbackSpeed ?: 1f
            onPrev = { c.transportControls.skipToPrevious() }
            onNext = { c.transportControls.skipToNext() }
            onToggle = { if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play() }
            open = { try { c.sessionActivity?.send() } catch (_: Exception) {} }
        }
    }
}
