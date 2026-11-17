package app.tvdesk

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Короткий «клик» клавиши. Играет через динамик планшета, а не через звук ТВ (HDMI). */
object KeyClick {
    private var track: AudioTrack? = null
    private var failed = false

    private fun create(am: AudioManager): AudioTrack? {
        val rate = 44100
        val n = rate * 22 / 1000
        val pcm = ShortArray(n) { i ->
            val t = i.toDouble() / rate
            val env = exp(-t * 220.0)
            (sin(2 * PI * 1900 * t) * env * 0.55 * Short.MAX_VALUE).toInt().toShort()
        }
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        t.write(pcm, 0, pcm.size)
        am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            ?.let { t.preferredDevice = it }
        return t
    }

    fun play(am: AudioManager) {
        if (failed) return
        try {
            val t = track ?: create(am).also { track = it } ?: return
            t.stop()
            t.reloadStaticData()
            t.play()
        } catch (e: Exception) {
            failed = true
        }
    }
}
