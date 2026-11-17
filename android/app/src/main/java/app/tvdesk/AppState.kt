package app.tvdesk

import android.graphics.Rect

/**
 * Общее состояние между службами (всё живёт в одном процессе и в главном потоке).
 */
object AppState {
    var service: DeskAccessibilityService? = null
    var ime: DeskIme? = null

    /** Прямоугольник экрана (в пикселях), закрытый панелью клавиатуры. Пустой — панели нет. */
    val covered = Rect()
    var panelDockTop = false
    var imeHasInput = false

    enum class Mirror { OFF, WAITING_TV, STREAMING }
    var mirror = Mirror.OFF
    var mirrorNote = ""

    private val listeners = ArrayList<() -> Unit>()

    fun addListener(l: () -> Unit) { if (!listeners.contains(l)) listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }
    fun changed() { ArrayList(listeners).forEach { it() } }

    /** Часть экрана, которая НЕ закрыта панелью: именно её видно на ТВ и по ней ездит курсор. */
    fun visibleRect(w: Int, h: Int): Rect {
        val r = Rect(0, 0, w, h)
        if (covered.isEmpty) return r
        if (panelDockTop) r.top = covered.bottom.coerceIn(0, h - 1)
        else r.bottom = covered.top.coerceIn(1, h)
        return r
    }
}
