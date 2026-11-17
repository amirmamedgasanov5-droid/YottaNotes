package app.tvdesk

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo

/**
 * Клавиатура-«труба». Пока панель TV Desk на экране — своего окна не показывает (иначе система
 * сдвигала бы приложения), а просто принимает клавиши от панели и отдаёт их в активное поле ввода.
 * Если панели нет — работает как обычная экранная клавиатура.
 */
class DeskIme : InputMethodService() {

    private val listener: () -> Unit = { updateInputViewShown() }

    override fun onCreate() {
        super.onCreate()
        AppState.ime = this
        AppState.addListener(listener)
    }

    override fun onDestroy() {
        AppState.removeListener(listener)
        if (AppState.ime === this) AppState.ime = null
        AppState.imeHasInput = false
        AppState.changed()
        super.onDestroy()
    }

    override fun onEvaluateInputViewShown(): Boolean = AppState.covered.isEmpty

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val prefs = Prefs(this)
        val dm = resources.displayMetrics
        val unit = dm.widthPixels / 16f
        val rowH = (unit * 0.92f).coerceIn(36 * dm.density, 64 * dm.density).toInt()
        return KeyboardView(this, rowH, prefs).apply { setBackgroundColor(0xFF101418.toInt()) }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        AppState.imeHasInput = attribute != null && attribute.inputType != InputType.TYPE_NULL
        AppState.changed()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        AppState.imeHasInput = false
        AppState.changed()
    }
}
