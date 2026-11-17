package app.tvdesk

import android.os.SystemClock
import android.view.KeyEvent

/**
 * Отправка нажатий в приложение, у которого сейчас в фокусе поле ввода.
 * Идёт через нашу клавиатуру (InputMethodService) — это единственный способ для обычного
 * приложения без root доставлять настоящие KeyEvent с Ctrl/Alt/Shift в чужие окна.
 */
object KeyOutput {
    const val SHIFT = 1
    const val CTRL = 2
    const val ALT = 4
    const val META = 8

    private const val FLAGS = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE

    fun available(): Boolean = AppState.ime?.currentInputConnection != null

    fun text(s: String): Boolean {
        val ic = AppState.ime?.currentInputConnection ?: return false
        return ic.commitText(s, 1)
    }

    private fun metaState(mods: Int): Int {
        var m = 0
        if (mods and SHIFT != 0) m = m or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        if (mods and CTRL != 0) m = m or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (mods and ALT != 0) m = m or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (mods and META != 0) m = m or KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON
        return m
    }

    /** Нажать клавишу [code] при зажатых модификаторах [mods]. Модификаторы шлются как отдельные клавиши. */
    fun key(code: Int, mods: Int, repeat: Int = 0): Boolean {
        val ic = AppState.ime?.currentInputConnection ?: return false
        val order = listOf(
            SHIFT to KeyEvent.KEYCODE_SHIFT_LEFT,
            CTRL to KeyEvent.KEYCODE_CTRL_LEFT,
            ALT to KeyEvent.KEYCODE_ALT_LEFT,
            META to KeyEvent.KEYCODE_META_LEFT,
        ).filter { mods and it.first != 0 }

        val t = SystemClock.uptimeMillis()
        var acc = 0
        for ((bit, kc) in order) {
            acc = acc or bit
            ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, kc, 0, metaState(acc), -1, 0, FLAGS))
        }
        val m = metaState(mods)
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, repeat, m, -1, 0, FLAGS))
        ic.sendKeyEvent(KeyEvent(t, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, code, 0, m, -1, 0, FLAGS))
        for ((bit, kc) in order.reversed()) {
            ic.sendKeyEvent(KeyEvent(t, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, kc, 0, metaState(acc), -1, 0, FLAGS))
            acc = acc and bit.inv()
        }
        return true
    }
}
