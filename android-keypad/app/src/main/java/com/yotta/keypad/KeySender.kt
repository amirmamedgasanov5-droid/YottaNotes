package com.yotta.keypad

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Android не разрешает обычным приложениям слать нажатия клавиш в чужие приложения.
 * Это делается через Shizuku: команда `input` запускается с правами shell.
 */
object KeySender {
    fun isReady(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) { false }

    fun status(): String = try {
        when {
            !Shizuku.pingBinder() -> "Shizuku не запущен"
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> "Нет доступа Shizuku"
            else -> "Shizuku готов"
        }
    } catch (e: Throwable) { "Shizuku не установлен" }

    /** mods: например ["KEYCODE_CTRL_LEFT"], key: например "KEYCODE_Z" */
    fun send(mods: List<String>, key: String, onError: (String) -> Unit) {
        Thread {
            try {
                val cmd = if (mods.isEmpty()) arrayOf("input", "keyevent", key)
                else arrayOf("input", "keycombination", *mods.toTypedArray(), key)
                val m = Shizuku::class.java.getDeclaredMethod(
                    "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                m.isAccessible = true
                val p = m.invoke(null, cmd, null, null) as Process
                val code = p.waitFor()
                if (code != 0) onError("Ошибка input: код $code")
            } catch (e: Throwable) {
                onError("Не удалось отправить клавишу: ${e.javaClass.simpleName}")
            }
        }.start()
    }
}
